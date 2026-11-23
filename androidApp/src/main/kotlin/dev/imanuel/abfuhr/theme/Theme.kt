package dev.imanuel.abfuhr.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.rememberDynamicColorScheme

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable() () -> Unit
) {
    val primaryColorLightMode = Color(0xff388e3c)
    val primaryColorDarkMode = Color(0xff388e3c)

    val seedColor = if (darkTheme) {
        primaryColorDarkMode
    } else {
        primaryColorLightMode
    }
    val tertiarySeedColor = if (darkTheme) {
        primaryColorLightMode
    } else {
        primaryColorDarkMode
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> rememberDynamicColorScheme(
            seedColor = seedColor,
            isDark = darkTheme,
            specVersion = ColorSpec.SpecVersion.SPEC_2026,
            style = PaletteStyle.Cmf(tertiarySeedColor)
        )

    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

