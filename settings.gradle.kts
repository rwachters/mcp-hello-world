// Define global project metadata
val projectName = "mcp-hello-world"
val projectGroup = "eu.torvian"
val defaultVersion = "1.0-SNAPSHOT"

// Set the root project's name
rootProject.name = projectName

/**
 * Dynamically configure each project's group and version.
 * - Group: Defined globally
 * - Version: Falls back to default if not defined in build.gradle.kts
 */
gradle.beforeProject {
    group = projectGroup

    // Fallback to default version if not specified in the module
    if (project.version == "unspecified") {
        version = defaultVersion
    }

    // Log for verification
    println("Configured project: $name → Version: $version")
}

include("server")
include("client")
