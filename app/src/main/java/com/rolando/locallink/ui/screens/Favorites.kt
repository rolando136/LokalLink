package com.rolando.locallink.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

@Composable
fun FavoritesContent(
    favorites: List<ItemModel>,
    onToggleFavorite: (ItemModel) -> Unit,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Text(
            "Your Favorites",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 14.dp, top = 14.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(30.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(favorites) { item ->
                FavoriteItemCard(
                    item = item,
                    liked = true,
                    onUnlike = { onToggleFavorite(item) },
                    onClick = {
                        navController.navigate("details/${item.id}")
                    }
                )
            }
        }
    }
}

@Composable
fun FavoriteItemCard(
    item: ItemModel,
    liked: Boolean,
    onUnlike: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
    ) {
        Image(
            painter = rememberAsyncImagePainter(item.imageUrl),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Bottom gradient
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(70.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                    )
                )
        )

        // Favorite Button (REMOVES from favorites)
        IconButton(
            onClick = { onUnlike() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(28.dp)
        ) {
            Icon(
                modifier = Modifier.size(25.dp),
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color.Red
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("₱${item.price}", color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
        }
    }
}


