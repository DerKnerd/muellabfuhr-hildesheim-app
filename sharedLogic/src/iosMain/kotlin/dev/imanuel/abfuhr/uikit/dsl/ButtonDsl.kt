@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.*

@UIKitDsl
class ButtonBuilder(
    buttonType: Long = UIButtonTypeSystem
) {
    val button: UIButton = UIButton.buttonWithType(buttonType)

    var title: String? = null
    var titleColor: UIColor? = null
    var highlightedTitleColor: UIColor? = null
    var disabledTitleColor: UIColor? = null
    var selectedTitleColor: UIColor? = null
    var font: UIFont? = null
    var backgroundColor: UIColor? = null
    var cornerRadius: Double? = null
    var borderWidth: Double? = null
    var borderColor: UIColor? = null
    var tintColor: UIColor? = null
    var image: UIImage? = null
    var isEnabled: Boolean = true
    var isSelected: Boolean = false
    var clipsToBounds: Boolean = true

    private var clickAction: (() -> Unit)? = null
    private var topInset: Double = 0.0
    private var leftInset: Double = 0.0
    private var bottomInset: Double = 0.0
    private var rightInset: Double = 0.0
    private var hasCustomInsets: Boolean = false

    fun systemImage(
        name: String,
        pointSize: Double? = null,
        weight: UIImageSymbolWeight? = null
    ) {
        val config = if (pointSize != null && weight != null) {
            UIImageSymbolConfiguration.configurationWithPointSize(pointSize, weight)
        } else if (pointSize != null) {
            UIImageSymbolConfiguration.configurationWithPointSize(pointSize)
        } else if (weight != null) {
            UIImageSymbolConfiguration.configurationWithWeight(weight)
        } else {
            null
        }

        image = if (config != null) {
            UIImage.systemImageNamed(name, config)
        } else {
            UIImage.systemImageNamed(name)
        }
    }

    fun contentInsets(top: Double, left: Double, bottom: Double, right: Double) {
        this.topInset = top
        this.leftInset = left
        this.bottomInset = bottom
        this.rightInset = right
        this.hasCustomInsets = true
    }

    fun contentPadding(horizontal: Double, vertical: Double) {
        contentInsets(top = vertical, left = horizontal, bottom = vertical, right = horizontal)
    }

    fun contentPadding(all: Double) {
        contentInsets(top = all, left = all, bottom = all, right = all)
    }

    fun horizontalAlignment(alignment: UIControlContentHorizontalAlignment) {
        button.setContentHorizontalAlignment(alignment)
    }

    fun verticalAlignment(alignment: UIControlContentVerticalAlignment) {
        button.setContentVerticalAlignment(alignment)
    }

    fun onClick(action: () -> Unit) {
        this.clickAction = action
    }

    fun build(): UIButton {
        val hasConfig = hasCustomInsets
        if (hasConfig) {
            val config = button.configuration ?: UIButtonConfiguration.plainButtonConfiguration()
            title?.let { config.title = it }
            config.contentInsets = NSDirectionalEdgeInsetsMake(
                top = topInset,
                leading = leftInset,
                bottom = bottomInset,
                trailing = rightInset
            )
            button.configuration = config
        } else {
            title?.let { button.setTitle(it, UIControlStateNormal) }
        }

        title?.let { button.setTitle(it, UIControlStateNormal) }
        titleColor?.let { button.setTitleColor(it, UIControlStateNormal) }
        highlightedTitleColor?.let { button.setTitleColor(it, UIControlStateHighlighted) }
        disabledTitleColor?.let { button.setTitleColor(it, UIControlStateDisabled) }
        selectedTitleColor?.let { button.setTitleColor(it, UIControlStateSelected) }
        font?.let { button.titleLabel?.setFont(it) }
        backgroundColor?.let { button.setBackgroundColor(it) }
        tintColor?.let { button.setTintColor(it) }
        image?.let { button.setImage(it, UIControlStateNormal) }

        cornerRadius?.let {
            button.layer.cornerRadius = it
            button.layer.masksToBounds = clipsToBounds
        }

        borderWidth?.let { button.layer.borderWidth = it }
        borderColor?.let { button.layer.borderColor = it.CGColor }

        button.setEnabled(isEnabled)
        button.setSelected(isSelected)
        button.clipsToBounds = clipsToBounds

        clickAction?.let { action ->
            button.onClick(action)
        }

        return button
    }
}

@UIKitDsl
class IconButtonBuilder {
    var systemName: String? = null
    var image: UIImage? = null
    var iconSize: Double = 20.0
    var size: Double = 44.0
    var tintColor: UIColor? = null
    var backgroundColor: UIColor? = null
    var isCircular: Boolean = true
    var cornerRadius: Double? = null
    var borderWidth: Double? = null
    var borderColor: UIColor? = null
    var isEnabled: Boolean = true

    private var clickAction: (() -> Unit)? = null

    fun onClick(action: () -> Unit) {
        this.clickAction = action
    }

    fun build(): UIButton {
        val button = UIButton.buttonWithType(UIButtonTypeCustom)
        button.setFrame(CGRectMake(0.0, 0.0, size, size))

        val finalImage = image ?: systemName?.let { name ->
            val config = UIImageSymbolConfiguration.configurationWithPointSize(iconSize)
            UIImage.systemImageNamed(name, config)
        }

        finalImage?.let { button.setImage(it, UIControlStateNormal) }
        tintColor?.let { button.setTintColor(it) }
        backgroundColor?.let { button.setBackgroundColor(it) }

        val computedRadius = if (isCircular) {
            size / 2.0
        } else {
            cornerRadius ?: 0.0
        }

        if (computedRadius > 0.0) {
            button.layer.cornerRadius = computedRadius
            button.layer.masksToBounds = true
        }

        borderWidth?.let { button.layer.borderWidth = it }
        borderColor?.let { button.layer.borderColor = it.CGColor }

        button.setEnabled(isEnabled)

        clickAction?.let { action ->
            button.onClick(action)
        }

        return button
    }
}

inline fun button(
    title: String? = null,
    buttonType: Long = UIButtonTypeSystem,
    builder: ButtonBuilder.() -> Unit = {}
): UIButton {
    val b = ButtonBuilder(buttonType)
    if (title != null) b.title = title
    b.builder()
    return b.build()
}

inline fun iconButton(
    systemName: String? = null,
    builder: IconButtonBuilder.() -> Unit = {}
): UIButton {
    val b = IconButtonBuilder()
    if (systemName != null) b.systemName = systemName
    b.builder()
    return b.build()
}

inline fun iconButton(
    image: UIImage,
    builder: IconButtonBuilder.() -> Unit = {}
): UIButton {
    val b = IconButtonBuilder()
    b.image = image
    b.builder()
    return b.build()
}
