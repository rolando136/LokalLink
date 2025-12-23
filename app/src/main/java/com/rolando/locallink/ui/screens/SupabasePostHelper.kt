package com.rolando.locallink.ui.screens

import android.util.Log
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// 👇 NEW IMPORTS
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.JsonNull

object SupabasePostHelper {

    suspend fun createPost(
        userId: String,
        title: String,
        description: String,
        price: Double,
        category: String,
        condition: String,
        imageUrls: List<String>,
        type: String = "sell",
        budgetRange: String? = null
    ) {
        val post = PostModel(
            title = title,
            description = description,
            price = price,
            category = category,
            condition = condition,
            images = imageUrls,
            owner_id = userId,
            type = type,
            budget_range = budgetRange
        )
        withContext(Dispatchers.IO) {
            SupabaseClient.client.from("posts").insert(post)
        }
    }

    suspend fun getPosts(type: String = "sell"): List<PostModel> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.from("posts")
                    .select(columns = Columns.list("*, profiles(*)")) {
                        filter {
                            eq("type", type)
                        }
                        order("created_at", order = Order.DESCENDING)
                    }
                    .decodeList<PostModel>()
            } catch (e: Exception) {
                Log.e("SupabasePost", "Error fetching posts", e)
                emptyList()
            }
        }
    }

    suspend fun searchPosts(query: String, category: String?, type: String = "sell"): List<PostModel> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.from("posts")
                    .select(columns = Columns.list("*, profiles(*)")) {
                        filter {
                            ilike("title", "%$query%")
                            if (category != null) {
                                eq("category", category)
                            }
                            eq("type", type)
                        }
                        order("created_at", order = Order.DESCENDING)
                    }
                    .decodeList<PostModel>()
            } catch (e: Exception) {
                Log.e("SupabasePost", "Error searching posts", e)
                emptyList()
            }
        }
    }

    suspend fun getUserPosts(userId: String): List<PostModel> {
        return withContext(Dispatchers.IO) {
            SupabaseClient.client.from("posts").select(columns = Columns.list("*")) {
                filter { eq("owner_id", userId) }
                order("created_at", order = Order.DESCENDING)
            }.decodeList()
        }
    }

    suspend fun getPost(postId: String): PostModel? {
        return withContext(Dispatchers.IO) {
            SupabaseClient.client.from("posts").select(columns = Columns.list("*, profiles(*)")) {
                filter { eq("id", postId) }
            }.decodeSingleOrNull()
        }
    }

    suspend fun deletePost(postId: String) {
        withContext(Dispatchers.IO) {
            SupabaseClient.client.from("posts").delete {
                filter { eq("id", postId) }
            }
        }
    }

    // 👇 FIXED: Used buildJsonObject to handle mixed types safely
    suspend fun updatePost(
        postId: String,
        title: String,
        description: String,
        price: Double,
        category: String,
        condition: String,
        imageUrls: List<String>,
        type: String,
        budgetRange: String? = null
    ) {
        withContext(Dispatchers.IO) {
            val json = buildJsonObject {
                put("title", title)
                put("description", description)
                put("price", price)
                put("category", category)
                put("condition", condition)
                putJsonArray("images") {
                    imageUrls.forEach { add(it) }
                }
                put("type", type)

                // Clear budget_range if null (e.g. switched to 'sell'), otherwise set it
                if (budgetRange == null) {
                    put("budget_range", JsonNull)
                } else {
                    put("budget_range", budgetRange)
                }
            }

            SupabaseClient.client.from("posts").update(json) {
                filter { eq("id", postId) }
            }
        }
    }
}