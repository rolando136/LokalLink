package com.rolando.locallink.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationModel(
    val id: Long = 0,
    val user_id: String,
    val sender_id: String? = null,
    val title: String,
    val message: String,
    val type: String, // "message", "alert", "promotion"
    val is_read: Boolean = false,
    val created_at: String,
    val related_item_id: Long? = null
)