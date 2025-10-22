package io.modelcontextprotocol.kotlin.sdk.client

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.ClientSSESession
import io.ktor.client.plugins.sse.sseSession
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.append
import io.ktor.http.isSuccess
import io.ktor.http.protocolWithAuthority
import io.modelcontextprotocol.kotlin.sdk.JSONRPCMessage
import io.modelcontextprotocol.kotlin.sdk.shared.AbstractTransport
import io.modelcontextprotocol.kotlin.sdk.shared.McpJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration

@Deprecated("Use SseClientTransport instead", ReplaceWith("SseClientTransport"), DeprecationLevel.WARNING)
public typealias SSEClientTransport = SseClientTransport

/**
 * Client transport for SSE: this will connect to a server using Server-Sent Events for receiving
 * messages and make separate POST requests for sending messages.
 */
@OptIn(ExperimentalAtomicApi::class)
public class SseClientTransport(
    private val client: HttpClient,
    private val urlString: String?,
    private val reconnectionTime: Duration? = null,
    private val requestBuilder: HttpRequestBuilder.() -> Unit = {},
) : AbstractTransport() {
    private val logger = KotlinLogging.logger {}

    private val initialized: AtomicBoolean = AtomicBoolean(false)
    private val endpoint = CompletableDeferred<String>()

    private lateinit var session: ClientSSESession
    private lateinit var scope: CoroutineScope
    private var job: Job? = null

    private val baseUrl: String by lazy {
        session.call.request.url.let { url ->
            val path = url.encodedPath
            when {
                path.isEmpty() -> url.protocolWithAuthority
                path.endsWith("/") -> url.protocolWithAuthority + path.removeSuffix("/")
                else -> url.protocolWithAuthority + path.take(path.lastIndexOf("/"))
            }
        }
    }

    override suspend fun start() {
        check(initialized.compareAndSet(expectedValue = false, newValue = true)) {
            "SSEClientTransport already started! If using Client class, note that connect() calls start() automatically."
        }

        try {
            session = urlString?.let {
                client.sseSession(
                    urlString = it,
                    reconnectionTime = reconnectionTime,
                    block = requestBuilder,
                )
            } ?: client.sseSession(
                reconnectionTime = reconnectionTime,
                block = requestBuilder,
            )
            scope = CoroutineScope(session.coroutineContext + SupervisorJob())

            job = scope.launch(CoroutineName("SseMcpClientTransport.connect#${hashCode()}")) {
                collectMessages()
            }

            endpoint.await()
        } catch (e: Exception) {
            closeResources()
            initialized.store(false)
            throw e
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun send(message: JSONRPCMessage) {
        check(initialized.load()) { "SseClientTransport is not initialized!" }
        check(job?.isActive == true) { "SseClientTransport is closed!" }
        check(endpoint.isCompleted) { "Not connected!" }

        try {
            val response = client.post(endpoint.getCompleted()) {
                requestBuilder()
                headers.append(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(McpJson.encodeToString(message))
            }

            if (!response.status.isSuccess()) {
                val text = response.bodyAsText()
                error("Error POSTing to endpoint (HTTP ${response.status}): $text")
            }

            logger.debug { "Client successfully sent message via SSE $endpoint" }
        } catch (e: Throwable) {
            _onError(e)
            throw e
        }
    }

    override suspend fun close() {
        check(initialized.load()) { "SseClientTransport is not initialized!" }
        closeResources()
    }

    private suspend fun CoroutineScope.collectMessages() {
        try {
            session.incoming.collect { event ->
                ensureActive()

                when (event.event) {
                    "error" -> {
                        val error = IllegalStateException("SSE error: ${event.data}")
                        _onError(error)
                        throw error
                    }

                    "open" -> {
                        // The connection is open, but we need to wait for the endpoint to be received.
                    }

                    "endpoint" -> handleEndpoint(event.data.orEmpty())

                    else -> handleMessage(event.data.orEmpty())
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            _onError(e)
            throw e
        } finally {
            closeResources()
        }
    }

    private fun handleEndpoint(eventData: String) {
        try {
            val path = if (eventData.startsWith("/")) eventData.substring(1) else eventData
            val endpointUrl = Url("$baseUrl/$path")
            endpoint.complete(endpointUrl.toString())
            logger.debug { "Client connected to endpoint: $endpointUrl" }
        } catch (e: Throwable) {
            _onError(e)
            endpoint.completeExceptionally(e)
            throw e
        }
    }

    private suspend fun handleMessage(data: String) {
        try {
            val message = McpJson.decodeFromString<JSONRPCMessage>(data)
            _onMessage(message)
        } catch (e: SerializationException) {
            _onError(e)
        }
    }

    private suspend fun closeResources() {
        if (!initialized.compareAndSet(expectedValue = true, newValue = false)) return

        job?.cancelAndJoin()
        try {
            if (::session.isInitialized) session.cancel()
            if (::scope.isInitialized) scope.cancel()
            endpoint.cancel()
        } catch (e: Throwable) {
            _onError(e)
        }

        _onClose()
    }
}
