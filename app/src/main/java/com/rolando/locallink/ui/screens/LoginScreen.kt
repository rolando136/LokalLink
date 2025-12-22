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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navigateToSignup: () -> Unit,
    onLoginClicked: (email: String, password: String) -> Unit,
    onBackPressed: () -> Unit,
    onGoogle: () -> Unit = {},
    onFacebook: () -> Unit = {}
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Loading State
    var isLoading by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF60A5FA),
        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedLabelColor = Color(0xFF60A5FA),
        unfocusedLabelColor = Color(0xFF94A3B8),
        cursorColor = MaterialTheme.colorScheme.onBackground,
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
                text = "Welcome Back",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // EMAIL
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

            // PASSWORD
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

            Spacer(modifier = Modifier.height(30.dp))

            // LOGIN BUTTON
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true

                    CoroutineScope(Dispatchers.Main).launch {
                        val result = SupabaseAuthHelper.login(email, password)

                        isLoading = false

                        result.onSuccess {
                            // ✅ SUCCESS TOAST
                            Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()

                            onLoginClicked(email, password)
                        }.onFailure { error ->
                            val errorMsg = error.message ?: "Login failed"
                            if (errorMsg.contains("Invalid login credentials")) {
                                Toast.makeText(context, "Invalid email or password", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Login", color = Color.White, fontSize = 18.sp)
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

            // Social login buttons
            SocialButton(label = "Continue with Google", onClick = onGoogle)
            Spacer(modifier = Modifier.height(12.dp))
            SocialButton(label = "Continue with Facebook", onClick = onFacebook)

            Spacer(modifier = Modifier.height(40.dp))

            Row {
                Text("Don't have an account?", color = Color.Gray)
                Spacer(Modifier.width(6.dp))
                Text(
                    "Sign Up",
                    color = Color(0xFF60A5FA),
                    modifier = Modifier.clickable { navigateToSignup() }
                )
            }
        }
    }
}
