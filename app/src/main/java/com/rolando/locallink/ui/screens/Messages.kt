package com.rolando.locallink.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.rolando.locallink.utils.DateUtils
import com.rolando.locallink.utils.ProfileCache // 👈 Import the new Cache
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
@IgnoreExtraProperties
data class ChatSummary(
    val chatId: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0,
    val participants: Map<String, Boolean> = emptyMap(),
    val unread: Map<String, Boolean> = emptyMap()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    navController: NavController,
    currentUserId: String
) {
    var chats by remember { mutableStateOf<List<ChatSummary>>(emptyList()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // 👇 FIXED: Use global cache instead of local 'remember'
    // This ensures data persists when switching tabs
    val profiles = ProfileCache.cache

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var chatToDelete by remember { mutableStateOf<ChatSummary?>(null) }

    BackHandler(enabled = searchQuery.isNotEmpty()) {
        searchQuery = ""
        focusManager.clearFocus()
    }

    LaunchedEffect(currentUserId) {
        val cachedChats = PostCacheManager.loadChatSummaries(context)
        if (cachedChats.isNotEmpty()) {
            chats = cachedChats
            // Fetch profiles for cached chats immediately
            cachedChats.forEach { chat ->
                val otherId = chat.participants.keys.firstOrNull { it != currentUserId }
                if (otherId != null && !profiles.containsKey(otherId)) {
                    scope.launch {
                        val p = SupabaseProfileHelper.getProfile(otherId)
                        if (p != null) profiles[otherId] = p
                    }
                }
            }
        }

        val ref = Firebase.database.reference.child("chats")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val loadedChats = mutableListOf<ChatSummary>()
                for (child in snapshot.children) {
                    val participants = child.child("participants").value as? Map<String, Boolean>
                    if (participants != null && participants.containsKey(currentUserId)) {
                        val summary = child.getValue(ChatSummary::class.java)?.copy(chatId = child.key ?: "")
                        if (summary != null) loadedChats.add(summary)
                    }
                }
                val sortedChats = loadedChats.sortedByDescending { it.timestamp }

                chats = sortedChats
                PostCacheManager.saveChatSummaries(context, sortedChats)

                // Fetch profiles for updated list
                sortedChats.forEach { chat ->
                    val otherId = chat.participants.keys.firstOrNull { it != currentUserId }
                    // Only fetch if NOT already in the global cache
                    if (otherId != null && !profiles.containsKey(otherId)) {
                        scope.launch {
                            val p = SupabaseProfileHelper.getProfile(otherId)
                            if (p != null) profiles[otherId] = p
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
    }

    val filteredChats = chats.filter { chat ->
        val otherId = chat.participants.keys.firstOrNull { it != currentUserId } ?: return@filter false
        val name = profiles[otherId]?.name ?: ""
        name.contains(searchQuery, ignoreCase = true) || chat.lastMessage.contains(searchQuery, ignoreCase = true)
    }

    if (chatToDelete != null) {
        AlertDialog(
            onDismissRequest = { chatToDelete = null },
            title = { Text("Delete Conversation") },
            text = { Text("Are you sure you want to delete this conversation? This action cannot be undone and will delete it for both users.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val chat = chatToDelete!!
                        FirebaseChatHelper.deleteChat(chat.chatId)
                        val updatedList = chats.filter { it.chatId != chat.chatId }
                        chats = updatedList
                        PostCacheManager.saveChatSummaries(context, updatedList)
                        chatToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { chatToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { focusManager.clearFocus() }
    ) {
        Text(
            text = "Messages",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(bottom = 14.dp, top = 14.dp)
                .align(Alignment.CenterHorizontally)
        )

        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search messages...") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        if (filteredChats.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isEmpty()) "No messages yet" else "No matches found",
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(
                    items = filteredChats,
                    key = { it.chatId }
                ) { chat ->
                    val otherUserId = chat.participants.keys.firstOrNull { it != currentUserId } ?: return@items

                    // Profile lookup is now instant if in cache
                    val profile = profiles[otherUserId]
                    val name = profile?.name ?: "Loading..."
                    val avatar = profile?.avatar ?: ""
                    val isUnread = chat.unread[currentUserId] == true

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                chatToDelete = chat
                                false
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            val color = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) Color.Red else Color.Transparent
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color, RoundedCornerShape(12.dp))
                                    .padding(end = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White
                                )
                            }
                        },
                        content = {
                            Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                                ChatListRow(
                                    name = name,
                                    avatarUrl = avatar,
                                    lastMessage = chat.lastMessage,
                                    timestamp = chat.timestamp,
                                    isUnread = isUnread,
                                    onClick = {
                                        navController.navigate("chat/$otherUserId/$name")
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatListRow(
    name: String,
    avatarUrl: String,
    lastMessage: String,
    timestamp: Long,
    isUnread: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(if (avatarUrl.isNotEmpty()) avatarUrl else "https://ui-avatars.com/api/?name=$name&background=random")
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    text = DateUtils.formatChatListTime(timestamp),
                    fontSize = 12.sp,
                    color = if (isUnread) MaterialTheme.colorScheme.primary else Color.Gray,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = lastMessage,
                color = if (isUnread) MaterialTheme.colorScheme.onBackground else Color.Gray,
                maxLines = 1,
                fontSize = 14.sp,
                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        if (isUnread) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color.Red, CircleShape)
            )
        }
    }
}