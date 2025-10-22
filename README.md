# MCP Hello World (Kotlin)

This project demonstrates a minimal "Hello World" application using the Model Context Protocol (MCP) in Kotlin. It consists of a simple MCP server that exposes a "greet" tool, and a corresponding MCP client that connects to the server, discovers the tool, and allows interactive execution.

This example is designed to showcase the core client-server communication of MCP without involving any external AI models or complex business logic.

## Table of Contents

*   [What is MCP?](#what-is-mcp)
*   [Project Structure](#project-structure)
*   [Prerequisites](#prerequisites)
*   [Building the Project](#building-the-project)
*   [Running the Project](#running-the-project)
*   [How it Works](#how-it-works)
*   [Troubleshooting](#troubleshooting)
*   [Further Learning](#further-learning)

## What is MCP?

The Model Context Protocol (MCP) is an open-source standard for connecting AI applications to external systems. It provides a standardized way for AI applications (like LLMs) to access data sources, tools, and workflows, enabling them to retrieve information and perform tasks in the external world.

Think of MCP as a universal adapter for AI models, allowing them to extend their capabilities beyond their internal knowledge to interact with real-world systems.

## Project Structure

This project has a multi-module Gradle setup:

```
mcp-hello-world/
├── build.gradle.kts          // Root Gradle configuration
├── settings.gradle.kts       // Gradle settings for multi-module project
├── client/                   // Client module
│   ├── build.gradle.kts
│   └── src/main/kotlin/eu/torvian/mcp/helloworld/client/
│       ├── HelloWorldClient.kt // The MCP client implementation
│       └── main.kt             // Entry point for the client
├── docs/MCP/                 // Documentation for MCP and related SDKs
│   ├── Architecture-overview.md // Overview of MCP architecture
│   ├── Clients.md             // Documentation on MCP clients
│   ├── Servers.md             // Documentation on MCP servers
│   ├── Specification.md       // Detailed specification of MCP
│   ├── Versioning.md          // Versioning policy for MCP
│   ├── What-is-MCP.md         // Introduction to MCP
│   └── Kotlin-SDK-0.7.3/     // Source code for the MCP Kotlin SDK
├── gradle/
│   ├── libs.versions.toml    // Version catalog for dependencies
│   └── wrapper/              // Gradle wrapper files
└── server                    // Server module
    ├── build.gradle.kts
    └── src/main/kotlin/eu/torvian/mcp/helloworld/server/
        └── HelloWorldServer.kt // The MCP server implementation
```

## Prerequisites

*   **Java 17 or later**: Required for running Kotlin applications on the JVM.
*   **Gradle**: (Optional) If you don't use the provided Gradle wrapper, ensure you have Gradle installed.
*   **Basic understanding of Kotlin**: Familiarity with Kotlin syntax and concepts will be helpful.

## Building the Project

The project uses Gradle to build two separate "fat JAR" files, one for the server and one for the client. These JARs include all necessary dependencies to run independently.

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-username/mcp-hello-world.git
    cd mcp-hello-world
    ```

2.  **Build the server JAR:**
    ```bash
    ./gradlew :server:jar
    ```
    This will produce `server/build/libs/mcp-hello-world-server.jar`.

3.  **Build the client JAR:**
    ```bash
    ./gradlew :client:jar
    ```
    This will produce `client/build/libs/mcp-hello-world-client.jar`.

## Running the Project

To see MCP in action, you only need to execute the client application. The client is configured to launch the server as a subprocess and communicate with it directly via standard I/O streams.

1.  **Ensure both client and server JARs are built** (as per the [Building the Project](#building-the-project) section).

2.  **Execute the client application** from your project root, providing the path to the server JAR as an argument:
    ```bash
    java -jar client/build/libs/mcp-hello-world-client.jar server/build/libs/mcp-hello-world-server.jar
    ```

    The client will start, launch the server as a child process, connect to it, and then enter an interactive loop. The combined output from both the client and its server subprocess will appear in the same terminal:

    ```
    Starting Hello World MCP Server...
    Client: Starting server process: java -jar server/build/libs/mcp-hello-world-server.jar
    Client: Successfully connected to MCP server.
    Client: Discovered tools from server: greet

    --- Interactive Tool Caller ---
    Type a tool name to call it, or 'quit' to exit.
    > Enter tool name: greet
    > Enter value for 'name': Rogier
    Client: Calling tool 'greet' with arguments: {name=Rogier}
    Server: Called 'greet' with name='Rogier'. Responding with: 'Hello, Rogier!'
    Server Response: Hello, Rogier!
    > Enter tool name: quit
    Client: Connection closed.
    Server closed.
    ```

## How it Works

*   **`HelloWorldServer.kt`**:
    *   Initializes an `MCP Server` instance with basic capabilities.
    *   Defines a `Tool` named `greet` with a `name` argument, using the MCP Kotlin SDK's schema definition.
    *   Registers a lambda function for the `greet` tool that extracts the `name` argument and returns a "Hello, \[name]!" `TextContent` as a `CallToolResult`.
    *   Connects to a `StdioServerTransport`, which allows it to communicate via standard input/output streams with a client.

*   **`HelloWorldClient.kt`**:
    *   Initializes an `MCP Client` instance.
    *   In `connectToServer()`, it launches the `mcp-hello-world-server.jar` as a separate child process.
    *   It then sets up a `StdioClientTransport` to communicate with the server subprocess's standard I/O streams.
    *   After connecting, it calls `mcp.listTools()` to discover the `greet` tool offered by the server.
    *   The `interactiveToolLoop()` allows the user to type `greet`, prompts for the `name` argument, and then calls `mcp.callTool()` to execute the server's `greet` tool.
    *   The result from the server is then processed and printed to the console.

*   **`build.gradle.kts` (Fat JARs)**:
    *   The Gradle build scripts for both `client` and `server` modules are configured to create "fat JARs". This means all dependencies (like `kotlin-stdlib`, `kotlinx-coroutines-core`, and the MCP Kotlin SDK itself) are bundled directly into the `mcp-hello-world-client.jar` and `mcp-hello-world-server.jar` files. This makes them easily runnable with `java -jar`.
    *   Special `exclude` rules are applied in the `jar` task to prevent `Duplicate entry` errors during the build, particularly for `META-INF` files and `module-info.class` files that often cause conflicts when merging JARs.

## Troubleshooting

*   **`NoClassDefFoundError`**: This typically means your JAR is not a "fat JAR" and is missing runtime dependencies. Ensure you're building with the fat JAR configuration in `build.gradle.kts` and using `./gradlew :<module>:jar`.
*   **`Duplicate entry` during build**: Check your `build.gradle.kts` `jar` task for the `from(configurations.runtimeClasspath.get()...)` block and ensure the `exclude` rules for `META-INF` files are present and correct.
*   **Client fails to connect**:
    *   Ensure the server JAR path provided as an argument to the client is correct.
    *   Verify there are no unexpected issues starting the subprocess (e.g., permissions, `java` command not in PATH).
*   **SLF4J warnings**: `SLF4J(W): No SLF4J providers were found.` is a common warning. SLF4J is a logging facade. To remove the warning, you can add a logging implementation like `slf4j-simple` to your dependencies (e.g., `implementation("org.slf4j:slf4j-simple:2.0.13")`). This is purely for logging output and doesn't affect the core MCP functionality.

## Further Learning

*   **Model Context Protocol Documentation**: [https://modelcontextprotocol.io/introduction](https://modelcontextprotocol.io/introduction)
*   **MCP Kotlin SDK GitHub**: [https://github.com/modelcontextprotocol/kotlin-sdk](https://github.com/modelcontextprotocol/kotlin-sdk)
*   **Kotlin Coroutines Documentation**: [https://kotlinlang.org/docs/coroutines-guide.html](https://kotlinlang.org/docs/coroutines-guide.html)
