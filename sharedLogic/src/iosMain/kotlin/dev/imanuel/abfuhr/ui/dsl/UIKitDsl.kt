package dev.imanuel.abfuhr.ui.dsl

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIColor
import platform.UIKit.UIControl
import platform.UIKit.UIControlEventEditingChanged
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIControlEvents
import platform.UIKit.UIFont
import platform.UIKit.UIFontWeight
import platform.UIKit.UIFontWeightBold
import platform.UIKit.UIFontWeightMedium
import platform.UIKit.UIFontWeightRegular
import platform.UIKit.UIFontWeightSemibold
import platform.UIKit.UIView
import platform.darwin.NSObject

/**
 * DSL Marker to prevent nested scope bleeding in the UIKit DSL.
 */
@DslMarker
annotation class UIKitDslMarker

/**
 * Base interface for all container builders that can host subviews.
 */
@UIKitDslMarker
interface ViewScope {
    /**
     * Adds a subview to this container.
     */
    fun add(view: UIView)

    /**
     * Unary plus operator to allow adding pre-constructed views easily: `+myCustomView`.
     */
    operator fun UIView.unaryPlus() {
        add(this)
    }
}

/**
 * Retains target-action callback handlers to prevent garbage collection
 * during the lifetime of UIKit controls.
 */
internal object DslActionRegistry {
    private val retainedTargets = mutableMapOf<NSObject, MutableList<NSObject>>()

    fun retain(owner: NSObject, target: NSObject) {
        val list = retainedTargets.getOrPut(owner) { mutableListOf() }
        list.add(target)
    }

    fun release(owner: NSObject) {
        retainedTargets.remove(owner)
    }
}

/**
 * Internal target-action bridge for zero-argument callbacks (e.g., button clicks).
 */
@OptIn(ExperimentalForeignApi::class)
internal class DslActionTarget(
    private val action: () -> Unit
) : NSObject() {
    @ObjCAction
    fun trigger() {
        action()
    }
}

/**
 * Internal target-action bridge for UIControl event callbacks with sender.
 */
@OptIn(ExperimentalForeignApi::class)
internal class DslControlActionTarget<T : UIControl>(
    private val action: (T) -> Unit
) : NSObject() {
    @ObjCAction
    fun trigger(sender: UIControl) {
        @Suppress("UNCHECKED_CAST")
        action(sender as T)
    }
}

/**
 * Helper extension to safely attach click/tap actions to any UIControl.
 */
@OptIn(ExperimentalForeignApi::class)
fun UIControl.onEvent(events: UIControlEvents = UIControlEventTouchUpInside, action: () -> Unit) {
    val target = DslActionTarget(action)
    DslActionRegistry.retain(this, target)
    this.addTarget(
        target = target,
        action = NSSelectorFromString("trigger"),
        forControlEvents = events
    )
}

/**
 * Color utility helpers for the DSL.
 */
object DslColors {
    fun rgb(red: Double, green: Double, blue: Double, alpha: Double = 1.0): UIColor {
        return UIColor.colorWithRed(
            red = red / 255.0,
            green = green / 255.0,
            blue = blue / 255.0,
            alpha = alpha
        )
    }

    fun hex(hex: Long, alpha: Double = 1.0): UIColor {
        val red = ((hex shr 16) and 0xFF).toDouble()
        val green = ((hex shr 8) and 0xFF).toDouble()
        val blue = (hex and 0xFF).toDouble()
        return rgb(red, green, blue, alpha)
    }

    val primary: UIColor get() = rgb(33.0, 150.0, 243.0)
    val background: UIColor get() = UIColor.systemBackgroundColor
    val secondaryBackground: UIColor get() = UIColor.secondarySystemBackgroundColor
    val textPrimary: UIColor get() = UIColor.labelColor
    val textSecondary: UIColor get() = UIColor.secondaryLabelColor
    val separator: UIColor get() = UIColor.separatorColor
}

/**
 * Typography utility helpers for the DSL.
 */
object DslTypography {
    fun system(size: Double, weight: UIFontWeight = UIFontWeightRegular): UIFont {
        return UIFont.systemFontOfSize(size, weight)
    }

    fun bold(size: Double): UIFont = UIFont.boldSystemFontOfSize(size)
    fun italic(size: Double): UIFont = UIFont.italicSystemFontOfSize(size)

    val largeTitle: UIFont get() = system(34.0, UIFontWeightBold)
    val title1: UIFont get() = system(28.0, UIFontWeightBold)
    val title2: UIFont get() = system(22.0, UIFontWeightBold)
    val title3: UIFont get() = system(20.0, UIFontWeightSemibold)
    val headline: UIFont get() = system(17.0, UIFontWeightSemibold)
    val body: UIFont get() = system(17.0, UIFontWeightRegular)
    val callout: UIFont get() = system(16.0, UIFontWeightRegular)
    val subhead: UIFont get() = system(15.0, UIFontWeightRegular)
    val footnote: UIFont get() = system(13.0, UIFontWeightRegular)
    val caption: UIFont get() = system(12.0, UIFontWeightRegular)
}
