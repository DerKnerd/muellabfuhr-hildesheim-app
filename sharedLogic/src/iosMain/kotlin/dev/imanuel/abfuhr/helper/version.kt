@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.helper

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import platform.Foundation.NSOperatingSystemVersion
import platform.Foundation.NSProcessInfo

fun hasGlassSupport() = NSProcessInfo.processInfo.isOperatingSystemAtLeastVersion(
    cValue<NSOperatingSystemVersion> {
        majorVersion = 26
        minorVersion = 0
        patchVersion = 0
    }
)

fun isIos18() = NSProcessInfo.processInfo.isOperatingSystemAtLeastVersion(
    cValue<NSOperatingSystemVersion> {
        majorVersion = 18
        minorVersion = 0
        patchVersion = 0
    }
)