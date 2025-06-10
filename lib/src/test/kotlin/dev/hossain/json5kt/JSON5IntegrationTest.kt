package dev.hossain.json5kt

import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Simple integration test to verify end-to-end functionality
 */
@DisplayName("JSON5 Integration Test")
class JSON5IntegrationTest {
    @Serializable
    data class Config(
        val appName: String,
        val version: Int,
        val features: List<String>,
        val settings: Map<String, String>,
    )

    @Test
    fun `should work with real-world example`() {
        val config =
            Config(
                appName = "MyApp",
                version = 2,
                features = listOf("auth", "analytics"),
                settings = mapOf("theme" to "dark", "lang" to "en"),
            )

        // Serialize to JSON5
        val json5 = JSON5.encodeToString(Config.serializer(), config)

        // Verify it's valid JSON5 format
        json5 shouldBe "{appName:'MyApp',version:2,features:['auth','analytics'],settings:{theme:'dark',lang:'en'}}"

        // Deserialize back
        val decoded = JSON5.decodeFromString(Config.serializer(), json5)

        // Should be identical
        decoded shouldBe config
    }

    @Test
    fun `should handle JSON5 with comments and formatting`() {
        val json5WithComments =
            """
            {
                // Application configuration
                appName: 'MyApp',
                version: 2, // current version
                features: [
                    'auth',
                    'analytics', // trailing comma OK
                ],
                settings: {
                    theme: 'dark',
                    lang: 'en',
                }
            }
            """.trimIndent()

        val config = JSON5.decodeFromString(Config.serializer(), json5WithComments)

        config shouldBe
            Config(
                appName = "MyApp",
                version = 2,
                features = listOf("auth", "analytics"),
                settings = mapOf("theme" to "dark", "lang" to "en"),
            )
    }

    @Serializable
    data class BuildConfig(
        val modelVersion: String,
        val dependencies: Map<String, String>,
        val execution: ExecutionConfig,
        val logging: LoggingConfig,
        val debugging: DebuggingConfig,
        val nodeOptions: NodeOptionsConfig,
    )

    @Serializable
    data class ExecutionConfig(
        val analyze: String,
        val daemon: Boolean,
        val incremental: Boolean,
        val parallel: Boolean,
        val typeCheck: Boolean,
    )

    @Serializable
    data class LoggingConfig(
        val level: String,
    )

    @Serializable
    data class DebuggingConfig(
        val stacktrace: Boolean,
    )

    @Serializable
    data class NodeOptionsConfig(
        val maxOldSpaceSize: Int,
        val exposeGC: Boolean,
    )

    @Test
    fun `should handle complex JSON5 build configuration with mixed comment styles`() {
        val buildConfigJson5 =
            """
            {
              "modelVersion": "5.0.1",  // Version of the hvigor base build capability
              "dependencies": {
              },
              "execution": {
                 "analyze": "normal",              /* Build analysis mode */
                 "daemon": true,                   /* Whether to enable daemon process build */
                 "incremental": true,              /* Whether to enable incremental build */
                 "parallel": true,                 /* Whether to enable parallel build */
                 "typeCheck": false,               /* Whether to enable type check */
              },
              "logging": {
                 "level": "info"                          /* Log level */
              },
              "debugging": {
                 "stacktrace": false                      /* Whether to enable stack trace */
              },
              "nodeOptions": {
                 "maxOldSpaceSize": 4096,                  /* Memory size of the daemon process when the daemon process is enabled for build, in MB */
                 "exposeGC": true                         /* Whether to enable GC */
              }
            }
            """.trimIndent()

        val buildConfig = JSON5.decodeFromString(BuildConfig.serializer(), buildConfigJson5)

        buildConfig shouldBe
            BuildConfig(
                modelVersion = "5.0.1",
                dependencies = emptyMap(),
                execution =
                    ExecutionConfig(
                        analyze = "normal",
                        daemon = true,
                        incremental = true,
                        parallel = true,
                        typeCheck = false,
                    ),
                logging = LoggingConfig(level = "info"),
                debugging = DebuggingConfig(stacktrace = false),
                nodeOptions =
                    NodeOptionsConfig(
                        maxOldSpaceSize = 4096,
                        exposeGC = true,
                    ),
            )
    }
}
