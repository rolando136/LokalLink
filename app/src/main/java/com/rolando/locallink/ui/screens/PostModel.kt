package com.rolando.locallink.ui.screens

import kotlinx.serialization.Serializable

@Serializable
data class PostModel(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val condition: String = "",
    val images: List<String>? = null,
    val owner_id: String? = null,
    val created_at: String? = null,
    val profiles: UserProfile? = null,
    val type: String = "sell",
    val budget_range: String? = null // 👈 Added
)