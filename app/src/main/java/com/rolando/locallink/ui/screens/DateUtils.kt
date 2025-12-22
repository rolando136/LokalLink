package com.rolando.locallink.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

object DateUtils {

    // Format for inside the Chat Screen (e.g., "10:30 AM")
    fun formatMessageTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // Format for the Messages List (e.g., "10:30 AM", "Yesterday", "Dec 8")
    fun formatChatListTime(timestamp: Long): String {
        val now = Calendar.getInstance()
        val time = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            // Same day: Show time (10:30 AM)
            now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR) -> {
                formatMessageTime(timestamp)
            }
            // Yesterday: Show "Yesterday"
            now.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) - time.get(Calendar.DAY_OF_YEAR) == 1 -> {
                "Yesterday"
            }
            // Older: Show Date (Dec 8)
            else -> {
                val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}