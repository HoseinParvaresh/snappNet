package com.hosein.snappnet.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.hosein.snappnet.R

/** One file on purpose: bold is synthesised from it. Add a real bold file here if you have one. */
val Shabnam = FontFamily(Font(R.font.shabnam_fd))

val Danger = Color(0xFFE5484D)
val Amber = Color(0xFFF59E0B)

@Composable
fun SnappNetTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Color(0xFF2ECC71), onPrimary = Color(0xFF05210F),
            primaryContainer = Color(0xFF143824), onPrimaryContainer = Color(0xFFBFF1D2),
            background = Color(0xFF0E1114), surface = Color(0xFF171B20),
            surfaceVariant = Color(0xFF232932), onSurface = Color(0xFFE8ECF0),
            onSurfaceVariant = Color(0xFFA3ACB5), outline = Color(0xFF3A424C),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF16A34A), onPrimary = Color.White,
            primaryContainer = Color(0xFFDDF6E7), onPrimaryContainer = Color(0xFF0B3D1E),
            background = Color(0xFFF3F5F7), surface = Color.White,
            surfaceVariant = Color(0xFFEBEEF1), onSurface = Color(0xFF111418),
            onSurfaceVariant = Color(0xFF5F6B76), outline = Color(0xFFC5CCD3),
        )
    }
    MaterialTheme(colorScheme = scheme, typography = shabnamTypography(), content = content)
}

private fun shabnamTypography(): Typography {
    val b = Typography()
    return Typography(
        displayLarge = b.displayLarge.copy(fontFamily = Shabnam),
        displayMedium = b.displayMedium.copy(fontFamily = Shabnam),
        displaySmall = b.displaySmall.copy(fontFamily = Shabnam),
        headlineLarge = b.headlineLarge.copy(fontFamily = Shabnam),
        headlineMedium = b.headlineMedium.copy(fontFamily = Shabnam),
        headlineSmall = b.headlineSmall.copy(fontFamily = Shabnam),
        titleLarge = b.titleLarge.copy(fontFamily = Shabnam),
        titleMedium = b.titleMedium.copy(fontFamily = Shabnam),
        titleSmall = b.titleSmall.copy(fontFamily = Shabnam),
        bodyLarge = b.bodyLarge.copy(fontFamily = Shabnam),
        bodyMedium = b.bodyMedium.copy(fontFamily = Shabnam),
        bodySmall = b.bodySmall.copy(fontFamily = Shabnam),
        labelLarge = b.labelLarge.copy(fontFamily = Shabnam),
        labelMedium = b.labelMedium.copy(fontFamily = Shabnam),
        labelSmall = b.labelSmall.copy(fontFamily = Shabnam),
    )
}
