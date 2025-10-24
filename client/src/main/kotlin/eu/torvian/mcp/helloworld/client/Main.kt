package eu.torvian.mcp.helloworld.client

import kotlinx.coroutines.runBlocking

/**
 * The main entry point for the MCP Hello World Client application.
 *
 * This function handles command-line arguments, initializes the [HelloWorldClient],
 * connects it to an MCP Server, and starts the interactive tool-calling loop.
 */
fun main(args: Array<String>) = runBlocking {
    // Check if the server JAR path argument is provided.
    if (args.isEmpty()) {
        println("Usage: java -jar <client.jar> <path_to_server.jar>")
        return@runBlocking // Exit if no argument.
    }

    val serverPath = args.first() // Get the path to the server JAR from the arguments.
    val client = HelloWorldClient() // Create an instance of our MCP client.

    // Use a `use` block to ensure `close()` is called automatically when the client is no longer needed,
    // even if exceptions occur. This handles resource cleanup for the AutoCloseable client.
    client.use {
        client.connectToServer(serverPath) // Establish connection to the server.
        client.interactiveToolLoop()       // Start the interactive loop for calling tools.
    }
}

