package com.rolando.locallink.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostScreen(
    navController: NavHostController,
    postId: String
) {
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }

    // 👇 NEW: Slider State
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    val conditionLabels = listOf("New", "Used - Like New", "Used - Good", "Used - Fair")

    var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val categories = listOf("Gadgets", "Clothes", "Art", "Vehicles", "Furniture", "Books", "Accessories", "Others")

    // Load Post Data
    LaunchedEffect(postId) {
        val post = SupabasePostHelper.getPost(postId)
        if (post != null) {
            title = post.title
            price = post.price.toString()
            description = post.description
            selectedCategory = post.category

            // 👇 Initialize Slider Position from existing condition
            val conditionIndex = conditionLabels.indexOf(post.condition)
            sliderPosition = if (conditionIndex >= 0) conditionIndex.toFloat() else 0f

            isLoading = false
        } else {
            Toast.makeText(context, "Error loading post", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    Scaffold { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                // --- Header ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text("Edit Post", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(20.dp))

                // --- Title Input ---
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(Modifier.height(16.dp))

                // --- Price Input ---
                TextField(
                    value = price,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() || char == '.' }) {
                            price = it
                        }
                    },
                    placeholder = { Text("Price (₱)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(Modifier.height(16.dp))

                // --- Category Dropdown ---
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    TextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedCategory = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 👇 NEW: Condition Slider UI
                Text(
                    text = "Condition: ${conditionLabels[sliderPosition.toInt()]}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    valueRange = 0f..3f,
                    steps = 2,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF3B82F6),
                        activeTrackColor = Color(0xFF3B82F6)
                    )
                )

                Spacer(Modifier.height(16.dp))

                // --- Description Input ---
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    maxLines = 5,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(Modifier.height(24.dp))

                // --- Save Button ---
                Button(
                    onClick = {
                        if (title.isBlank() || price.isBlank()) return@Button
                        isSaving = true
                        coroutineScope.launch {
                            val updatedPost = PostModel(
                                id = postId,
                                title = title,
                                description = description,
                                price = price.toDoubleOrNull() ?: 0.0,
                                category = selectedCategory,
                                // 👇 Pass the updated condition
                                condition = conditionLabels[sliderPosition.toInt()],
                                owner_id = null
                            )
                            SupabasePostHelper.updatePost(updatedPost)

                            // Update local cache so changes reflect immediately
                            val currentMyPosts = PostCacheManager.loadMyPosts(context)
                            val updatedList = currentMyPosts.map {
                                if (it.id == postId) it.copy(
                                    title = title,
                                    description = description,
                                    price = price.toDoubleOrNull() ?: 0.0,
                                    category = selectedCategory,
                                    condition = conditionLabels[sliderPosition.toInt()]
                                ) else it
                            }
                            PostCacheManager.saveMyPosts(context, updatedList)

                            isSaving = false
                            Toast.makeText(context, "Post updated!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(18.dp),
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6),
                        contentColor = Color.White
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

