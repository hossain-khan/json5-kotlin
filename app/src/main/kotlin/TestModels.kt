package org.json5.app

import kotlinx.serialization.Serializable

@Serializable
data class Address(
    val street: String,
    val city: String,
    val state: String,
    val zip: String,
)

@Serializable
data class Contact(
    val email: String,
    val phone: String? = null,
)

@Serializable
data class Employee(
    val id: Int,
    val name: String,
    val position: String,
    val address: Address,
    val contact: Contact,
    val isActive: Boolean = true,
)
