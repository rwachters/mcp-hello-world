package eu.torvian.mcp.helloworld.server

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

fun main(): Unit = runBlocking {
    println("Starting Hello World MCP Server...")

    // 1. Initialize the MCP Server
    val server = Server(
        serverInfo = Implementation(
            name = "hello-world-server",
            version = "1.0.0"
        ),
        options = ServerOptions(
            // We need to explicitly state what capabilities this server offers.
            // Since we only offer tools, we can define just the tools capability.
            // Other capabilities can be omitted or set to null/default.
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true) // Set to true to allow change notifications, although not used in this simple example.
            )
        )
    )

    // 2. Define a simple "greet" tool using the correct SDK classes
    val greetTool = Tool(
        name = "greet",
        title = "Greet User", // Optional but good practice
        description = "Returns a simple greeting.",
        // The inputSchema must be a `Tool.Input` object
        inputSchema = Tool.Input(
            // `properties` is a JsonObject, which we can construct
            properties = buildJsonObject {
                // Define the "name" property
                put("name", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("The name to greet."))
                })
            },
            // `required` is a list of the required property names
            required = listOf("name")
        ),
        outputSchema = null,
        annotations = null
    )

    // 3. Add the tool to the server and define its behavior
    server.addTool(greetTool) { request ->
        // The SDK conveniently deserializes the arguments JsonObject into a map for you here.
        // We need to get the JsonPrimitive and then its content.
        val name = (request.arguments["name"] as? JsonPrimitive)?.content ?: "World"
        val greeting = "Hello, $name!"
        println("Server: Called 'greet' with name='$name'. Responding with: '$greeting'")

        // Return the result in the format expected by the protocol
        CallToolResult(
            content = listOf(
                TextContent(text = greeting)
            )
        )
    }

    // 4. Start the server using STDIO transport
    // StdioServerTransport requires inputStream and outputStream
    val transport = StdioServerTransport(
        inputStream = System.`in`.asSource().buffered(),
        outputStream = System.out.asSink().buffered()
    )

    // Connect the server to the transport. This is a suspending function.
    // We need to keep the main coroutine running until the server connection closes.
    val done = Job()
    server.onClose {
        done.complete()
    }
    server.connect(transport)
    done.join() // Wait for the server to close.
    println("Server closed.")
}