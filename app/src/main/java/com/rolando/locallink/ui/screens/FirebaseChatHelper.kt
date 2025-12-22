package com.rolando.locallink.ui.screens

import android.content.Context
import androidx.compose.runtime.Immutable
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.rolando.locallink.utils.NotificationHelper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.Serializable
import android.util.Log
import com.rolando.locallink.ui.screens.SupabaseProfileHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ... (ChatMessage data class remains the same) ...
@Serializable
@Immutable
data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val seen: Boolean = false,
    val attachedItemId: String? = null,
    val attachedItemTitle: String? = null,
    val attachedItemPrice: String? = null,
    val attachedItemImage: String? = null,
    val imageUrl: String? = null
)

object FirebaseChatHelper {
    private val db = Firebase.database.reference

    private var isListenerAttached = false

    // 👇 NEW: Track which chat is currently open to avoid notifying
    var currentOpenChatId: String? = null

    // ... (sendMessage, getChatId methods remain the same) ...
    fun getChatId(user1: String, user2: String): String {
        return if (user1 < user2) "${user1}_$user2" else "${user2}_$user1"
    }

    fun sendMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        text: String,
        attachedItemId: String? = null,
        attachedItemTitle: String? = null,
        attachedItemPrice: String? = null,
        attachedItemImage: String? = null,
        imageUrl: String? = null
    ) {
        val messageId = db.child("chats").child(chatId).child("messages").push().key ?: return

        val message = ChatMessage(
            id = messageId,
            senderId = senderId,
            text = text,
            seen = false,
            attachedItemId = attachedItemId,
            attachedItemTitle = attachedItemTitle,
            attachedItemPrice = attachedItemPrice,
            attachedItemImage = attachedItemImage,
            imageUrl = imageUrl
        )

        db.child("chats").child(chatId).child("messages").child(messageId).setValue(message)

        val update = mapOf(
            "lastMessage" to if (imageUrl != null) "Sent an image" else text,
            "timestamp" to System.currentTimeMillis(),
            "participants/$senderId" to true,
            "participants/$receiverId" to true,
            "unread/$receiverId" to true
        )
        db.child("chats").child(chatId).updateChildren(update)
    }

    // ... (markChatAsRead, markMessagesAsSeen, deleteChat remain the same) ...
    fun markChatAsRead(chatId: String, userId: String) {
        db.child("chats").child(chatId).child("unread").child(userId).setValue(false)
    }

    fun markMessagesAsSeen(chatId: String, currentUserId: String) {
        val ref = db.child("chats").child(chatId).child("messages")
        ref.orderByChild("seen").equalTo(false)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val msg = child.getValue(ChatMessage::class.java)
                        if (msg != null && msg.senderId != currentUserId) {
                            child.ref.child("seen").setValue(true)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun deleteChat(chatId: String) {
        db.child("chats").child(chatId).removeValue()
    }

    // 👇 NEW: Initialize Background Notification Listener
    fun startNotificationListener(context: Context, currentUserId: String) {
        if (isListenerAttached) return // Don't attach twice

        Log.d("Notification", "Attaching ChildEventListener...")
        val chatsRef = db.child("chats")

        chatsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // This fires when a message is added to an existing chat
                processUpdate(snapshot, context, currentUserId)
            }
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // This fires for ALL existing chats on startup.
                // We process it too, but rely on timestamp check to ignore old messages.
                processUpdate(snapshot, context, currentUserId)
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("Notification", "Database Error: ${error.message}")
            }
        })
        isListenerAttached = true
    }

    private fun processUpdate(chatSnapshot: DataSnapshot, context: Context, currentUserId: String) {
        val chatId = chatSnapshot.key ?: return

        val participants = chatSnapshot.child("participants").value as? Map<String, Boolean>
        if (participants?.containsKey(currentUserId) != true) return

        val messagesSnapshot = chatSnapshot.child("messages")
        val lastMessageSnap = messagesSnapshot.children.lastOrNull() ?: return
        val message = lastMessageSnap.getValue(ChatMessage::class.java) ?: return

        val isNotMe = message.senderId != currentUserId
        // Check if message is less than 60 seconds old
        val isRecent = System.currentTimeMillis() - message.timestamp < 60_000
        val isChatClosed = currentOpenChatId != chatId

        if (isNotMe && isRecent && isChatClosed) {
            // 👇 Fetch Profile to get Name and Avatar
            CoroutineScope(Dispatchers.IO).launch {
                val senderProfile = SupabaseProfileHelper.getProfile(message.senderId)
                val name = senderProfile?.name ?: "User"
                val avatar = senderProfile?.avatar

                NotificationHelper.showNewMessageNotification(
                    context,
                    name,   // Sender Name
                    avatar, // Sender Avatar URL
                    chatId,
                    message.senderId // Other User ID (needed for redirect)
                )
            }
        }
    }

    fun listenToUnreadCount(userId: String): Flow<Int> = callbackFlow {
        val ref = db.child("chats")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var count = 0
                for (child in snapshot.children) {
                    val participants = child.child("participants").value as? Map<String, Boolean>
                    if (participants?.containsKey(userId) == true) {
                        val isUnread = child.child("unread").child(userId).getValue(Boolean::class.java) ?: false
                        if (isUnread) count++
                    }
                }
                trySend(count)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val ref = db.child("chats").child(chatId).child("messages")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                trySend(messages)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}