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
import com.rolando.locallink.utils.CategoryUtils
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
    val context = LocalContext.current

    // Search & Filter State
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedType by rememberSaveable { mutableStateOf("sell") }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var posts by remember { mutableStateOf<List<PostModel>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val logoPainter = painterResource(id = if (isDark) R.drawable.darklogo else R.drawable.lightlogo)

    // Listen for Category Selection from CategoryScreen
    val categoryResult = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<String>("selected_category")
        ?.observeAsState()

    LaunchedEffect(categoryResult?.value) {
        if (categoryResult?.value != null) {
            selectedCategory = categoryResult.value
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("selected_category")
        }
    }

    // Refresh Logic
    fun refreshPosts(force: Boolean = false, showLoading: Boolean = true) {
        coroutineScope.launch {
            if (showLoading) isRefreshing = true
            try {
                val fetchedPosts = if (searchQuery.isNotBlank() || selectedCategory != null) {
                    SupabasePostHelper.searchPosts(searchQuery, selectedCategory, type = selectedType)
                } else {
                    SupabasePostHelper.getPosts(type = selectedType)
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

    LaunchedEffect(searchQuery, selectedCategory, selectedType) {
        if (searchQuery.isNotBlank()) delay(500)
        refreshPosts(force = true, showLoading = false)
    }

    LaunchedEffect(Unit) {
        val cachedPosts = PostCacheManager.loadPosts(context)
        if (cachedPosts.isNotEmpty()) posts = cachedPosts
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
                        IconButton(onClick = {navController.navigate("notifications")}) {
                            Icon(Icons.Default.Notifications, "Notifications")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(painter = logoPainter, contentDescription = "App Logo", tint = Color.Unspecified, modifier = Modifier.size(28.dp))
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        },
        bottomBar = {
            CompactBottomBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it }, totalUnread = totalUnread)
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { navController.navigate("createPost") },
                    containerColor = Color(0xFF3B82F6),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Post"
                    )
                }
            }
        },
        modifier = nestedScrollModifier,
        containerColor = MaterialTheme.colorScheme.background,
        content = { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (selectedTab) {
                    0 -> HomeContentShrinkSearch(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        selectedType = selectedType,
                        onTypeSelected = { selectedType = it },
                        favoriteItems = favoriteItems,
                        onToggleFavorite = { item ->
                            if (favoriteItems.any { it.id == item.id }) {
                                favoriteItems.removeAll { it.id == item.id }
                                coroutineScope.launch { SupabaseFavoritesHelper.removeFavorite(userId, item.id) }
                            } else {
                                favoriteItems.add(item)
                                coroutineScope.launch { SupabaseFavoritesHelper.addFavorite(context, userId, item.id) }
                            }
                        },
                        onItemClick = { clickedItem -> navController.navigate("details/${clickedItem.id}") },
                        posts = posts,
                        isRefreshing = isRefreshing,
                        onRefresh = { refreshPosts(force = true, showLoading = true) },
                        onSeeMoreCategories = { navController.navigate("categories") }
                    )
                    1 -> FavoritesContent(favoriteItems, { item -> favoriteItems.removeAll { it.id == item.id }; coroutineScope.launch { SupabaseFavoritesHelper.removeFavorite(userId, item.id) } }, navController)
                    2 -> MessagesScreen(navController = navController, currentUserId = userId)
                    3 -> ProfileContent(navController, profile, userId, { navController.navigate("editProfile") }, {
                        coroutineScope.launch { SupabaseAuthHelper.logout(); navController.navigate("login") { popUpTo(navController.graph.id) { inclusive = true } } }
                    })
                }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeContentShrinkSearch(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    favoriteItems: List<ItemModel>,
    onToggleFavorite: (ItemModel) -> Unit,
    onItemClick: (ItemModel) -> Unit,
    posts: List<PostModel>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSeeMoreCategories: () -> Unit
) {
    val scrollState = rememberLazyListState()
    val headerHeightPx = with(LocalDensity.current) { 100.dp.toPx() }
    var isShrunk by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val isDark = isSystemInDarkTheme()
    val logoPainter = painterResource(id = if (isDark) R.drawable.darklogo else R.drawable.lightlogo)

    if (scrollState.isScrollInProgress) LaunchedEffect(Unit) { focusManager.clearFocus() }
    BackHandler(enabled = searchQuery.isNotEmpty() || selectedCategory != null) {
        if (searchQuery.isNotEmpty()) onSearchChange("")
        if (selectedCategory != null) onCategorySelected(null)
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
                sellerName = post.profiles?.name ?: "Unknown",
                sellerImage = post.profiles?.avatar ?: "",
                category = post.category,
                condition = post.condition,
                ownerId = post.owner_id ?: "",
                type = post.type,
                budgetRange = post.budget_range // 👈 Map budgetRange
            )
        }
    }

    val searchBarFraction by animateFloatAsState(targetValue = if (isShrunk) 0.7f else 1f, animationSpec = tween(300))

    LaunchedEffect(scrollState.firstVisibleItemIndex, scrollState.firstVisibleItemScrollOffset) {
        val totalScroll = scrollState.firstVisibleItemIndex * headerHeightPx + scrollState.firstVisibleItemScrollOffset
        isShrunk = totalScroll > headerHeightPx * 0.1f
    }

    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = scrollState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {

            // Sticky Header (Search + Type Toggle Only)
            stickyHeader {
                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background.copy(0.95f))) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                        AnimatedVisibility(isShrunk) { IconButton(onClick = {}) { Icon(painter = logoPainter, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(28.dp)) } }
                        Spacer(Modifier.width(4.dp))
                        Box(modifier = Modifier.weight(searchBarFraction)) {
                            TextField(
                                value = searchQuery, onValueChange = onSearchChange, placeholder = { Text("Search...") },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { onSearchChange(""); focusManager.clearFocus() }) { Icon(Icons.Default.Close, null) } },
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                                colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                                singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        AnimatedVisibility(isShrunk) { IconButton(onClick = {}) { Icon(Icons.Default.Notifications, null) } }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == "sell",
                            onClick = { onTypeSelected("sell") },
                            label = { Text("For Sale", fontSize = 12.sp) },
                            leadingIcon = { if (selectedType == "sell") Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.height(32.dp),
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF3B82F6),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = Color.LightGray.copy(alpha = 0.3f),
                                labelColor = if (isDark) Color.White else Color.Black // 👈 UPDATED: Dynamic Text Color
                            ),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedType == "sell", borderColor = Color.Transparent)
                        )
                        FilterChip(
                            selected = selectedType == "buy",
                            onClick = { onTypeSelected("buy") },
                            label = { Text("Looking For", fontSize = 12.sp) },
                            leadingIcon = { if (selectedType == "buy") Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.height(32.dp),
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF3B82F6),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = Color.LightGray.copy(alpha = 0.3f),
                                labelColor = if (isDark) Color.White else Color.Black // 👈 UPDATED: Dynamic Text Color
                            ),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedType == "buy", borderColor = Color.Transparent)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            // Categories Grid
            item {
                if (selectedCategory == null) {
                    Text("Categories", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
                    HomeCategoryGrid(
                        onCategoryClick = { onCategorySelected(it) },
                        onSeeMoreClick = onSeeMoreCategories
                    )
                    Spacer(Modifier.height(16.dp))
                } else {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Category: ", color = Color.Gray)
                        InputChip(
                            selected = true,
                            onClick = { onCategorySelected(null) },
                            label = { Text(selectedCategory) },
                            trailingIcon = { Icon(Icons.Default.Close, null) }
                        )
                    }
                }
            }

            // Listings Header
            item {
                Text(
                    text = "Listings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Post Items
            items(items = displayItems, key = { it.id }) { item ->
                ItemCard(item = item, isFavorite = favoriteItems.any { it.id == item.id }, onToggleFavorite = { onToggleFavorite(item) }, onItemClick = { onItemClick(item) }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
fun HomeCategoryGrid(
    onCategoryClick: (String) -> Unit,
    onSeeMoreClick: () -> Unit
) {
    val displayCats = CategoryUtils.categories.take(3)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeCategoryCard(category = displayCats[0], onClick = { onCategoryClick(displayCats[0]) }, modifier = Modifier.weight(1f))
            HomeCategoryCard(category = displayCats[1], onClick = { onCategoryClick(displayCats[1]) }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeCategoryCard(category = displayCats[2], onClick = { onCategoryClick(displayCats[2]) }, modifier = Modifier.weight(1f))
            Card(
                modifier = Modifier.weight(1f).height(80.dp).clickable { onSeeMoreClick() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.primary)
                    Text("See More", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun HomeCategoryCard(category: String, onClick: () -> Unit, modifier: Modifier) {
    Card(
        modifier = modifier.height(80.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                CategoryUtils.getIconForCategory(category),
                null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(category, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun CompactBottomBar(selectedTab: Int, onTabSelected: (Int) -> Unit, totalUnread: Int) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth().height(58.dp)) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            BottomItem("Home", Icons.Filled.Home, Icons.Outlined.Home, selectedTab == 0) { onTabSelected(0) }
            BottomItem("Favorites", Icons.Filled.Favorite, Icons.Outlined.Favorite, selectedTab == 1) { onTabSelected(1) }
            BottomItemWithBadge("Messages", Icons.Filled.Mail, Icons.Outlined.Mail, selectedTab == 2, totalUnread) { onTabSelected(2) }
            BottomItem("Profile", Icons.Filled.Person, Icons.Outlined.Person, selectedTab == 3) { onTabSelected(3) }
        }
    }
}

@Composable
fun BottomItemWithBadge(label: String, selectedIcon: ImageVector, unselectedIcon: ImageVector, selected: Boolean, unreadCount: Int, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp)) {
        Box {
            Icon(if (selected) selectedIcon else unselectedIcon, label, tint = if (selected) MaterialTheme.colorScheme.onBackground else Color.Gray, modifier = Modifier.size(22.dp))
            if (unreadCount > 0) Box(modifier = Modifier.size(10.dp).background(Color.Red, CircleShape).align(Alignment.TopEnd))
        }
        Text(label, fontSize = 11.sp, color = if (selected) MaterialTheme.colorScheme.onBackground else Color.Gray)
    }
}

@Composable
fun BottomItem(label: String, selectedIcon: ImageVector, unselectedIcon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp)) {
        Icon(if (selected) selectedIcon else unselectedIcon, label, tint = if (selected) MaterialTheme.colorScheme.onBackground else Color.Gray, modifier = Modifier.size(22.dp))
        Text(label, fontSize = 11.sp, color = if (selected) MaterialTheme.colorScheme.onBackground else Color.Gray)
    }
}


