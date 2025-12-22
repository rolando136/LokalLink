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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
    var description by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("Gadgets") }
    var postType by rememberSaveable { mutableStateOf("sell") }

    // Price States
    var price by rememberSaveable { mutableStateOf("") }
    var minBudget by rememberSaveable { mutableStateOf("") }
    var maxBudget by rememberSaveable { mutableStateOf("") }

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    val conditionLabels = listOf("New", "Used - Like New", "Used - Good", "Used - Fair")

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
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
                Text("Create Post", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(20.dp))

            // Type Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypeOption("I'm Selling", postType == "sell", Modifier.weight(1f)) { postType = "sell" }
                TypeOption("Looking For", postType == "buy", Modifier.weight(1f)) { postType = "buy" }
            }

            Spacer(Modifier.height(16.dp))

            TextField(
                value = title, onValueChange = { title = it }, placeholder = { Text("Add Title…") },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                singleLine = true,
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
            )

            Spacer(Modifier.height(16.dp))

            // Conditional Price Inputs
            if (postType == "sell") {
                TextField(
                    value = price,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) price = it },
                    placeholder = { Text("Price (₱)") },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                )
            } else {
                // Range Inputs for "Looking For"
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = minBudget,
                        onValueChange = { if (it.all { char -> char.isDigit() }) minBudget = it },
                        placeholder = { Text("Min (₱)") },
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextField(
                        value = maxBudget,
                        onValueChange = { if (it.all { char -> char.isDigit() }) maxBudget = it },
                        placeholder = { Text("Max (₱)") },
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 👇 This is the function that was missing
            DropdownSelector("Category", selectedCategory, createPostCategoryList) { selectedCategory = it }

            Spacer(Modifier.height(16.dp))

            Text("Condition: ${conditionLabels[sliderPosition.toInt()]}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Slider(
                value = sliderPosition, onValueChange = { sliderPosition = it }, valueRange = 0f..3f, steps = 2,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF3B82F6), activeTrackColor = Color(0xFF3B82F6))
            )

            Spacer(Modifier.height(16.dp))

            TextField(
                value = description, onValueChange = { description = it }, placeholder = { Text("Add Description…") },
                modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                maxLines = 5,
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
            )

            Spacer(Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Box(
                        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, null, tint = Color.Gray); Text("Add Image", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
                items(imageUris) { uri ->
                    Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(14.dp))) {
                        Image(painter = rememberAsyncImagePainter(uri), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        IconButton(onClick = { imageUris.remove(uri) }, modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(0.5f), RoundedCornerShape(4.dp))) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (title.isBlank() || description.isBlank() || imageUris.isEmpty()) {
                        Toast.makeText(context, "Fill required fields & add image", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (postType == "sell" && price.isBlank()) {
                        Toast.makeText(context, "Enter Price", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (postType == "buy" && (minBudget.isBlank() || maxBudget.isBlank())) {
                        Toast.makeText(context, "Enter Budget Range", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val urls = imageUris.map { uploadImageToSupabase(context, it) }

                            val finalPrice = if (postType == "buy") maxBudget.toDouble() else price.toDouble()
                            val budgetRangeString = if (postType == "buy") "$minBudget-$maxBudget" else null

                            SupabasePostHelper.createPost(
                                userId = userId,
                                title = title,
                                description = description,
                                price = finalPrice,
                                category = selectedCategory,
                                condition = conditionLabels[sliderPosition.toInt()],
                                imageUrls = urls,
                                type = postType,
                                budgetRange = budgetRangeString
                            )

                            withContext(Dispatchers.Main) {
                                isLoading = false
                                navController.previousBackStackEntry?.savedStateHandle?.set("refresh_home", true)
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
                modifier = Modifier.fillMaxWidth().height(55.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6), contentColor = Color.White)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Post")
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// 👇 Helper Composable for Type Button
@Composable
fun TypeOption(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(40.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.background else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

// 👇 Helper Composable for Dropdown (Fixed missing reference)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        TextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
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
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = item) },
                    onClick = {
                        onValueChange(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

// 👇 Category List (Fixed missing reference)
val createPostCategoryList = listOf(
    "Gadgets", "Clothes", "Art", "Vehicles", "Furniture", "Books", "Accessories", "Others"
)













