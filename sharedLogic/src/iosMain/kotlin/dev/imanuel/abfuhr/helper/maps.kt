package dev.imanuel.abfuhr.helper

import dev.imanuel.abfuhr.models.AbfallAbcDisposalRoute
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.UIKit.UIApplication

fun AbfallAbcDisposalRoute.openInAppleMaps() {
    val address = listOf(
        street,
        zipcode,
        city
    ).filter { it.isNotBlank() }
        .joinToString(", ")

    if (address.isBlank()) return

    val components = NSURLComponents().apply {
        scheme = "https"
        host = "maps.apple.com"
        queryItems = listOf(
            NSURLQueryItem(name = "q", value = address)
        )
    }

    val url = components.URL ?: return

    UIApplication.sharedApplication.openURL(
        url,
        options = emptyMap<Any?, Any>(),
        completionHandler = null
    )
}
