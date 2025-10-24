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

/**
 * This is a minimal "Hello World" example of an MCP (Model Context Protocol) Server in Kotlin.
 *
 * An MCP Server is a program that exposes specific capabilities (like tools, resources, or prompts)
 * to an AI application (the MCP Client). It acts as a bridge, allowing AI models to interact with
 * external systems or logic.
 *
 * This server specifically provides one simple "greet" tool.
 */
fun main(): Unit = runBlocking {
    println("Starting Hello World MCP Server...")

    // 1. Initialize the MCP Server instance.
    // An MCP Server needs to identify itself and declare what capabilities it offers.
    val server = Server(
        serverInfo = Implementation(
            name = "hello-world-server",
            version = "1.0.0"
        ),
        options = ServerOptions(
            // ServerCapabilities define what features this server supports.
            // For this "Hello World" example, we only offer "tools".
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true)
                // `listChanged = true` indicates that this server can notify clients
                // if its list of available tools changes dynamically.
            )
        )
    )

    // 2. Define a simple "greet" tool.
    // An MCP Tool is an executable function that an AI application (client) can invoke.
    // It has a name, description, and a schema defining its expected input arguments.
    val greetTool = Tool(
        name = "greet", // The unique identifier for this tool.
        title = "Greet User", // A human-readable title.
        description = "Returns a simple greeting.",
        // `inputSchema` defines the structure of the arguments the tool expects.
        // It uses JSON Schema format.
        inputSchema = Tool.Input(
            properties = buildJsonObject {
                // We define one argument named "name" which is a string.
                put("name", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("The name to greet."))
                })
            },
            required = listOf("name") // The "name" argument is mandatory.
        ),
        outputSchema = null, // No explicit output schema defined for this simple example.
        annotations = null   // No additional annotations.
    )

    // 3. Add the defined tool to the server and provide its implementation.
    // When the client calls the "greet" tool, this lambda function will be executed.
    server.addTool(greetTool) { request ->
        // `request.arguments` contains the input provided by the client,
        // formatted as a JsonObject. We extract the "name" argument.
        val name = (request.arguments["name"] as? JsonPrimitive)?.content ?: "World"
        val greeting = "Hello, $name!"
        println("Server: Called 'greet' with name='$name'. Responding with: '$greeting'")

        // `CallToolResult` is the standard way for a server to return the result of a tool execution.
        // `content` is a list of various content types; here we just use plain text.
        CallToolResult(
            content = listOf(
                TextContent(text = greeting)
            )
        )
    }

    // 4. Start the server using STDIO (Standard Input/Output) transport.
    // `StdioServerTransport` enables communication over the standard input and output streams
    // of the process, making it suitable for local, subprocess-based connections.
    val transport = StdioServerTransport(
        inputStream = System.`in`.asSource().buffered(), // Server reads from its standard input.
        outputStream = System.out.asSink().buffered()    // Server writes to its standard output.
    )

    // Connect the server instance to the chosen transport.
    // This `connect` call is a suspending function and will keep the server running
    // until the connection is terminated (e.g., the client closes its end).
    val done = Job() // A Job to manage the lifecycle of the server's coroutine.
    server.onClose {
        done.complete() // Complete the Job when the server connection closes.
    }
    server.connect(transport) // Establish the connection and start listening.
    done.join() // Wait for the server connection to close before exiting `main`.
    println("Server closed.")
}