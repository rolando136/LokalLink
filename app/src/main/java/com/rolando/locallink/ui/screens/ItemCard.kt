package com.rolando.locallink.ui.screens

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@Immutable
data class ItemModel(
    val id: String,
    val title: String,
    val price: Int,
    val imageUrl: String,
    val images: List<String>,
    val description: String,
    val sellerName: String,
    val sellerImage: String,
    val category: String,
    val condition: String,
    val ownerId: String,
    val type: String = "sell",
    val budgetRange: String? = null // 👈 Added
) : Parcelable

@Composable
fun ItemCard(
    item: ItemModel,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onItemClick: (ItemModel) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClick(item) }
    ) {
        // --- Image Section ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.imageUrl)
                    .build(),
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(if (item.sellerImage.isNotEmpty()) item.sellerImage else "https://ui-avatars.com/api/?name=${item.sellerName}&background=random")
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.sellerName,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (item.type == "buy") {
                Surface(
                    color = Color(0xFFFF9800),
                    shape = RoundedCornerShape(bottomEnd = 12.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Search, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("LOOKING FOR", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- Info Section ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 👇 UPDATED: Show Range if available
                    val priceText = if (item.type == "buy" && !item.budgetRange.isNullOrEmpty()) {
                        "Budget: ₱${item.budgetRange}"
                    } else if (item.type == "buy") {
                        "Budget: ₱${item.price}"
                    } else {
                        "₱${item.price}"
                    }

                    Text(
                        text = priceText,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (item.type == "buy") Color(0xFFFF9800) else Color(0xFF3B82F6),
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.width(8.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.condition,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(32.dp).padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFFF4081) else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailsScreen(
    item: ItemModel,
    currentUserId: String,
    onBack: () -> Unit,
    onMessageClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onViewProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val isOwner = currentUserId == item.ownerId
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Post") },
            text = { Text("Are you sure you want to delete this post?") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDeleteClick() }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.background) {
                if (isOwner) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onEditClick, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Edit")
                        }
                        Button(onClick = { showDeleteDialog = true }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Delete")
                        }
                    }
                } else {
                    Button(
                        onClick = onMessageClick,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(60.dp),
                        colors = ButtonDefaults.buttonColors(Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(if (item.type == "buy") "Offer Item" else "Message Seller", fontSize = 18.sp, color = Color.White)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            item {
                val pagerState = rememberPagerState(pageCount = { item.images.size })
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().height(300.dp)) { page ->
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(item.images[page]).build(),
                        contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                }
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
                    repeat(item.images.size) { index ->
                        Box(modifier = Modifier.size(if (index == pagerState.currentPage) 10.dp else 6.dp).clip(CircleShape).background(if (index == pagerState.currentPage) Color(0xFF3B82F6) else Color.Gray.copy(alpha = 0.4f)).padding(4.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    if (item.type == "buy") {
                        Surface(color = Color(0xFFFF9800), shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Icon(Icons.Default.Search, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("LOOKING FOR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                            Text(text = item.category, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(color = Color(0xFF3B82F6).copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                            Text(text = item.condition, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color(0xFF3B82F6))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(item.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)

                    // 👇 UPDATED: Price/Budget Display in Details
                    val priceText = if (item.type == "buy" && !item.budgetRange.isNullOrEmpty()) {
                        "Budget: ₱${item.budgetRange}"
                    } else if (item.type == "buy") {
                        "Budget: ₱${item.price}"
                    } else {
                        "₱${item.price}"
                    }

                    Text(text = priceText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = if (item.type == "buy") Color(0xFFFF9800) else Color(0xFF3B82F6))
                }
            }
            item {
                Text("Description", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                Text(item.description, fontSize = 15.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 20.dp))
            }
            item {
                Text("Seller", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onViewProfileClick() }, verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = item.sellerImage, contentDescription = null, modifier = Modifier.size(55.dp).clip(CircleShape).background(Color.LightGray), contentScale = ContentScale.Crop)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(item.sellerName, fontWeight = FontWeight.Bold)
                        Text("View profile", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}


