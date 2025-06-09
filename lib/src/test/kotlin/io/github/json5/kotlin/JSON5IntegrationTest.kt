package io.github.json5.kotlin

import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

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
        val settings: Map<String, String>
    )

    @Test
    fun `should work with real-world example`() {
        val config = Config(
            appName = "MyApp",
            version = 2,
            features = listOf("auth", "analytics"),
            settings = mapOf("theme" to "dark", "lang" to "en")
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
        val json5WithComments = """
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
        
        config shouldBe Config(
            appName = "MyApp",
            version = 2,
            features = listOf("auth", "analytics"),
            settings = mapOf("theme" to "dark", "lang" to "en")
        )
    }
}