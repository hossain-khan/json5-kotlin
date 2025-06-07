plugins {
    // The Kotlin DSL plugin provides a convenient way to develop convention plugins.
    // Convention plugins are located in `src/main/kotlin`, with the file extension `.gradle.kts`,
    // and are applied in the project's `build.gradle.kts` files as required.
    `kotlin-dsl`
}

import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

//kotlin {
//    // Correct way to set the JVM toolchain language version using the Kotlin extension
//    // Ensure you have the necessary Kotlin plugin version that supports this
//    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure<KotlinJvmProjectExtension>("kotlin") {
//        jvmToolchain {
//            (this as org.gradle.jvm.toolchain.JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(21))
//        }
//    }
//}
//
dependencies {
    // Add a dependency on the Kotlin Gradle plugin, so that convention plugins can apply it.
    implementation(libs.kotlinGradlePlugin) // libs might not be available if version catalog in buildSrc/settings.gradle.kts is also commented out
}
