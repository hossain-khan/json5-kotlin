package dev.hossain.json5kt.benchmark

import kotlinx.serialization.Serializable

/**
 * Data models used for benchmarking serialization/deserialization performance.
 */

@Serializable
data class SimplePerson(
    val name: String,
    val age: Int,
    val isActive: Boolean = true
)

@Serializable
data class ComplexPerson(
    val firstName: String,
    val lastName: String,
    val age: Int,
    val email: String,
    val isActive: Boolean,
    val salary: Double,
    val address: Address,
    val phoneNumbers: List<String>,
    val skills: List<String>,
    val metadata: Map<String, String>
)

@Serializable
data class Address(
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String
)

@Serializable
data class Company(
    val name: String,
    val employees: List<ComplexPerson>,
    val departments: List<Department>,
    val founded: Int,
    val revenue: Long,
    val isPublic: Boolean
)

@Serializable
data class Department(
    val name: String,
    val manager: SimplePerson,
    val budget: Double,
    val projects: List<String>
)

@Serializable
data class NumberTypes(
    val intValue: Int,
    val longValue: Long,
    val doubleValue: Double,
    val floatValue: Float,
    val byteValue: Byte,
    val shortValue: Short
)

@Serializable
data class CollectionTypes(
    val stringList: List<String>,
    val intList: List<Int>,
    val booleanList: List<Boolean>,
    val nestedList: List<List<String>>,
    val stringMap: Map<String, String>,
    val intMap: Map<String, Int>,
    val nestedMap: Map<String, Map<String, String>>
)