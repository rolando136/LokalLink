package com.rolando.locallink.ui.screens

import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SupabaseProfileHelper {

    suspend fun getProfile(userId: String): UserProfile? {
        return withContext(Dispatchers.IO) {
            try {
                // Fetch single row where id matches userId
                SupabaseClient.client
                    .from("profiles")
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeSingleOrNull<UserProfile>()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun saveProfile(userId: String, profile: UserProfile): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Prepare data for upsert
                // We map the object fields to the Supabase column names
                val profileData = UserProfile(
                    id = userId, // Ensure ID is included for the Upsert to work on the correct row
                    name = profile.name,
                    email = profile.email,
                    avatar = profile.avatar
                )

                SupabaseClient.client
                    .from("profiles")
                    .upsert(profileData)

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}