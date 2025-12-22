package com.rolando.locallink.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rolando.locallink.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navigateToSignup: () -> Unit,
    navigateToLogin: () -> Unit,
    navigateToHome: () -> Unit // 👈 Add this parameter
) {
    // State to hide buttons while checking session
    var isLoading by remember { mutableStateOf(true) }

    // 👇 AUTO-LOGIN LOGIC
    LaunchedEffect(Unit) {
        // Wait for Supabase to restore the session
        val isLoggedIn = SupabaseAuthHelper.isUserLoggedIn()

        if (isLoggedIn) {
            navigateToHome()
        } else {
            // No user found, show buttons
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111827))
    ) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.splashbg),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black)
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Center Logo & Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF3B82F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_storefront),
                            contentDescription = "Storefront",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = buildAnnotatedString {
                        append("Lokal")
                        withStyle(style = SpanStyle(color = Color(0xFF3B82F6))) {
                            append("Link")
                        }
                    },
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Text(
                    text = "Your Community Marketplace",
                    color = Color(0xFFe5e7eb),
                    fontSize = 18.sp
                )
            }

            // 👇 SHOW BUTTONS ONLY IF NOT LOADING
            if (!isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = navigateToSignup,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth()
                            .height(55.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Sign Up", color = Color.White, fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row {
                        Text(
                            text = "Already have an account? ",
                            color = Color(0xFFD1D5DB),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Login",
                            color = Color(0xFF60A5FA),
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { navigateToLogin() }
                        )
                    }
                    Spacer(modifier = Modifier.height(35.dp))
                }
            } else {
                // Optional: Show simple spinner while checking
                CircularProgressIndicator(color = Color(0xFF3B82F6))
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}
