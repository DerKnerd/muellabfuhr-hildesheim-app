@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.UIKit.*

@UIKitDsl
enum class ProgressIndicatorStyle {
    Medium,
    Large
}

private fun ProgressIndicatorStyle.toNativeStyle(): UIActivityIndicatorViewStyle {
    return when (this) {
        ProgressIndicatorStyle.Medium -> UIActivityIndicatorViewStyleMedium
        ProgressIndicatorStyle.Large -> UIActivityIndicatorViewStyleLarge
    }
}

@UIKitDsl
class ActivityIndicatorBuilder(
    var style: UIActivityIndicatorViewStyle = UIActivityIndicatorViewStyleMedium
) {
    val indicatorView: UIActivityIndicatorView = UIActivityIndicatorView(activityIndicatorStyle = style)

    var color: UIColor? = UIColor.blueColor
    var hidesWhenStopped: Boolean = true
    var isAnimating: Boolean = true
    var scale: Double? = null
    var backgroundColor: UIColor? = null

    fun style(indicatorStyle: ProgressIndicatorStyle) {
        this.style = indicatorStyle.toNativeStyle()
        this.indicatorView.setActivityIndicatorViewStyle(this.style)
    }

    fun startAnimating() {
        indicatorView.startAnimating()
    }

    fun build(): UIActivityIndicatorView {
        indicatorView.setActivityIndicatorViewStyle(style)
        color?.let { indicatorView.setColor(it) }
        indicatorView.setHidesWhenStopped(hidesWhenStopped)
        backgroundColor?.let { indicatorView.setBackgroundColor(it) }

        scale?.let { factor ->
            indicatorView.setTransform(CGAffineTransformMakeScale(factor, factor))
        }

        if (isAnimating) {
            indicatorView.startAnimating()
        } else {
            indicatorView.stopAnimating()
        }

        return indicatorView
    }
}

inline fun activityIndicator(
    style: UIActivityIndicatorViewStyle = UIActivityIndicatorViewStyleMedium,
    color: UIColor? = null,
    isAnimating: Boolean = true,
    builder: ActivityIndicatorBuilder.() -> Unit = {}
): UIActivityIndicatorView {
    val b = ActivityIndicatorBuilder(style)
    if (color != null) b.color = color
    b.isAnimating = isAnimating
    b.builder()
    return b.build()
}
