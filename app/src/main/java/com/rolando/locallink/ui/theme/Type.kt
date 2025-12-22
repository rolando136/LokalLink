package com.rolando.locallink.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rolando.locallink.R

val GoogleSansFlex = FontFamily(
    Font(R.font.google_sans_flex, FontWeight.Normal)
    // Add other weights if you have them
)

val LocalLinkTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = GoogleSansFlex,
        fontSize = 40.sp
    ),
    titleLarge = TextStyle(
        fontFamily = GoogleSansFlex,
        fontSize = 26.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = GoogleSansFlex,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = GoogleSansFlex,
        fontSize = 14.sp
    )
)
