package com.rolando.locallink.data.network

import android.content.Context
import android.util.Log
import com.rolando.locallink.data.model.NotificationModel
import com.rolando.locallink.ui.screens.SupabaseClient
import com.rolando.locallink.utils.NotificationHelper
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator // 👈 IMPORT THIS
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

object SupabaseNotificationHelper {

    suspend fun getNotifications(userId: String): List<NotificationModel> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.from("notifications")
                    .select(columns = Columns.list("*")) {
                        filter {
                            eq("user_id", userId)
                        }
                        order("created_at", order = Order.DESCENDING)
                    }
                    .decodeList<NotificationModel>()
            } catch (e: Exception) {
                Log.e("SupabaseNotif", "Error fetching notifications", e)
                emptyList()
            }
        }
    }

    suspend fun createNotification(
        userId: String,
        senderId: String,
        title: String,
        message: String,
        type: String,
        relatedItemId: String? = null
    ) {
        withContext(Dispatchers.IO) {
            try {
                val notification = mapOf(
                    "user_id" to userId,
                    "sender_id" to senderId,
                    "title" to title,
                    "message" to message,
                    "type" to type,
                    "is_read" to false,
                    "related_item_id" to relatedItemId
                )
                SupabaseClient.client.from("notifications").insert(notification)
            } catch (e: Exception) {
                Log.e("SupabaseNotif", "Error creating notification", e)
            }
        }
    }

    suspend fun markAsRead(notificationId: Long) {
        withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.from("notifications")
                    .update({
                        set("is_read", true)
                    }) {
                        filter { eq("id", notificationId) }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getUnreadCount(userId: String): Int {
        return try {
            SupabaseClient.client.from("notifications")
                .select {
                    count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                    filter {
                        eq("user_id", userId)
                        eq("is_read", false)
                    }
                }.countOrNull()?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun startRealtimeListener(context: Context, currentUserId: String, scope: CoroutineScope) {
        try {
            val channel = SupabaseClient.client.channel("public:notifications")

            val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "notifications"
                // 👇 FIXED: Use structured filter instead of raw string
                filter("user_id", FilterOperator.EQ, currentUserId)
            }

            changeFlow.onEach { change ->
                try {
                    val notif = change.decodeRecord<NotificationModel>()
                    Log.d("SupabaseNotif", "Received Realtime Notif: ${notif.title}")

                    if (notif.related_item_id != null) {
                        NotificationHelper.showLikeNotification(
                            context,
                            notif.title,
                            notif.message,
                            notif.related_item_id.toString()
                        )
                    }
                } catch (e: Exception) {
                    Log.e("SupabaseNotif", "Error parsing realtime notif", e)
                }
            }.launchIn(scope)

            scope.launch(Dispatchers.IO) {
                channel.subscribe()
            }
        } catch (e: Exception) {
            Log.e("SupabaseNotif", "Failed to start listener", e)
        }
    }
}