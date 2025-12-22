package com.rolando.locallink.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add // 👈 Added
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUserProfileScreen(
    navController: NavController,
    userId: String,
    currentUserId: String,
    favoriteItems: MutableList<ItemModel>,
    onToggleFavorite: (ItemModel) -> Unit
) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var posts by remember { mutableStateOf<List<PostModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() // 👈 Added Scope for delete

    val isCurrentUser = userId == currentUserId // 👈 Check if this is "My Profile"

    LaunchedEffect(userId) {
        profile = SupabaseProfileHelper.getProfile(userId)
        posts = SupabasePostHelper.getUserPosts(userId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isCurrentUser) "My Listings" else "Seller Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        // 👇 ADDED: FAB for "Create Post" (Only if Current User)
        floatingActionButton = {
            if (isCurrentUser) {
                FloatingActionButton(
                    onClick = { navController.navigate("createPost") },
                    containerColor = Color(0xFF3B82F6),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Post")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                // --- Profile Header ---
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(20.dp))
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(if (profile?.avatar.isNullOrEmpty()) "https://ui-avatars.com/api/?name=${profile?.name ?: "User"}&background=random" else profile!!.avatar)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = profile?.name ?: "Unknown User",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = profile?.email ?: "",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        Spacer(Modifier.height(16.dp))

                        // Hide Message button if it's your own profile
                        if (!isCurrentUser) {
                            Button(
                                onClick = {
                                    val name = profile?.name ?: "User"
                                    navController.navigate("chat/$userId/$name")
                                },
                                modifier = Modifier
                                    .height(45.dp)
                                    .width(140.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Message")
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (isCurrentUser) "My Posts" else "Posts",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // --- Posts List ---
                if (posts.isEmpty()) {
                    item {
                        Text("No posts yet.", color = Color.Gray, modifier = Modifier.padding(top = 20.dp))
                    }
                } else {
                    // 👇 Logic to switch between Editable Card and View-Only Card
                    items(posts) { post ->
                        if (isCurrentUser) {
                            // Render Editable Card
                            MyPostCard(
                                post = post,
                                onEdit = { navController.navigate("editPost/${post.id}") },
                                onDelete = {
                                    coroutineScope.launch {
                                        try {
                                            SupabasePostHelper.deletePost(post.id!!)
                                            posts = posts.filter { it.id != post.id }
                                            PostCacheManager.saveMyPosts(context, posts)
                                            Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error deleting post", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                            Spacer(Modifier.height(16.dp))
                        } else {
                            // Render Standard Item Card
                            val item = ItemModel(
                                id = post.id ?: "",
                                title = post.title,
                                price = post.price.toInt(),
                                imageUrl = post.images?.firstOrNull() ?: "",
                                images = post.images ?: emptyList(),
                                description = post.description,
                                sellerName = profile?.name ?: "Unknown",
                                sellerImage = profile?.avatar ?: "",
                                category = post.category,
                                condition = post.condition,
                                ownerId = post.owner_id ?: ""
                            )
                            ItemCard(
                                item = item,
                                isFavorite = favoriteItems.any { it.id == item.id },
                                onToggleFavorite = { onToggleFavorite(item) },
                                onItemClick = { navController.navigate("details/${item.id}") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            )
                        }
                    }

                    // Spacer for FAB
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}