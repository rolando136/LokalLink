package com.rolando.locallink.ui.screens

import kotlinx.serialization.Serializable

@Serializable
data class PostModel(
    val id: String? = null,
    val title: String,
    val description: String,
    val price: Double = 0.0,
    val images: List<String>? = null,
    val category: String = "General",
    val condition: String = "New", // 👈 NEW FIELD
    val owner_id: String? = null,
    val profiles: UserProfile? = null
)