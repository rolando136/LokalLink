package com.rolando.locallink

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.rolando.locallink.data.network.SupabaseNotificationHelper // 👈 Import
import com.rolando.locallink.navigation.AppNavGraph
import com.rolando.locallink.ui.screens.FirebaseChatHelper
import com.rolando.locallink.ui.screens.SupabaseClient
import com.rolando.locallink.ui.theme.LocalLinkTheme
import com.rolando.locallink.utils.NotificationHelper
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus

class MainActivity : ComponentActivity() {

    // Pair<UserId, UserName> for Chat, or String for ItemId
    private var pendingChatLink by mutableStateOf<Pair<String, String>?>(null)
    private var pendingItemLink by mutableStateOf<String?>(null) // 👈 New state for items

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)
        NotificationHelper.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            LocalLinkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val context = applicationContext
                    val scope = rememberCoroutineScope() // Scope for listeners

                    // Listener for Auth & Notifications
                    LaunchedEffect(Unit) {
                        SupabaseClient.client.auth.sessionStatus.collect { status ->
                            if (status is SessionStatus.Authenticated) {
                                val userId = status.session.user?.id
                                if (userId != null) {
                                    // 1. Firebase Chat Listener
                                    FirebaseChatHelper.startNotificationListener(context, userId)
                                    // 2. Supabase Realtime Listener (Likes)
                                    SupabaseNotificationHelper.startRealtimeListener(context, userId, scope)
                                }
                            }
                        }
                    }

                    // Handle Chat Redirect
                    LaunchedEffect(pendingChatLink) {
                        val link = pendingChatLink
                        if (link != null) {
                            val (userId, userName) = link
                            navController.navigate("chat/$userId/$userName")
                            pendingChatLink = null
                        }
                    }

                    // 👇 Handle Item/Like Redirect
                    LaunchedEffect(pendingItemLink) {
                        val itemId = pendingItemLink
                        if (itemId != null) {
                            navController.navigate("details/$itemId")
                            pendingItemLink = null
                        }
                    }

                    AppNavGraph(navController = navController)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        // Chat Redirects
        val chatUserId = intent?.getStringExtra("chat_redirect_user_id")
        val chatUserName = intent?.getStringExtra("chat_redirect_user_name")
        // Item Redirects
        val itemId = intent?.getStringExtra("item_redirect_id")

        if (chatUserId != null && chatUserName != null) {
            pendingChatLink = chatUserId to chatUserName
            intent.removeExtra("chat_redirect_user_id")
            intent.removeExtra("chat_redirect_user_name")
        } else if (itemId != null) {
            // 👈 Handle Item Link
            pendingItemLink = itemId
            intent.removeExtra("item_redirect_id")
        }
    }
}
