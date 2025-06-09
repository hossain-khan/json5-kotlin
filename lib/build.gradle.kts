plugins {
    // Apply the shared build logic from a convention plugin.
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlinPluginSerialization)
    `maven-publish`
}

dependencies {
    implementation(libs.bundles.kotlinxEcosystem)

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")

    // For property-based testing if needed
    testImplementation("io.kotest:kotest-property:5.8.0")
}

tasks.test {
    useJUnitPlatform()
}

// Configure JAR to include sources
val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Publishing configuration
publishing {
    publications {
        register<MavenPublication>("maven") {
            groupId = "hossain.dev"
            artifactId = "json5kt"
            version = "1.0.0"
            
            from(components["java"])
            artifact(sourcesJar)
            
            pom {
                name.set("JSON5 Kotlin")
                description.set("A robust JSON5 parser and serializer for Kotlin/JVM that extends JSON with helpful features like comments, trailing commas, and unquoted keys while maintaining full backward compatibility with JSON.")
                url.set("https://github.com/hossain-khan/json5-kotlin")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("hossain-khan")
                        name.set("Hossain Khan")
                        email.set("hossain@hossain.dev")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/hossain-khan/json5-kotlin.git")
                    developerConnection.set("scm:git:ssh://github.com:hossain-khan/json5-kotlin.git")
                    url.set("https://github.com/hossain-khan/json5-kotlin")
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/hossain-khan/json5-kotlin")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
}
