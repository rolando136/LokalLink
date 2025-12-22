package com.rolando.locallink.ui.screens

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String? = null, // Optional: useful if you need to store the UID inside the object
    val name: String = "",
    val email: String = "",
    val avatar: String = ""
)
