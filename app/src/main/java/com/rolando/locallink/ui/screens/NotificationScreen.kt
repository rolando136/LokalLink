package com.rolando.locallink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rolando.locallink.data.model.NotificationModel
import com.rolando.locallink.data.network.SupabaseNotificationHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    userId: String
) {
    var notifications by remember { mutableStateOf<List<NotificationModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) } // Added Loading state
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        notifications = SupabaseNotificationHelper.getNotifications(userId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No notifications yet", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(notifications) { notification ->
                    NotificationItem(
                        notification = notification,
                        onClick = {
                            val updatedList = notifications.map {
                                if (it.id == notification.id) it.copy(is_read = true) else it
                            }
                            notifications = updatedList

                            scope.launch {
                                SupabaseNotificationHelper.markAsRead(notification.id)
                            }

                            if (notification.related_item_id != null) {
                                // 👇 Navigate to the item details
                                navController.navigate("details/${notification.related_item_id}")
                            }
                        }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: NotificationModel,
    onClick: () -> Unit
) {
    val backgroundColor = if (notification.is_read) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = notification.message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatDate(notification.created_at),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        if (!notification.is_read) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

fun formatDate(dateString: String): String {
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val output = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        val date = input.parse(dateString)
        output.format(date ?: "")
    } catch (e: Exception) {
        "Just now"
    }
}