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
    implementation(libs.syntaxerrorJson5)

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
}

application {
    // Set the main class for the benchmark application
    mainClass.set("dev.hossain.json5kt.benchmark.BenchmarkRunner")
}

tasks.test {
    useJUnitPlatform()
}