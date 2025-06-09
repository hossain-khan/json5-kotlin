plugins {
    // Apply the shared build logic from a convention plugin.
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
    application
}

dependencies {
    // Depend on the lib module for JSON5 implementation
    implementation(project(":lib"))
    
    // kotlinx.serialization for JSON comparison
    implementation(libs.bundles.kotlinxEcosystem)
    
    // External JSON5 library for comparison
    implementation("at.syntaxerror:json5:2.1.0")

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

application {
    // Set the main class for the benchmark application
    mainClass.set("dev.hossain.json5kt.benchmark.BenchmarkRunner")
}

tasks.test {
    useJUnitPlatform()
}