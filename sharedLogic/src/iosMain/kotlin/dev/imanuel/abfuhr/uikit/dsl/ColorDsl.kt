package dev.imanuel.abfuhr.uikit.dsl

import platform.UIKit.*

/**
 * Utility functions for parsing hex colors and defining adaptive colors in UIKit DSL.
 */

/**
 * Creates a [UIColor] from a hex color string (e.g., "#388E3C", "388E3C", "#80388E3C", or "80388E3C").
 */
fun colorFromHex(hex: String, defaultAlpha: Double = 1.0): UIColor {
    val cleanHex = hex.trim().removePrefix("#")
    val alpha: Double
    val red: Double
    val green: Double
    val blue: Double

    when (cleanHex.length) {
        6 -> {
            val r = cleanHex.substring(0, 2).toIntOrNull(16) ?: 0
            val g = cleanHex.substring(2, 4).toIntOrNull(16) ?: 0
            val b = cleanHex.substring(4, 6).toIntOrNull(16) ?: 0
            red = r / 255.0
            green = g / 255.0
            blue = b / 255.0
            alpha = defaultAlpha
        }

        8 -> {
            val a = cleanHex.substring(0, 2).toIntOrNull(16) ?: 255
            val r = cleanHex.substring(2, 4).toIntOrNull(16) ?: 0
            val g = cleanHex.substring(4, 6).toIntOrNull(16) ?: 0
            val b = cleanHex.substring(6, 8).toIntOrNull(16) ?: 0
            alpha = a / 255.0
            red = r / 255.0
            green = g / 255.0
            blue = b / 255.0
        }

        3 -> {
            val r = cleanHex.substring(0, 1).repeat(2).toIntOrNull(16) ?: 0
            val g = cleanHex.substring(1, 2).repeat(2).toIntOrNull(16) ?: 0
            val b = cleanHex.substring(2, 3).repeat(2).toIntOrNull(16) ?: 0
            red = r / 255.0
            green = g / 255.0
            blue = b / 255.0
            alpha = defaultAlpha
        }

        else -> {
            red = 0.0
            green = 0.0
            blue = 0.0
            alpha = defaultAlpha
        }
    }

    return UIColor(
        red = red,
        green = green,
        blue = blue,
        alpha = alpha
    )
}

/**
 * Application color palette definitions.
 */
object AppColors {
    private const val PRIMARY_HEX: String = "#388E3C"

    val primaryLight: UIColor = colorFromHex(PRIMARY_HEX)

    val primary: UIColor = primaryLight

    val background: UIColor get() = UIColor.systemBackgroundColor()
    val systemBackground: UIColor get() = UIColor.systemBackgroundColor()
    val secondarySystemBackground: UIColor get() = UIColor.secondarySystemBackgroundColor()
    val tertiarySystemBackground: UIColor get() = UIColor.tertiarySystemBackgroundColor()
    val systemGroupedBackground: UIColor get() = UIColor.systemGroupedBackgroundColor()
}
