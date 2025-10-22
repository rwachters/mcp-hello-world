package io.modelcontextprotocol.kotlin.sdk.client

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.websocket.WebSocketSession
import io.modelcontextprotocol.kotlin.sdk.shared.MCP_SUBPROTOCOL
import io.modelcontextprotocol.kotlin.sdk.shared.WebSocketMcpTransport
import kotlin.properties.Delegates

private val logger = KotlinLogging.logger {}

/**
 * Client transport for WebSocket: this will connect to a server over the WebSocket protocol.
 */
public class WebSocketClientTransport(
    private val client: HttpClient,
    private val urlString: String?,
    private val requestBuilder: HttpRequestBuilder.() -> Unit = {},
) : WebSocketMcpTransport() {
    override var session: WebSocketSession by Delegates.notNull()

    override suspend fun initializeSession() {
        logger.debug { "Websocket session initialization started..." }

        session = urlString?.let {
            client.webSocketSession(it) {
                requestBuilder()

                header(HttpHeaders.SecWebSocketProtocol, MCP_SUBPROTOCOL)
            }
        } ?: client.webSocketSession {
            requestBuilder()

            header(HttpHeaders.SecWebSocketProtocol, MCP_SUBPROTOCOL)
        }

        logger.debug { "Websocket session initialization finished" }
    }
}
