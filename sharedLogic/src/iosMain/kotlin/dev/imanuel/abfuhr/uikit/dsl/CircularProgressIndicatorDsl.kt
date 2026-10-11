@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIActivityIndicatorView
import platform.UIKit.UIActivityIndicatorViewStyle
import platform.UIKit.UIActivityIndicatorViewStyleLarge
import platform.UIKit.UIActivityIndicatorViewStyleMedium

@UIKitDsl
enum class ProgressIndicatorStyle {
    Medium,
    Large;

    fun toNativeStyle(): UIActivityIndicatorViewStyle {
        return when (this) {
            Medium -> UIActivityIndicatorViewStyleMedium
            Large -> UIActivityIndicatorViewStyleLarge
        }
    }
}

@UIKitDsl
class ActivityIndicatorBuilder(
    var style: UIActivityIndicatorViewStyle = UIActivityIndicatorViewStyleMedium
) {
    val indicatorView: UIActivityIndicatorView = UIActivityIndicatorView(activityIndicatorStyle = style)

    var isAnimating: Boolean = true

    fun style(indicatorStyle: ProgressIndicatorStyle) {
        this.style = indicatorStyle.toNativeStyle()
        this.indicatorView.setActivityIndicatorViewStyle(this.style)
    }

    fun startAnimating() {
        indicatorView.startAnimating()
    }

    fun build(): UIActivityIndicatorView {
        indicatorView.translatesAutoresizingMaskIntoConstraints = false

        indicatorView.setActivityIndicatorViewStyle(style)
        indicatorView.setHidesWhenStopped(true)

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
    isAnimating: Boolean = true,
    builder: ActivityIndicatorBuilder.() -> Unit = {}
): UIActivityIndicatorView {
    val b = ActivityIndicatorBuilder(style)
    b.isAnimating = isAnimating
    b.builder()
    return b.build()
}
