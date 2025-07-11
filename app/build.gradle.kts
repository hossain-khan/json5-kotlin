plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")

    // Apply the Application plugin to add support for building an executable JVM application.
    application
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.kotlinter)
}

dependencies {
    // Project "app" depends on project "lib"
    implementation(project(":lib"))
    implementation(libs.kotlinxSerialization)
}

application {
    // Define the Fully Qualified Name for the application main class
    // (Note that Kotlin compiles `App.kt` to a class with FQN `com.example.app.AppKt`.)
    mainClass = "org.json5.app.AppKt"
}
