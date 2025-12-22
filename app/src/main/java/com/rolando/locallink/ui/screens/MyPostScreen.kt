package com.rolando.locallink.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border // 👈 Added import for border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPostsScreen(
    navController: androidx.navigation.NavController,
    userId: String
) {
    var posts by remember { mutableStateOf<List<PostModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Fetch posts on load
    LaunchedEffect(Unit) {
        // 1. Load from Cache
        val cachedPosts = PostCacheManager.loadMyPosts(context)
        if (cachedPosts.isNotEmpty()) {
            posts = cachedPosts
            isLoading = false
        }

        // 2. Fetch from Network
        try {
            val fetchedPosts = SupabasePostHelper.getUserPosts(userId)
            if (fetchedPosts.isNotEmpty()) {
                posts = fetchedPosts
                PostCacheManager.saveMyPosts(context, fetchedPosts)
            } else if (cachedPosts.isEmpty()) {
                posts = emptyList()
            }
        } catch (e: Exception) {
            // If network fails, keep cache
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Posts") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (posts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("You haven't posted anything yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(posts) { post ->
                    MyPostCard(
                        post = post,
                        onEdit = {
                            navController.navigate("editPost/${post.id}")
                        },
                        onDelete = {
                            coroutineScope.launch {
                                try {
                                    SupabasePostHelper.deletePost(post.id!!)
                                    val updatedList = posts.filter { it.id != post.id }
                                    posts = updatedList
                                    PostCacheManager.saveMyPosts(context, updatedList)
                                    Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error deleting post", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MyPostCard(
    post: PostModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Post") },
            text = { Text("Are you sure you want to delete this post? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        // 👇 FIXED: Transparent background
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            // 👇 FIXED: Added Outline Border
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(post.images?.firstOrNull())
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant), // Placeholder color
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1
                )
                Text(
                    text = "₱${post.price.toInt()}",
                    color = Color(0xFF3B82F6),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = post.category,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Action Buttons
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
    }
}