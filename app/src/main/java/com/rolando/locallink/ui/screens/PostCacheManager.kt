package com.rolando.locallink.ui.screens

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object PostCacheManager {
    private const val PREFS_NAME = "locallink_cache"

    // Keys
    private const val KEY_HOME_POSTS = "cached_home_posts"
    private const val KEY_MY_POSTS = "cached_my_posts"
    private const val KEY_FAVORITES = "cached_favorites"
    private const val KEY_PROFILE = "cached_profile"
    private const val KEY_CHAT_SUMMARIES = "cached_chat_summaries"
    private const val KEY_CHAT_MESSAGES_PREFIX = "cached_messages_"
    private const val KEY_TIMESTAMP = "cache_timestamp"

    private const val CACHE_EXPIRATION_MS = 5 * 60 * 1000 // 5 minutes

    // Wrappers
    @Serializable data class PostListCache(val posts: List<PostModel>)
    @Serializable data class ItemListCache(val items: List<ItemModel>)
    @Serializable data class ChatSummaryListCache(val chats: List<ChatSummary>)
    @Serializable data class ChatMessageListCache(val messages: List<ChatMessage>)

    // --- GENERIC HELPERS ---
    private fun saveData(context: Context, key: String, json: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(key, json)
            .putLong(key + "_timestamp", System.currentTimeMillis())
            .apply()
    }

    private fun loadData(context: Context, key: String): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(key, null)
    }

    // --- HOME POSTS ---
    fun savePosts(context: Context, posts: List<PostModel>) {
        saveData(context, KEY_HOME_POSTS, Json.encodeToString(PostListCache(posts)))
        // Save global timestamp for home feed compatibility
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_TIMESTAMP, System.currentTimeMillis()).apply()
    }

    fun loadPosts(context: Context): List<PostModel> {
        val json = loadData(context, KEY_HOME_POSTS) ?: return emptyList()
        return try { Json.decodeFromString<PostListCache>(json).posts } catch (e: Exception) { emptyList() }
    }

    // --- MY POSTS ---
    fun saveMyPosts(context: Context, posts: List<PostModel>) {
        saveData(context, KEY_MY_POSTS, Json.encodeToString(PostListCache(posts)))
    }

    fun loadMyPosts(context: Context): List<PostModel> {
        val json = loadData(context, KEY_MY_POSTS) ?: return emptyList()
        return try { Json.decodeFromString<PostListCache>(json).posts } catch (e: Exception) { emptyList() }
    }

    // --- FAVORITES ---
    fun saveFavorites(context: Context, items: List<ItemModel>) {
        saveData(context, KEY_FAVORITES, Json.encodeToString(ItemListCache(items)))
    }

    fun loadFavorites(context: Context): List<ItemModel> {
        val json = loadData(context, KEY_FAVORITES) ?: return emptyList()
        return try { Json.decodeFromString<ItemListCache>(json).items } catch (e: Exception) { emptyList() }
    }

    // --- USER PROFILE ---
    fun saveProfile(context: Context, profile: UserProfile) {
        saveData(context, KEY_PROFILE, Json.encodeToString(profile))
    }

    fun loadProfile(context: Context): UserProfile? {
        val json = loadData(context, KEY_PROFILE) ?: return null
        return try { Json.decodeFromString<UserProfile>(json) } catch (e: Exception) { null }
    }

    // --- CHAT SUMMARIES ---
    fun saveChatSummaries(context: Context, chats: List<ChatSummary>) {
        saveData(context, KEY_CHAT_SUMMARIES, Json.encodeToString(ChatSummaryListCache(chats)))
    }

    fun loadChatSummaries(context: Context): List<ChatSummary> {
        val json = loadData(context, KEY_CHAT_SUMMARIES) ?: return emptyList()
        return try { Json.decodeFromString<ChatSummaryListCache>(json).chats } catch (e: Exception) { emptyList() }
    }

    // --- CHAT MESSAGES ---
    fun saveChatMessages(context: Context, chatId: String, messages: List<ChatMessage>) {
        val key = KEY_CHAT_MESSAGES_PREFIX + chatId
        saveData(context, key, Json.encodeToString(ChatMessageListCache(messages)))
    }

    fun loadChatMessages(context: Context, chatId: String): List<ChatMessage> {
        val key = KEY_CHAT_MESSAGES_PREFIX + chatId
        val json = loadData(context, key) ?: return emptyList()
        return try { Json.decodeFromString<ChatMessageListCache>(json).messages } catch (e: Exception) { emptyList() }
    }

    // --- EXPIRY CHECK ---
    fun isCacheExpired(context: Context, key: String = KEY_TIMESTAMP): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Check specific timestamp if exists, else fallback to global (for backward compatibility)
        val lastSaved = prefs.getLong(if(key == KEY_TIMESTAMP) KEY_TIMESTAMP else key + "_timestamp", 0)
        return System.currentTimeMillis() - lastSaved > CACHE_EXPIRATION_MS
    }
}