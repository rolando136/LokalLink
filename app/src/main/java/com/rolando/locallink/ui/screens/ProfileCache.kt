package com.rolando.locallink.utils

import androidx.compose.runtime.mutableStateMapOf
import com.rolando.locallink.ui.screens.UserProfile

object ProfileCache {
    // This map lives as long as the app is running.
    // It won't be cleared when you switch tabs.
    val cache = mutableStateMapOf<String, UserProfile>()
}