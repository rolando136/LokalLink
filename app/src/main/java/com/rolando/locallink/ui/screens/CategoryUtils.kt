package com.rolando.locallink.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryUtils {
    fun getIconForCategory(category: String): ImageVector {
        return when (category) {
            "Gadgets" -> Icons.Default.PhoneAndroid
            "Clothes" -> Icons.Default.Checkroom
            "Art" -> Icons.Default.Palette
            "Vehicles" -> Icons.Default.DirectionsCar
            "Furniture" -> Icons.Default.Weekend
            "Books" -> Icons.Default.MenuBook
            "Accessories" -> Icons.Default.Watch
            "Others" -> Icons.Default.Category
            else -> Icons.Default.Category
        }
    }

    val categories = listOf(
        "Gadgets", "Clothes", "Art", "Vehicles",
        "Furniture", "Books", "Accessories", "Others"
    )
}