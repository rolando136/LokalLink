package com.rolando.locallink.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onBackPressed: () -> Unit,
    onSignUp: () -> Unit,
    onLogin: () -> Unit,
    onGoogle: () -> Unit = {},
    onFacebook: () -> Unit = {}
) {
    val context = LocalContext.current // Context for Toast

    // State Variables
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Loading State
    var isLoading by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF60A5FA),
        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedLabelColor = Color(0xFF60A5FA),
        unfocusedLabelColor = Color(0xFF94A3B8),
        cursorColor = MaterialTheme.colorScheme.surfaceVariant,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Create Your Account",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(15.dp))

            // Email Input
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(15.dp))

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            tint = Color.White,
                            contentDescription = null
                        )
                    }
                },
                shape = RoundedCornerShape(18.dp),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(15.dp))

            // Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            tint = Color.White,
                            contentDescription = null
                        )
                    }
                },
                shape = RoundedCornerShape(18.dp),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Sign Up Button
            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (password != confirmPassword) {
                        Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true

                    CoroutineScope(Dispatchers.Main).launch {
                        val result = SupabaseAuthHelper.signUp(email, password)

                        result.onSuccess {
                            val newUserId = SupabaseAuthHelper.getCurrentUserId()

                            if (newUserId != null) {
                                val newProfile = UserProfile(
                                    id = newUserId,
                                    name = name,
                                    email = email,
                                    avatar = ""
                                )
                                SupabaseProfileHelper.saveProfile(newUserId, newProfile)
                                isLoading = false

                                // ✅ SUCCESS TOAST
                                Toast.makeText(context, "Sign up successful!", Toast.LENGTH_SHORT).show()

                                onSignUp()
                            } else {
                                isLoading = false
                            }
                        }.onFailure { error ->
                            isLoading = false
                            val errorMsg = error.message ?: ""

                            if (errorMsg.contains("already registered", ignoreCase = true)) {
                                Toast.makeText(context, "Account already exists! Please Login.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6),
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Sign Up", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Divider
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f), color = Color.Gray)
                Text("  or  ", color = Color.Gray)
                Divider(modifier = Modifier.weight(1f), color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(26.dp))

            SocialButton(label = "Continue with Google", onClick = onGoogle)
            Spacer(modifier = Modifier.height(12.dp))
            SocialButton(label = "Continue with Facebook", onClick = onFacebook)

            Spacer(modifier = Modifier.height(40.dp))

            Row {
                Text("Already have an account?", color = Color.Gray)
                Spacer(Modifier.width(6.dp))
                Text(
                    "Login",
                    color = Color(0xFF60A5FA),
                    modifier = Modifier.clickable { onLogin() }
                )
            }
        }
    }
}

@Composable
fun SocialButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(width = 0.5.dp, brush = SolidColor(Color.Gray))
    ) {
        Text(label)
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    SignUpScreen({}, {}, {})
}