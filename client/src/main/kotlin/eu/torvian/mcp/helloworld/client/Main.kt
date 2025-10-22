package eu.torvian.mcp.helloworld.client

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    if (args.isEmpty()) {
        println("Usage: java -jar <client.jar> <path_to_server.jar>")
        return@runBlocking
    }
    val serverPath = args.first()
    val client = HelloWorldClient()
    client.use {
        client.connectToServer(serverPath)
        client.interactiveToolLoop() // Replaced chatLoop with a simpler, direct tool loop
    }
}

