import org.gradle.api.plugins.JavaPluginExtension

plugins {
    id("org.jetbrains.kotlin.jvm") apply false // Apply Kotlin plugin to subprojects, not root
}

allprojects {
    repositories {
        mavenCentral()
    }

    // Apply Java toolchain configuration after Java/Kotlin plugin is applied
    plugins.withId("org.jetbrains.kotlin.jvm") {
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
    }
    // Also handle pure Java projects if any (though Kotlin plugin should cover most)
    plugins.withId("java") {
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
    }
}
