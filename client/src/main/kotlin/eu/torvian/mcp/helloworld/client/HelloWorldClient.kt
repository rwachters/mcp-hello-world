package eu.torvian.mcp.helloworld.client

import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

/**
 * This class represents a minimal "Hello World" example of an MCP (Model Context Protocol) Client in Kotlin.
 *
 * An MCP Client is typically part of an AI application (or host) that connects to one or more
 * MCP Servers to gain access to their capabilities (tools, resources, prompts).
 *
 * This client connects to a local server, discovers its "greet" tool, and allows interactive execution of it.
 */
class HelloWorldClient : AutoCloseable {
    // 1. Initialize the core MCP Client instance.
    // The client needs to identify itself to the server during the connection handshake.
    private val mcp: Client = Client(clientInfo = Implementation(name = "hello-world-client", version = "1.0.0"))

    // A local cache to store the tools discovered from the connected MCP Server.
    // The key is the tool's name, and the value is the Tool definition.
    private lateinit var availableTools: Map<String, Tool>

    /**
     * Connects this MCP Client to an MCP Server.
     *
     * For this example, the server is launched as a subprocess and communication occurs
     * via standard input/output (STDIO). After connection, the client discovers the
     * tools offered by the server.
     *
     * @param serverScriptPath The file path to the server's executable JAR file.
     */
    suspend fun connectToServer(serverScriptPath: String) {
        try {
            // Build the command array to launch the server JAR.
            // This client is designed to launch a Java JAR server.
            val command = listOf("java", "-jar", serverScriptPath)

            // Start the server application as a new subprocess.
            // This is how the client establishes a local connection to the server.
            println("Client: Starting server process: ${command.joinToString(" ")}")
            val process = ProcessBuilder(command).start()

            // Setup STDIO (Standard Input/Output) transport.
            // This transport uses the input and output streams of the launched subprocess
            // to send and receive MCP messages.
            val transport = StdioClientTransport(
                input = process.inputStream.asSource().buffered(),  // Client reads server's stdout.
                output = process.outputStream.asSink().buffered(), // Client writes to server's stdin.
            )

            // Connect the MCP client instance to the configured transport.
            // This initiates the MCP handshake and establishes the communication session.
            mcp.connect(transport)
            println("Client: Successfully connected to MCP server.")

            // Request the list of available tools from the connected server.
            // This is how the client discovers what capabilities the server offers.
            val toolsResult = mcp.listTools()
            availableTools = toolsResult.tools.associateBy { it.name } // Store them in our map.

            println("Client: Discovered tools from server: ${availableTools.keys.joinToString(", ")}")
        } catch (e: Exception) {
            println("Client: Failed to connect to MCP server: $e")
            throw e
        }
    }

    /**
     * Runs an interactive loop, allowing the user to input tool names and arguments
     * to call tools on the connected MCP Server.
     *
     * This simulates how an AI application might decide to use a tool based on a user's request.
     */
    suspend fun interactiveToolLoop() {
        println("\n--- Interactive Tool Caller ---")
        println("Type a tool name to call it, or 'quit' to exit.")

        while (true) {
            print("\n> Enter tool name: ")
            val toolName = readlnOrNull()?.trim() ?: break // Read user input for tool name.
            if (toolName.equals("quit", ignoreCase = true)) break // Exit loop on "quit".

            val tool = availableTools[toolName]
            if (tool == null) {
                println("Unknown tool '$toolName'. Available tools are: ${availableTools.keys.joinToString(", ")}")
                continue // Ask for input again if tool not found.
            }

            // For this "Hello World" example, we only support calling the "greet" tool.
            if (toolName == "greet") {
                print("> Enter value for 'name': ")
                val nameArg = readlnOrNull() ?: "" // Prompt for the "name" argument.
                val arguments = mapOf("name" to nameArg) // Package argument into a map.

                println("Client: Calling tool '$toolName' with arguments: $arguments")
                // Execute the tool on the server. `mcp.callTool` sends the request
                // and waits for the server's `CallToolResult`.
                val result = mcp.callTool(name = toolName, arguments = arguments)

                // Process and print the text content from the tool's response.
                val resultText = result?.content
                    ?.filterIsInstance<TextContent>() // Filter for text content objects.
                    ?.joinToString("\n") { it.text.toString() } // Extract the text.

                println("Server Response: $resultText")
            } else {
                println("This client only knows how to call the 'greet' tool.")
            }
        }
    }

    /**
     * Closes the MCP client connection and any associated resources.
     *
     * This is crucial for proper resource management and terminating the server subprocess.
     */
    override fun close() {
        runBlocking {
            mcp.close() // Close the MCP connection.
            println("Client: Connection closed.")
            // The subprocess launched by ProcessBuilder will typically terminate
            // when its standard input/output streams are closed by the parent process.
        }
    }
}
