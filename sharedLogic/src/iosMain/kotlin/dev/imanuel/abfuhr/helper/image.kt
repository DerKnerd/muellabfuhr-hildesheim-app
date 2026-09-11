@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.helper

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.*

/**
 * Safely resolves an SF Symbol image
 */
fun UIImage.Companion.resolveSystemSymbol(symbolName: String): UIImage? {
    return UIImage.systemImageNamed(symbolName)
}

fun UIImage.resize(width: Int, height: Int): UIImage? {
    val size = CGSizeMake(width.toDouble(), height.toDouble())

    UIGraphicsBeginImageContextWithOptions(
        size = size,
        opaque = false,
        scale = 1.0
    )

    size.useContents {
        drawInRect(CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()))
    }

    val resized = UIGraphicsGetImageFromCurrentImageContext() ?: return null

    UIGraphicsEndImageContext()

    return resized
}

fun UIImage.resizeToFit(maxWidth: Int, maxHeight: Int): UIImage? =
    size.useContents {
        val scale = minOf(
            maxWidth.toDouble() / width,
            maxHeight.toDouble() / height
        )
        return resize(
            (width * scale).toInt(),
            (height * scale).toInt()
        )
    }

fun UIImage.toJpegByteArray(quality: Double = 0.9): ByteArray? {
    val data = UIImageJPEGRepresentation(this, quality)
        ?: return null

    return data.bytes?.readBytes(data.length.toInt())
}