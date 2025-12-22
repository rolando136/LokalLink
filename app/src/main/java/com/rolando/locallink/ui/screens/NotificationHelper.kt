package com.rolando.locallink.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.rolando.locallink.MainActivity
import com.rolando.locallink.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationHelper {

    private const val CHANNEL_ID = "chat_messages"
    private const val CHANNEL_NAME = "LocalLink Notifications"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifications for LocalLink"
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Existing Chat Notification (Keep this)
    fun showNewMessageNotification(
        context: Context,
        senderName: String,
        avatarUrl: String?,
        chatId: String,
        otherUserId: String
    ) {
        showNotification(
            context = context,
            title = senderName,
            message = "New message",
            avatarUrl = avatarUrl,
            notificationId = chatId.hashCode(),
            intentExtras = mapOf(
                "chat_redirect_user_id" to otherUserId,
                "chat_redirect_user_name" to senderName
            )
        )
    }

    // 👇 NEW: Generic Notification (For Likes/Alerts)
    fun showLikeNotification(
        context: Context,
        title: String,
        message: String,
        itemId: String
    ) {
        showNotification(
            context = context,
            title = title,
            message = message,
            avatarUrl = null, // Or app logo
            notificationId = itemId.hashCode(),
            intentExtras = mapOf(
                "item_redirect_id" to itemId // 👈 Key for Item Redirect
            )
        )
    }

    // Helper to build and show
    private fun showNotification(
        context: Context,
        title: String,
        message: String,
        avatarUrl: String?,
        notificationId: Int,
        intentExtras: Map<String, String>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            var largeIcon: Bitmap? = null
            if (!avatarUrl.isNullOrEmpty()) {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(avatarUrl)
                    .allowHardware(false)
                    .build()
                val result = (loader.execute(request) as? SuccessResult)?.drawable
                largeIcon = (result as? BitmapDrawable)?.bitmap
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                intentExtras.forEach { (k, v) -> putExtra(k, v) }
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setLargeIcon(largeIcon)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            try {
                NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}