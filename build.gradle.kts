plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

//allprojects {
//    group = "eu.torvian.mcp.helloworld"
//    version = "1.0-SNAPSHOT"
//
//    repositories {
//        mavenCentral()
//    }
//}
//
//subprojects {
//    // Apply the Kotlin JVM plugin and Application plugin to each subproject
//    apply(plugin = "org.jetbrains.kotlin.jvm")
//    apply(plugin = "application") // For easy executable JAR creation
//
//    dependencies {
//        // Use aliases from libs.versions.toml for dependencies
//        implementation(libs.kotlin.stdlib)
//        implementation(libs.mcp.kotlin.sdk)
//        implementation(libs.kotlinx.coroutines.core)
//    }
//
//    // Configuration for creating a "fat JAR" (executable JAR with all dependencies)
//    tasks.jar {
//        archiveBaseName.set(project.name) // Use subproject name for JAR filename
//        duplicatesStrategy = DuplicatesStrategy.EXCLUDE // Prevent issues with duplicate entries
//
//        // Copy all runtime dependencies into the JAR
//        from(configurations.runtimeClasspath.get().map {
//            if (it.isDirectory) it else zipTree(it)
//        }) {
//            // Exclude META-INF files from dependencies to avoid conflicts
//            exclude("META-INF/*.SF")
//            exclude("META-INF/*.DSA")
//            exclude("META-INF/*.RSA")
//            exclude("META-INF/LICENSE*")
//            exclude("META-INF/NOTICE*")
//        }
//    }
//}