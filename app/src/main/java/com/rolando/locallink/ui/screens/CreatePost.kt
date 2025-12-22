package com.rolando.locallink.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(navController: NavHostController, userId: String) {

    var title by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("Gadgets") }

    // 👇 NEW: Slider State
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    val conditionLabels = listOf("New", "Used - Like New", "Used - Good", "Used - Fair")

    var expanded by remember { mutableStateOf(false) }
    val categories = listOf("Gadgets", "Clothes", "Art", "Vehicles", "Furniture", "Books", "Accessories", "Others")

    val imageUris = remember { mutableStateListOf<Uri>() }
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        imageUris.addAll(uris)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(padding)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
                Text("Create Post", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(20.dp))

            // Title
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Add Title…") },
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

            // Price
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

            // Category
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

            // 👇 NEW: Condition Slider
            Text(
                text = "Condition: ${conditionLabels[sliderPosition.toInt()]}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Slider(
                value = sliderPosition,
                onValueChange = { sliderPosition = it },
                valueRange = 0f..3f,
                steps = 2, // Steps between 0 and 3 (i.e., 1 and 2)
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF3B82F6),
                    activeTrackColor = Color(0xFF3B82F6)
                )
            )

            Spacer(Modifier.height(16.dp))

            // Description
            TextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Add Description…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
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

            Spacer(Modifier.height(16.dp))

            // Image Picker
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray)
                            Text("Add Image", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }

                items(imageUris) { uri ->
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // POST BUTTON
            Button(
                onClick = {
                    if (title.isBlank() || description.isBlank() || price.isBlank() || imageUris.isEmpty()) {
                        Toast.makeText(context, "All fields required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val urls = imageUris.map { uploadImageToSupabase(context, it) }

                            val newPost = PostModel(
                                title = title,
                                description = description,
                                price = price.toDoubleOrNull() ?: 0.0,
                                images = urls,
                                category = selectedCategory,
                                condition = conditionLabels[sliderPosition.toInt()], // 👈 Pass selected condition
                                owner_id = userId
                            )

                            SupabasePostHelper.createPost(newPost)

                            withContext(Dispatchers.Main) {
                                isLoading = false
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("refresh_home", true)

                                Toast.makeText(context, "Post Created!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6),
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Post")
                }
            }
        }
    }
}












