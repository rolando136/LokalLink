package com.rolando.locallink.ui.screens

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SupabaseAuthHelper {

    suspend fun signUp(email: String, pass: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = pass
                }
                Result.success("Sign up successful!")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun login(email: String, pass: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = pass
                }
                Result.success("Logged in successfully")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun getCurrentUserId(): String? {
        return SupabaseClient.client.auth.currentUserOrNull()?.id
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.auth.signOut()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun isUserLoggedIn(): Boolean {
        SupabaseClient.client.auth.awaitInitialization()
        return SupabaseClient.client.auth.currentUserOrNull() != null
    }

    // 👇 The error is fixed here because we removed the bad import
    suspend fun changePassword(newPass: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.auth.updateUser {
                    password = newPass
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

