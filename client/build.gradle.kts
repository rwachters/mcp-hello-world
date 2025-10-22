plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.mcp.kotlin.sdk)
    implementation(libs.kotlinx.coroutines.core)
}

application {
    // Specify the main class for the client application
    mainClass.set("eu.torvian.mcp.helloworld.client.MainKt")
}

tasks.jar {
    archiveFileName.set("mcp-hello-world-client.jar")
    manifest {
        attributes["Main-Class"] = "eu.torvian.mcp.helloworld.client.MainKt"
    }

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        // Exclude problematic META-INF files
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/licenses/**") // Often good to exclude license duplicates too
        exclude("META-INF/AL2.0") // Another common duplicate, Apache License 2.0
        exclude("META-INF/LGPL2.1") // GNU Lesser General Public License 2.1

        // KEY FIX: Exclude module-info.class from different Java versions
        exclude("META-INF/versions/**/module-info.class")
        exclude("module-info.class") // Sometimes it's at the root or other places
    }
    // You can also set a duplicates strategy, but excluding is often cleaner for these files.
    // duplicatesStrategy = DuplicatesStrategy.EXCLUDE // This is an alternative if you want to apply globally

}