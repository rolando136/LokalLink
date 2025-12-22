package com.rolando.locallink.ui.screens

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SupabasePostHelper {

    // Fetch all posts (Standard feed)
    suspend fun getPosts(): List<PostModel> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client
                    .from("posts")
                    .select(columns = Columns.list("*", "profiles(*)")) {
                        order("created_at", order = Order.DESCENDING)
                    }
                    .decodeList<PostModel>()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    // 👇 FIXED: Added 'filter { }' block around ilike and eq
    suspend fun searchPosts(query: String, category: String?): List<PostModel> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client
                    .from("posts")
                    .select(columns = Columns.list("*", "profiles(*)")) {

                        // 1. FILTERS MUST BE INSIDE THIS BLOCK
                        filter {
                            if (query.isNotBlank()) {
                                ilike("title", "%$query%")
                            }

                            if (category != null && category != "All") {
                                eq("category", category)
                            }
                        }

                        // 2. SORTING goes outside the filter block
                        order("created_at", order = Order.DESCENDING)
                    }
                    .decodeList<PostModel>()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun getPost(id: String): PostModel? {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client
                    .from("posts")
                    .select(columns = Columns.list("*", "profiles(*)")) {
                        filter {
                            eq("id", id)
                        }
                    }
                    .decodeSingleOrNull<PostModel>()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun createPost(post: PostModel) {
        withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.from("posts").insert(post)
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }

    suspend fun getUserPosts(userId: String): List<PostModel> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client
                    .from("posts")
                    .select(columns = Columns.list("*", "profiles(*)")) {
                        filter {
                            eq("owner_id", userId)
                        }
                        order("created_at", order = Order.DESCENDING)
                    }
                    .decodeList<PostModel>()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun deletePost(postId: String) {
        withContext(Dispatchers.IO) {
            SupabaseClient.client.from("posts").delete {
                filter { eq("id", postId) }
            }
        }
    }

    suspend fun updatePost(post: PostModel) {
        withContext(Dispatchers.IO) {
            SupabaseClient.client.from("posts").update(
                {
                    set("title", post.title)
                    set("description", post.description)
                    set("price", post.price)
                    set("category", post.category)
                    set("condition", post.condition) // 👈 Added
                }
            ) {
                filter { eq("id", post.id!!) }
            }
        }
    }
}