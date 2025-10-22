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

class HelloWorldClient : AutoCloseable {
    // Initialize the core MCP client
    private val mcp: Client = Client(clientInfo = Implementation(name = "hello-world-client", version = "1.0.0"))

    // A map to store the tools offered by the server, keyed by tool name.
    private lateinit var availableTools: Map<String, Tool>

    // Connects to the server process via STDIO
    suspend fun connectToServer(serverScriptPath: String) {
        try {
            // Build the command to run the server JAR
            val command = listOf("java", "-jar", serverScriptPath)

            // Start the server as a subprocess
            println("Client: Starting server process: ${command.joinToString(" ")}")
            val process = ProcessBuilder(command).start()

            // Setup STDIO transport using the process's input/output streams
            val transport = StdioClientTransport(
                input = process.inputStream.asSource().buffered(),
                output = process.outputStream.asSink().buffered(),
            )

            // Connect the MCP client to the server
            mcp.connect(transport)
            println("Client: Successfully connected to MCP server.")

            // Request the list of available tools from the server
            val toolsResult = mcp.listTools()
            availableTools = toolsResult.tools.associateBy { it.name }

            println("Client: Discovered tools from server: ${availableTools.keys.joinToString(", ")}")
        } catch (e: Exception) {
            println("Client: Failed to connect to MCP server: $e")
            throw e
        }
    }

    // An interactive loop to call tools manually
    suspend fun interactiveToolLoop() {
        println("\n--- Interactive Tool Caller ---")
        println("Type a tool name to call it, or 'quit' to exit.")

        while (true) {
            print("\n> Enter tool name: ")
            val toolName = readlnOrNull()?.trim() ?: break
            if (toolName.equals("quit", ignoreCase = true)) break

            val tool = availableTools[toolName]
            if (tool == null) {
                println("Unknown tool '$toolName'. Available tools are: ${availableTools.keys.joinToString(", ")}")
                continue
            }

            // For this simple example, we'll manually ask for the 'name' argument for the 'greet' tool.
            if (toolName == "greet") {
                print("> Enter value for 'name': ")
                val nameArg = readlnOrNull() ?: ""
                val arguments = mapOf("name" to nameArg)

                println("Client: Calling tool '$toolName' with arguments: $arguments")
                val result = mcp.callTool(name = toolName, arguments = arguments)

                // Process and print the text result from the tool
                val resultText = result?.content
                    ?.filterIsInstance<TextContent>()
                    ?.joinToString("\n") { it.text.toString() }

                println("Server Response: $resultText")
            } else {
                println("This client only knows how to call the 'greet' tool.")
            }
        }
    }

    override fun close() {
        runBlocking {
            mcp.close()
            println("Client: Connection closed.")
        }
    }
}
