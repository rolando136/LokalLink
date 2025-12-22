package com.rolando.locallink.ui.screens

import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime // 👈 Ensure this import exists
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.HttpTimeout

object SupabaseClient {

    @OptIn(SupabaseInternal::class)
    val client = createSupabaseClient(
        supabaseUrl = "https://fsixidosxjxvrkavxpxz.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZzaXhpZG9zeGp4dnJrYXZ4cHh6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQ4NjM3MTcsImV4cCI6MjA4MDQzOTcxN30.kfH7czHUdf2EABUVQ1D2cI3x0FdI7xNDKOVA_ctbZxA"
    ) {
        install(Storage)
        install(Postgrest)
        install(Auth)
        install(Realtime) // 👈 This enables the Realtime plugin

        httpConfig {
            install(HttpTimeout) {
                requestTimeoutMillis = 60000
                connectTimeoutMillis = 60000
                socketTimeoutMillis = 60000
            }
        }

        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        })
    }
}