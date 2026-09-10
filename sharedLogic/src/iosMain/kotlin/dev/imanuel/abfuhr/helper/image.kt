package dev.imanuel.abfuhr.helper

import platform.UIKit.UIImage

/**
 * Safely resolves an SF Symbol image
 */
fun UIImage.Companion.resolveSystemSymbol(symbolName: String): UIImage? {
    return UIImage.systemImageNamed(symbolName)
}
