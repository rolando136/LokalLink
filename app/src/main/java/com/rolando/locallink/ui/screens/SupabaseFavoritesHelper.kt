package com.rolando.locallink.ui.screens

import android.content.Context // 👈 Add Import
import android.widget.Toast // 👈 Add Import
import com.rolando.locallink.data.network.SupabaseNotificationHelper
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

object SupabaseFavoritesHelper {

    @Serializable
    data class FavoriteWrapper(
        val posts: PostModel
    )

    // ... (keep getUserFavorites as is) ...
    suspend fun getUserFavorites(userId: String): List<ItemModel> {
        return withContext(Dispatchers.IO) {
            try {
                val result = SupabaseClient.client
                    .from("favorites")
                    .select(columns = Columns.list("posts(*, profiles(*))")) {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<FavoriteWrapper>()

                result.map { wrapper ->
                    val post = wrapper.posts
                    ItemModel(
                        id = post.id ?: "",
                        title = post.title,
                        price = post.price.toInt(),
                        imageUrl = post.images?.firstOrNull() ?: "",
                        images = post.images ?: emptyList(),
                        description = post.description,
                        sellerName = post.profiles?.name ?: "Unknown",
                        sellerImage = post.profiles?.avatar ?: "",
                        category = post.category,
                        condition = post.condition,
                        ownerId = post.owner_id ?: ""
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    // 👇 UPDATED: Add Context to debug errors
    suspend fun addFavorite(context: Context, userId: String, postId: String) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Insert Favorite
                val entry = mapOf("user_id" to userId, "post_id" to postId)
                SupabaseClient.client.from("favorites").insert(entry)

                // 2. Fetch Post Info
                val post = SupabasePostHelper.getPost(postId)

                // 3. Send Notification
                if (post != null && post.owner_id != null && post.owner_id != userId) {
                    SupabaseNotificationHelper.createNotification(
                        userId = post.owner_id,
                        senderId = userId,
                        title = "New Like",
                        message = "Someone liked your item: ${post.title}",
                        type = "alert",
                        relatedItemId = postId
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 👇 Debugging Toast
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Fav Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    suspend fun removeFavorite(userId: String, postId: String) {
        withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.from("favorites").delete {
                    filter {
                        eq("user_id", userId)
                        eq("post_id", postId)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}