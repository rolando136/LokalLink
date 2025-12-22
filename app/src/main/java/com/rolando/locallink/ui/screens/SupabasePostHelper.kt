package com.rolando.locallink.ui.screens

import android.util.Log
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        budgetRange: String? = null // 👈 Added
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
            budget_range = budgetRange // 👈 Added
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
                        filter { eq("type", type) }
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
                            if (category != null) eq("category", category)
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
            SupabaseClient.client.from("posts").delete { filter { eq("id", postId) } }
        }
    }

    suspend fun updatePost(
        postId: String,
        title: String,
        description: String,
        price: Double,
        category: String,
        condition: String,
        imageUrls: List<String>,
        type: String,
        budgetRange: String? = null // 👈 Added
    ) {
        withContext(Dispatchers.IO) {
            val updateData = mutableMapOf(
                "title" to title,
                "description" to description,
                "price" to price,
                "category" to category,
                "condition" to condition,
                "images" to imageUrls,
                "type" to type
            )
            // Add budget_range explicitly
            if (budgetRange != null) updateData["budget_range"] = budgetRange

            SupabaseClient.client.from("posts").update(updateData) {
                filter { eq("id", postId) }
            }
        }
    }
}