package com.rolando.locallink.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.rolando.locallink.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    userId: String,
    profile: UserProfile,
    onProfileUpdated: (UserProfile) -> Unit,
    favoriteItems: MutableList<ItemModel>,
    totalUnread: Int = 0
) {
    val context = LocalContext.current // 👈 Context is captured here

    // Search & Filter State
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    var posts by remember { mutableStateOf<List<PostModel>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val logoPainter = painterResource(id = if (isDark) R.drawable.darklogo else R.drawable.lightlogo)

    // Refresh Logic
    fun refreshPosts(force: Boolean = false, showLoading: Boolean = true) {
        coroutineScope.launch {
            if (showLoading) isRefreshing = true
            try {
                val fetchedPosts = if (searchQuery.isNotBlank() || selectedCategory != null) {
                    SupabasePostHelper.searchPosts(searchQuery, selectedCategory)
                } else {
                    SupabasePostHelper.getPosts()
                }

                posts = fetchedPosts

                if (searchQuery.isBlank() && selectedCategory == null) {
                    PostCacheManager.savePosts(context, fetchedPosts)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (showLoading) isRefreshing = false
            }
        }
    }

    LaunchedEffect(searchQuery, selectedCategory) {
        if (searchQuery.isNotBlank()) {
            delay(500)
        }
        refreshPosts(force = true, showLoading = false)
    }

    LaunchedEffect(Unit) {
        val cachedPosts = PostCacheManager.loadPosts(context)
        if (cachedPosts.isNotEmpty()) {
            posts = cachedPosts
        }
        if (cachedPosts.isEmpty() || PostCacheManager.isCacheExpired(context)) {
            refreshPosts(force = true, showLoading = true)
        }
    }

    val refreshTrigger = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Boolean>("refresh_home")
        ?.observeAsState()

    LaunchedEffect(refreshTrigger?.value) {
        if (refreshTrigger?.value == true) {
            refreshPosts(force = true)
            navController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>("refresh_home")
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val nestedScrollModifier = if (selectedTab == 0) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier

    Scaffold(
        topBar = {
            if (selectedTab == 0) {
                TopAppBar(
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Lokal", fontWeight = FontWeight.Bold)
                                Text("Link", color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {navController.navigate("notifications")})
                        { Icon(Icons.Default.Notifications, "Notifications") }
                    },
                    navigationIcon = { IconButton(onClick = {}) {
                        Icon(
                            painter = logoPainter,
                            contentDescription = "App Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(28.dp)
                        )
                    } },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        },
        bottomBar = {
            CompactBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                totalUnread = totalUnread
            )
        },
        modifier = nestedScrollModifier,
        containerColor = MaterialTheme.colorScheme.background,
        content = { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (selectedTab) {
                    0 -> HomeContentShrinkSearch(
                        navController = navController,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        favoriteItems = favoriteItems,
                        onToggleFavorite = { item ->
                            if (favoriteItems.any { it.id == item.id }) {
                                favoriteItems.removeAll { it.id == item.id }
                                coroutineScope.launch { SupabaseFavoritesHelper.removeFavorite(userId, item.id) }
                            } else {
                                favoriteItems.add(item)
                                coroutineScope.launch {
                                    // 👇 FIXED: Passed 'context' as the first argument
                                    SupabaseFavoritesHelper.addFavorite(context, userId, item.id)
                                }
                            }
                        },
                        onItemClick = { clickedItem -> navController.navigate("details/${clickedItem.id}") },
                        posts = posts,
                        isRefreshing = isRefreshing,
                        onRefresh = { refreshPosts(force = true, showLoading = true) }
                    )
                    1 -> FavoritesContent(
                        favorites = favoriteItems,
                        onToggleFavorite = { item ->
                            favoriteItems.removeAll { it.id == item.id }
                            coroutineScope.launch { SupabaseFavoritesHelper.removeFavorite(userId, item.id) }
                        },
                        navController = navController
                    )
                    2 -> MessagesScreen(navController = navController, currentUserId = userId)
                    3 -> ProfileContent(
                        navController = navController,
                        profile = profile,
                        userId = userId,
                        onEditProfile = { navController.navigate("editProfile") },
                        onLogout = {
                            coroutineScope.launch {
                                SupabaseAuthHelper.logout()
                                navController.navigate("login") {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                }
                            }
                        }
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeContentShrinkSearch(
    navController: NavHostController,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    favoriteItems: List<ItemModel>,
    onToggleFavorite: (ItemModel) -> Unit,
    onItemClick: (ItemModel) -> Unit,
    posts: List<PostModel>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val scrollState = rememberLazyListState()
    val headerHeightPx = with(LocalDensity.current) { 160.dp.toPx() }
    var isShrunk by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val isDark = isSystemInDarkTheme()
    val logoPainter = painterResource(id = if (isDark) R.drawable.darklogo else R.drawable.lightlogo)

    if (scrollState.isScrollInProgress) {
        LaunchedEffect(Unit) {
            focusManager.clearFocus()
        }
    }

    BackHandler(enabled = searchQuery.isNotEmpty()) {
        onSearchChange("")
        focusManager.clearFocus()
    }

    val displayItems = remember(posts) {
        posts.map { post ->
            ItemModel(
                id = post.id ?: "",
                title = post.title,
                price = post.price.toInt(),
                imageUrl = post.images?.firstOrNull() ?: "",
                images = post.images ?: emptyList(),
                description = post.description,
                sellerName = post.profiles?.name ?: "Unknown Seller",
                sellerImage = post.profiles?.avatar ?: "",
                category = post.category,
                condition = post.condition,
                ownerId = post.owner_id ?: ""
            )
        }
    }

    val searchBarFraction by animateFloatAsState(targetValue = if (isShrunk) 0.7f else 1f, animationSpec = tween(300))

    LaunchedEffect(scrollState.firstVisibleItemIndex, scrollState.firstVisibleItemScrollOffset) {
        val totalScroll = scrollState.firstVisibleItemIndex * headerHeightPx + scrollState.firstVisibleItemScrollOffset
        isShrunk = totalScroll > headerHeightPx * 0.1f
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            stickyHeader {
                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background.copy(0.9f))) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                        AnimatedVisibility(isShrunk) { IconButton(onClick = {}) {
                            Icon(
                                painter = logoPainter,
                                contentDescription = "App Logo",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                        } }
                        Spacer(Modifier.width(4.dp))
                        Box(modifier = Modifier.weight(searchBarFraction)) {
                            TextField(
                                value = searchQuery,
                                onValueChange = onSearchChange,
                                placeholder = { Text("Search...") },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = {
                                            onSearchChange("")
                                            focusManager.clearFocus()
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = { focusManager.clearFocus() }
                                )
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        AnimatedVisibility(isShrunk) { IconButton(onClick = {navController.navigate("notifications")}) { Icon(Icons.Default.Notifications, null) } }
                    }
                    CategoryRow(selectedCategory, onCategorySelected)
                    Spacer(Modifier.height(8.dp))
                }
            }

            items(
                items = displayItems,
                key = { it.id }
            ) { item ->
                ItemCard(
                    item = item,
                    isFavorite = favoriteItems.any { it.id == item.id },
                    onToggleFavorite = { onToggleFavorite(item) },
                    onItemClick = { onItemClick(item) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }
        }
    }
}

// ... (Rest of CategoryRow, CategoryChip, CompactBottomBar, etc. remain unchanged) ...
@Composable
fun CategoryRow(
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        item {
            CategoryChip(
                text = "All",
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) }
            )
        }
        items(categoryList) { category ->
            CategoryChip(
                text = category,
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

val categoryList = listOf(
    "Gadgets", "Clothes", "Art", "Vehicles", "Furniture", "Books", "Accessories", "Others"
)

@Composable
fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) Color(0xFF3B82F6) else MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
        shape = RoundedCornerShape(18.dp),
        border = if (!selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)) else null,
        modifier = Modifier.clip(RoundedCornerShape(18.dp)).clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (selected) Color(0xFFFFFFFF) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 14.sp
        )
    }
}

@Composable
fun CompactBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    totalUnread: Int
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            BottomItem(
                label = "Home",
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )

            BottomItem(
                label = "Favorites",
                selectedIcon = Icons.Filled.Favorite,
                unselectedIcon = Icons.Outlined.Favorite,
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )

            BottomItemWithBadge(
                label = "Messages",
                selectedIcon = Icons.Filled.Mail,
                unselectedIcon = Icons.Outlined.Mail,
                selected = selectedTab == 2,
                unreadCount = totalUnread,
                onClick = { onTabSelected(2) }
            )

            BottomItem(
                label = "Profile",
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person,
                selected = selectedTab == 3,
                onClick = { onTabSelected(3) }
            )
        }
    }
}

@Composable
fun BottomItemWithBadge(
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    selected: Boolean,
    unreadCount: Int,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
    ) {
        Box {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.onBackground else Color.Gray,
                modifier = Modifier.size(22.dp)
            )

            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color.Red, CircleShape)
                        .align(Alignment.TopEnd)
                )
            }
        }

        Text(
            text = label,
            fontSize = 11.sp,
            color = if (selected) MaterialTheme.colorScheme.onBackground else Color.Gray
        )
    }
}

@Composable
fun BottomItem(
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.onBackground else Color.Gray,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (selected) MaterialTheme.colorScheme.onBackground else Color.Gray
        )
    }
}


