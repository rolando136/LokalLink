package com.rolando.locallink.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
// import androidx.compose.material.icons.automirrored.filled.ArrowForward // 👈 Removed unused import
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

@Composable
fun ProfileContent(
    navController: NavHostController,
    profile: UserProfile,
    userId: String,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Log Out", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "Profile",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 14.dp, top = 14.dp)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(40.dp))

        // 👇 UPDATED: Clickable Profile Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    navController.navigate("otherProfile/$userId")
                }
                .padding(8.dp)
        ) {
            Image(
                painter = rememberAsyncImagePainter(profile.avatar.ifEmpty { "https://picsum.photos/200" }),
                contentDescription = null,
                modifier = Modifier.size(80.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                // 👇 CHANGED: Replaced Email with Action Text
                Text(
                    text = "Click to view your posts",
                    color = MaterialTheme.colorScheme.primary, // Blue color to indicate action
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            // ❌ REMOVED: Arrow Icon
        }

        Spacer(Modifier.height(20.dp))

        Button(onClick = onEditProfile, modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6), contentColor = Color.White)) {
            Text("Edit Profile")
        }

        Spacer(Modifier.height(30.dp))

        ProfileOption(
            icon = Icons.Default.Person,
            text = "Account Settings",
            onClick = { navController.navigate("accountSettings") }
        )
        ProfileOption(Icons.Default.Notifications, "Notifications") {}
        ProfileOption(Icons.Default.Help, "Help & Support") {}
        ProfileOption(Icons.Default.Info, "About App") {}

        Spacer(Modifier.height(10.dp))

        ProfileOption(
            icon = Icons.Default.ExitToApp,
            text = "Logout",
            textColor = Color.Red,
            iconTint = Color.Red,
            onClick = { showLogoutDialog = true }
        )
    }
}

// ... (Rest of the file: ProfileOption, EditProfileScreen remains unchanged) ...
@Composable
fun ProfileOption(
    icon: ImageVector,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    iconTint: Color = MaterialTheme.colorScheme.onBackground,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )

        Spacer(Modifier.width(16.dp))

        Text(
            text,
            color = textColor,
            fontSize = 16.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavHostController,
    userId: String,
    profile: UserProfile,
    onSave: (UserProfile) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(profile.name) }
    var email by rememberSaveable { mutableStateOf(profile.email) }
    var avatarUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(profile) {
        name = profile.name
        email = profile.email
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        avatarUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Spacer(Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .shadow(4.dp, CircleShape)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(avatarUri ?: profile.avatar),
                        contentDescription = "Profile Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Avatar",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = (-8).dp, y = (-8).dp)
                    )
                }

                Spacer(Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 16.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(18.dp)
                        )

                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(18.dp)
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (name.isBlank() || email.isBlank()) {
                            Toast.makeText(context, "Name and Email cannot be empty", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isLoading = true

                        coroutineScope.launch {
                            try {
                                val finalAvatarUrl = if (avatarUri != null) {
                                    uploadImageToSupabase(context, avatarUri!!)
                                } else {
                                    profile.avatar
                                }

                                val newProfile = UserProfile(
                                    name = name,
                                    email = email,
                                    avatar = finalAvatarUrl
                                )

                                val success = SupabaseProfileHelper.saveProfile(userId, newProfile)

                                if (success) {
                                    onSave(newProfile)
                                    Toast.makeText(context, "Profile Saved!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                } else {
                                    Toast.makeText(context, "Failed to save profile", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(18.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Save", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    )
}







