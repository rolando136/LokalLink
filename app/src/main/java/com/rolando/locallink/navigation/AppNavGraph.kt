package com.rolando.locallink.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rolando.locallink.ui.screens.*
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.rolando.locallink.utils.ProfileCache // 👈 IMPORT THIS

@Composable
fun AppNavGraph(navController: NavHostController) {

    val startDest = "splash"
    val context = LocalContext.current
    var currentUserId by remember { mutableStateOf(SupabaseAuthHelper.getCurrentUserId()) }

    val favoriteItems = remember { mutableStateListOf<ItemModel>() }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            val cachedFavs = PostCacheManager.loadFavorites(context)
            if (cachedFavs.isNotEmpty()) {
                favoriteItems.clear()
                favoriteItems.addAll(cachedFavs)
            }

            val dbFavorites = SupabaseFavoritesHelper.getUserFavorites(currentUserId!!)

            if (dbFavorites.isNotEmpty()) {
                favoriteItems.clear()
                favoriteItems.addAll(dbFavorites)
                PostCacheManager.saveFavorites(context, dbFavorites)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDest,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable("splash") {
            SplashScreen(
                navigateToSignup = { navController.navigate("signup") },
                navigateToLogin = { navController.navigate("login") },
                navigateToHome = {
                    currentUserId = SupabaseAuthHelper.getCurrentUserId()
                    navController.navigate("home") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                navigateToSignup = { navController.navigate("signup") },
                onLoginClicked = { _, _ ->
                    currentUserId = SupabaseAuthHelper.getCurrentUserId()
                    navController.navigate("home") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onBackPressed = { navController.popBackStack() }
            )
        }

        composable("signup") {
            SignUpScreen(
                onLogin = {
                    navController.navigate("login") { popUpTo("signup") { inclusive = true } }
                },
                onSignUp = {
                    currentUserId = SupabaseAuthHelper.getCurrentUserId()
                    navController.navigate("home") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onBackPressed = { navController.popBackStack() }
            )
        }

        composable("home") {
            if (currentUserId == null) {
                currentUserId = SupabaseAuthHelper.getCurrentUserId()
                if (currentUserId == null) {
                    LaunchedEffect(Unit) { navController.navigate("login") }
                }
            }

            if (currentUserId != null) {
                HomeScreenWrapper(navController, currentUserId!!, favoriteItems)
            }
        }

        composable("notifications") {
            NotificationScreen(
                navController = navController,
                userId = SupabaseAuthHelper.getCurrentUserId() ?: ""
            )
        }

        composable("editProfile") {
            if (currentUserId != null) {
                var profile by remember { mutableStateOf(UserProfile()) }
                val ctx = LocalContext.current

                LaunchedEffect(currentUserId) {
                    val cached = PostCacheManager.loadProfile(ctx)
                    if (cached != null) profile = cached

                    val p = SupabaseProfileHelper.getProfile(currentUserId!!)
                    if (p != null) {
                        profile = p
                        PostCacheManager.saveProfile(ctx, p)
                        ProfileCache.cache[currentUserId!!] = p // Update Memory Cache
                    }
                }
                EditProfileScreen(
                    navController = navController,
                    userId = currentUserId!!,
                    profile = profile,
                    onSave = {
                        PostCacheManager.saveProfile(ctx, it)
                        ProfileCache.cache[currentUserId!!] = it // Update Memory Cache
                    }
                )
            }
        }

        composable("createPost") {
            if (currentUserId != null) {
                CreatePostScreen(navController, currentUserId!!)
            }
        }

        composable("accountSettings") {
            AccountSettingsScreen(navController = navController)
        }

        composable("myPosts") {
            if (currentUserId != null) {
                MyPostsScreen(navController, currentUserId!!)
            }
        }

        composable("editPost/{postId}") { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId")
            if (postId != null) {
                EditPostScreen(navController, postId)
            }
        }

        composable("otherProfile/{userId}") { backStack ->
            val otherUserId = backStack.arguments?.getString("userId")
            if (otherUserId != null && currentUserId != null) {
                OtherUserProfileScreen(
                    navController = navController,
                    userId = otherUserId,
                    currentUserId = currentUserId!!,
                    favoriteItems = favoriteItems,
                    onToggleFavorite = { item ->
                        if (favoriteItems.any { it.id == item.id }) {
                            favoriteItems.removeAll { it.id == item.id }
                            kotlinx.coroutines.GlobalScope.launch {
                                SupabaseFavoritesHelper.removeFavorite(currentUserId!!, item.id)
                            }
                        } else {
                            favoriteItems.add(item)
                            kotlinx.coroutines.GlobalScope.launch {
                                // 👇 Pass 'context' here
                                SupabaseFavoritesHelper.addFavorite(context, currentUserId!!, item.id)
                            }
                        }
                    }
                )
            }
        }

        composable("details/{itemId}") { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: return@composable
            var item by remember { mutableStateOf<ItemModel?>(null) }

            LaunchedEffect(itemId) {
                val post = SupabasePostHelper.getPost(itemId)
                if (post != null) {
                    item = ItemModel(
                        id = post.id ?: "",
                        title = post.title,
                        price = post.price.toInt(),
                        imageUrl = post.images?.firstOrNull() ?: "",
                        images = post.images ?: emptyList(),
                        description = post.description,
                        sellerName = post.profiles?.name ?: "Seller",
                        sellerImage = post.profiles?.avatar ?: "",
                        category = post.category,
                        condition = post.condition,
                        ownerId = post.owner_id ?: ""
                    )
                }
            }

            if (item != null) {
                ItemDetailsScreen(
                    item = item!!,
                    currentUserId = currentUserId ?: "",
                    onBack = { navController.popBackStack() },
                    onMessageClick = {
                        val sellerName = item!!.sellerName
                        val sellerId = item!!.ownerId
                        if (sellerId.isNotEmpty() && currentUserId != null) {
                            val encodedImage = URLEncoder.encode(item!!.imageUrl, StandardCharsets.UTF_8.toString())
                            val route = "chat/$sellerId/$sellerName?itemId=${item!!.id}&itemTitle=${item!!.title}&itemPrice=${item!!.price}&itemImage=$encodedImage"
                            navController.navigate(route)
                        }
                    },
                    onEditClick = { navController.navigate("editPost/${item!!.id}") },
                    onDeleteClick = { /* ... */ },
                    onViewProfileClick = { navController.navigate("otherProfile/${item!!.ownerId}") }
                )
            }
        }

        composable(
            route = "chat/{otherUserId}/{otherUserName}?itemId={itemId}&itemTitle={itemTitle}&itemPrice={itemPrice}&itemImage={itemImage}",
            arguments = listOf(
                navArgument("otherUserId") { type = NavType.StringType },
                navArgument("otherUserName") { type = NavType.StringType },
                navArgument("itemId") { type = NavType.StringType; nullable = true },
                navArgument("itemTitle") { type = NavType.StringType; nullable = true },
                navArgument("itemPrice") { type = NavType.StringType; nullable = true },
                navArgument("itemImage") { type = NavType.StringType; nullable = true }
            )
        ) { backStack ->
            val otherUserId = backStack.arguments?.getString("otherUserId")
            val otherUserName = backStack.arguments?.getString("otherUserName")
            val itemId = backStack.arguments?.getString("itemId")
            val itemTitle = backStack.arguments?.getString("itemTitle")
            val itemPrice = backStack.arguments?.getString("itemPrice")
            val itemImage = backStack.arguments?.getString("itemImage")

            if (currentUserId != null && otherUserId != null) {
                ChatScreen(
                    navController = navController,
                    currentUserId = currentUserId!!,
                    otherUserId = otherUserId,
                    otherUserName = otherUserName ?: "Chat",
                    attachedItem = if (itemId != null) ItemAttachment(itemId, itemTitle, itemPrice, itemImage) else null
                )
            }
        }
    }
}

data class ItemAttachment(
    val id: String,
    val title: String?,
    val price: String?,
    val imageUrl: String?
)

@Composable
fun HomeScreenWrapper(
    navController: NavHostController,
    userId: String,
    favoriteItems: MutableList<ItemModel>
) {
    // 👇 FIXED: Init from Memory Cache (ProfileCache) for instant display
    var profile by remember {
        mutableStateOf(ProfileCache.cache[userId] ?: UserProfile())
    }

    var unreadCount by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(userId) {
        FirebaseChatHelper.listenToUnreadCount(userId).collect { count ->
            unreadCount = count
        }
    }

    LaunchedEffect(userId) {
        // 1. Try Disk Cache (if memory was empty)
        if (profile.name.isEmpty()) {
            val cached = PostCacheManager.loadProfile(context)
            if (cached != null) {
                profile = cached
                ProfileCache.cache[userId] = cached
            }
        }

        // 2. Fetch Fresh Data
        val fetched = SupabaseProfileHelper.getProfile(userId)
        if (fetched != null) {
            profile = fetched
            PostCacheManager.saveProfile(context, fetched)
            ProfileCache.cache[userId] = fetched // Update memory cache
        }
    }

    HomeScreen(
        navController = navController,
        userId = userId,
        profile = profile,
        onProfileUpdated = {
            profile = it
            ProfileCache.cache[userId] = it
        },
        favoriteItems = favoriteItems,
        totalUnread = unreadCount
    )
}

