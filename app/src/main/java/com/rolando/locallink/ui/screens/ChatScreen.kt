package com.rolando.locallink.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rolando.locallink.navigation.ItemAttachment
import com.rolando.locallink.utils.DateUtils
import com.rolando.locallink.utils.ProfileCache
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    currentUserId: String,
    otherUserId: String,
    otherUserName: String,
    attachedItem: ItemAttachment? = null
) {
    val chatId = FirebaseChatHelper.getChatId(currentUserId, otherUserId)
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }

    var currentAttachment by remember { mutableStateOf(attachedItem) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var realName by remember {
        mutableStateOf(ProfileCache.cache[otherUserId]?.name ?: otherUserName)
    }
    var realAvatar by remember {
        mutableStateOf(ProfileCache.cache[otherUserId]?.avatar ?: "")
    }

    var showTimeForMessageId by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current

    val imeInsets = WindowInsets.ime
    val currentMessages by rememberUpdatedState(messages)

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    val lastMyMessageId = remember(messages) {
        messages.lastOrNull { it.senderId == currentUserId }?.id
    }

    LaunchedEffect(Unit) {
        snapshotFlow { imeInsets.getBottom(density) }
            .distinctUntilChanged()
            .collect { bottom ->
                if (bottom > 0 && currentMessages.isNotEmpty()) {
                    listState.scrollToItem(currentMessages.size - 1)
                }
            }
    }

    LaunchedEffect(otherUserId) {
        if (realAvatar.isEmpty() || !ProfileCache.cache.containsKey(otherUserId)) {
            val profile = SupabaseProfileHelper.getProfile(otherUserId)
            if (profile != null) {
                realName = profile.name
                realAvatar = profile.avatar
                ProfileCache.cache[otherUserId] = profile
            }
        }
    }

    LaunchedEffect(chatId) {
        FirebaseChatHelper.markChatAsRead(chatId, currentUserId)

        val cachedMessages = PostCacheManager.loadChatMessages(context, chatId)
        if (cachedMessages.isNotEmpty()) {
            messages = cachedMessages
            scope.launch { listState.scrollToItem(cachedMessages.size - 1) }
        }

        FirebaseChatHelper.getMessages(chatId).collect { newMessages ->
            messages = newMessages
            PostCacheManager.saveChatMessages(context, chatId, newMessages)

            FirebaseChatHelper.markChatAsRead(chatId, currentUserId)
            FirebaseChatHelper.markMessagesAsSeen(chatId, currentUserId)

            if (newMessages.isNotEmpty()) {
                scope.launch { listState.scrollToItem(newMessages.size - 1) }
            }
        }
    }

    DisposableEffect(chatId) {
        FirebaseChatHelper.currentOpenChatId = chatId
        onDispose {
            FirebaseChatHelper.currentOpenChatId = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(if (realAvatar.isNotEmpty()) realAvatar else "https://ui-avatars.com/api/?name=$realName&background=random")
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(realName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
            // 👇 REMOVED: .imePadding() caused the double spacing issue
        ) {

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            ) {
                itemsIndexed(
                    items = messages,
                    key = { _, it -> it.id }
                ) { index, msg ->
                    val isMe = msg.senderId == currentUserId
                    val isExpanded = showTimeForMessageId == msg.id

                    val nextMsg = messages.getOrNull(index + 1)
                    val isLastInSequence = nextMsg == null || nextMsg.senderId != msg.senderId

                    val isAbsoluteLastFromMe = isMe && msg.id == lastMyMessageId
                    val bottomSpacing = if (isLastInSequence) 8.dp else 2.dp

                    val textColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = bottomSpacing)
                            .clickable {
                                showTimeForMessageId = if (isExpanded) null else msg.id
                            },
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (!isMe) {
                            if (isLastInSequence) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(if (realAvatar.isNotEmpty()) realAvatar else "https://ui-avatars.com/api/?name=$realName")
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.LightGray),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Spacer(modifier = Modifier.width(28.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Spacer(Modifier.width(40.dp))
                        }

                        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                            val hasImage = msg.imageUrl != null

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMe) Color(0xFF0084FF) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.widthIn(max = 300.dp)
                            ) {
                                Column(modifier = Modifier.padding(if (hasImage) 0.dp else 12.dp)) {

                                    if (msg.imageUrl != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(msg.imageUrl)
                                                .build(),
                                            contentDescription = "Sent image",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 250.dp)
                                                .clip(RoundedCornerShape(18.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (msg.text.isNotEmpty()) Spacer(Modifier.height(8.dp))
                                    }

                                    if (msg.attachedItemId != null) {
                                        Box(modifier = Modifier.padding(horizontal = if(hasImage) 12.dp else 0.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(
                                                        width = 1.dp,
                                                        color = textColor.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(6.dp)
                                            ) {
                                                AsyncImage(
                                                    model = msg.attachedItemImage,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = msg.attachedItemTitle ?: "",
                                                        color = textColor,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "₱${msg.attachedItemPrice}",
                                                        color = textColor.copy(alpha = 0.8f),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                        if (msg.text.isNotEmpty()) Spacer(Modifier.height(4.dp))
                                    }

                                    if (msg.text.isNotEmpty()) {
                                        Text(
                                            text = msg.text,
                                            color = textColor,
                                            fontSize = 16.sp,
                                            lineHeight = 20.sp,
                                            modifier = Modifier.padding(horizontal = if(hasImage) 12.dp else 0.dp, vertical = if(hasImage) 8.dp else 0.dp)
                                        )
                                    }
                                }
                            }

                            if (isAbsoluteLastFromMe || isExpanded) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp, end = 4.dp)
                                ) {
                                    if (isExpanded) {
                                        Text(
                                            text = DateUtils.formatMessageTime(msg.timestamp),
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    }

                                    if (isMe) {
                                        if (isAbsoluteLastFromMe) {
                                            if (msg.seen) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(if (realAvatar.isNotEmpty()) realAvatar else "https://ui-avatars.com/api/?name=$realName")
                                                        .build(),
                                                    contentDescription = "Seen",
                                                    modifier = Modifier.size(14.dp).clip(CircleShape).background(Color.Gray),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = "Sent",
                                                    tint = Color.LightGray,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        } else if (isExpanded) {
                                            Text(
                                                text = if (msg.seen) "Seen" else "Sent",
                                                fontSize = 10.sp,
                                                color = Color.Gray,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (currentAttachment != null) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = currentAttachment!!.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(Color.Gray),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentAttachment!!.title ?: "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "₱${currentAttachment!!.price}",
                            color = Color(0xFF3B82F6),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = { currentAttachment = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove attachment")
                    }
                }
            }

            if (selectedImageUri != null) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .size(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Gray.copy(alpha = 0.3f))
                        .align(Alignment.Start)
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { selectedImageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha=0.6f), CircleShape)
                            .size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { imageLauncher.launch("image/*") },
                    modifier = Modifier.size(40.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Send Image",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.width(4.dp))

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Aa") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if ((messageText.isNotBlank() || selectedImageUri != null || currentAttachment != null) && !isUploading) {
                            scope.launch {
                                var uploadedImageUrl: String? = null
                                if (selectedImageUri != null) {
                                    isUploading = true
                                    try {
                                        // Use helper with background thread
                                        uploadedImageUrl = uploadImageToSupabase(context, selectedImageUri!!)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
                                        isUploading = false
                                        return@launch
                                    }
                                    isUploading = false
                                }

                                FirebaseChatHelper.sendMessage(
                                    chatId = chatId,
                                    senderId = currentUserId,
                                    receiverId = otherUserId,
                                    text = messageText,
                                    attachedItemId = currentAttachment?.id,
                                    attachedItemTitle = currentAttachment?.title,
                                    attachedItemPrice = currentAttachment?.price,
                                    attachedItemImage = currentAttachment?.imageUrl,
                                    imageUrl = uploadedImageUrl
                                )

                                messageText = ""
                                currentAttachment = null
                                selectedImageUri = null
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color(0xFF0084FF)
                    )
                }
            }
        }
    }
}