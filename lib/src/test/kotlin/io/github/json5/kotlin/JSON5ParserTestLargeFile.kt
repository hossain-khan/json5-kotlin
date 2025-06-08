package io.github.json5.kotlin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Tests the JSON5 parser's ability to handle a large and complex JSON5 file.
 * This class focuses on ensuring the parser can correctly process substantial inputs
 * that may include a wide variety of JSON5 features, nested structures, and numerous entries.
 */
class JSON5ParserTestLargeFile {
    /**
     * Parses the `runtime_enabled_features.json5` file, a real-world example from Chromium.
     * This test performs several assertions to verify the structural integrity and
     * content of the parsed data. It checks for the presence of specific keys,
     * the types of values (Map, List), and the values of certain properties within
     * the parsed configuration. This ensures the parser handles large, complex,
     * and potentially deeply nested JSON5 documents correctly.
     */
    @Test
    fun testParseSimpleChromiumConfig() {
        val path = Paths.get("src/test/resources/runtime_enabled_features.json5")
        val json5Text = Files.readString(path)
        val result = JSON5Parser.parse(json5Text)

        // Basic structural assertions
        assert(result is Map<*, *>) { "Parsed result should be a Map" }
        val rootMap = result as Map<String, Any?>

        assert(rootMap.containsKey("parameters")) { "Root map should contain 'parameters' key" }
        assert(rootMap.containsKey("data")) { "Root map should contain 'data' key" }

        val parameters = rootMap["parameters"]
        assert(parameters is Map<*, *>) { "'parameters' should be a Map" }
        val parametersMap = parameters as Map<String, Any?>

        val data = rootMap["data"]
        assert(data is List<*>) { "'data' should be a List" }
        val dataList = data as List<Any?>

        // Validate 'parameters' content (example: status parameter)
        assert(parametersMap.containsKey("status")) { "Parameters should contain 'status'" }
        val statusParam = parametersMap["status"]
        assert(statusParam is Map<*, *>) { "'status' parameter should be a Map" }
        val statusParamMap = statusParam as Map<String, Any?>
        assert(statusParamMap.containsKey("valid_values")) { "'status' parameter should have 'valid_values'" }
        val validStatusValues = statusParamMap["valid_values"]
        assert(validStatusValues is List<*>) { "'valid_values' for status should be a List" }
        val validStatusList = validStatusValues as List<String>
        assert(validStatusList.contains("stable")) { "'valid_values' for status should include 'stable'" }
        assert(validStatusList.contains("experimental")) { "'valid_values' for status should include 'experimental'" }
        assert(validStatusList.contains("test")) { "'valid_values' for status should include 'test'" }

        // Validate 'data' content (example: first feature)
        assert(dataList.isNotEmpty()) { "Data list should not be empty" }
        val firstFeature = dataList.first()
        assert(firstFeature is Map<*, *>) { "First feature in data list should be a Map" }
        val firstFeatureMap = firstFeature as Map<String, Any?>

        assertEquals("Accelerated2dCanvas", firstFeatureMap["name"]) {"First feature name mismatch"}
        assertEquals(true, firstFeatureMap["settable_from_internals"]) {"First feature settable_from_internals mismatch"}
        assertEquals("stable", firstFeatureMap["status"]) {"First feature status mismatch"}

        // Example: Find a specific feature and validate its properties
        val adInterestGroupAPI = dataList.find { it is Map<*, *> && (it as Map<String, Any?>)["name"] == "AdInterestGroupAPI" }
        assert(adInterestGroupAPI != null) { "Feature 'AdInterestGroupAPI' should exist" }
        val adInterestGroupAPIMap = adInterestGroupAPI as Map<String, Any?>
        assertEquals("stable", adInterestGroupAPIMap["status"]) { "'AdInterestGroupAPI' status mismatch" }
        assertEquals(true, adInterestGroupAPIMap["public"]) { "'AdInterestGroupAPI' public flag mismatch" }
        val impliedByFledge = (adInterestGroupAPIMap["implied_by"] as? List<*>)?.contains("Fledge")
        assertEquals(true, impliedByFledge) { "'AdInterestGroupAPI' should be implied_by 'Fledge'" }

        // Example: Check a feature with a map as status
        val aiPromptAPI = dataList.find { it is Map<*, *> && (it as Map<String, Any?>)["name"] == "AIPromptAPI" }
        assert(aiPromptAPI != null) { "Feature 'AIPromptAPI' should exist" }
        val aiPromptAPIMap = aiPromptAPI as Map<String, Any?>
        val aiPromptAPIStatus = aiPromptAPIMap["status"]
        assert(aiPromptAPIStatus is Map<*, *>) { "'AIPromptAPI' status should be a Map" }
        val aiPromptAPIStatusMap = aiPromptAPIStatus as Map<String, Any?>
        assertEquals("experimental", aiPromptAPIStatusMap["Win"]) { "'AIPromptAPI' status for Win mismatch" }
        assertEquals("experimental", aiPromptAPIStatusMap["Mac"]) { "'AIPromptAPI' status for Mac mismatch" }
    }

}
