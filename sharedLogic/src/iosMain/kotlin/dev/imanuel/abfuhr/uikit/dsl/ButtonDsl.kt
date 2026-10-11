@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import dev.imanuel.abfuhr.helper.hasGlassSupport
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.*

@UIKitDsl
class ButtonBuilder(buttonType: Long = UIButtonTypeSystem) {
    val button: UIButton = UIButton.buttonWithType(buttonType)

    var title: String? = null
    var image: UIImage? = null

    var configuration: UIButtonConfiguration = UIButtonConfiguration.plainButtonConfiguration()

    private var clickAction: (() -> Unit)? = null

    fun systemImage(name: String, pointSize: Double? = null, weight: UIImageSymbolWeight? = null) {
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

    fun horizontalAlignment(alignment: UIControlContentHorizontalAlignment) {
        button.setContentHorizontalAlignment(alignment)
    }

    fun onClick(action: () -> Unit) {
        this.clickAction = action
    }

    fun build(): UIButton {
        title?.let { button.setTitle(it, UIControlStateNormal) }
        image?.let { button.setImage(it, UIControlStateNormal) }
        button.configuration = configuration

        button.clipsToBounds = true

        // Add spacing between image and title when both are present
        if (image != null && title != null) {
            // Prefer UIButtonConfiguration if present
            val existingConfig = button.configuration
            if (existingConfig != null) {
                existingConfig.imagePadding = 4.0
                button.configuration = existingConfig
            } else {
                // Fallback for pre-configuration style buttons
                val inset = 4.0
                button.setTitleEdgeInsets(UIEdgeInsetsMake(0.0, inset, 0.0, -inset))
                button.setImageEdgeInsets(UIEdgeInsetsMake(0.0, -inset, 0.0, inset))
            }
        }

        clickAction?.let { action ->
            button.onClick(action)
        }

        return button
    }
}

@UIKitDsl
class IconButtonBuilder {
    var systemName: String? = null
    var tintColor: UIColor? = null
    var backgroundColor: UIColor? = null
    var isCircular: Boolean = true
    var menu: UIMenu? = null
    var showsMenuAsPrimaryAction: Boolean = false
    var isGlass: Boolean = false

    private var clickAction: (() -> Unit)? = null

    fun onClick(action: () -> Unit) {
        this.clickAction = action
    }

    fun build(): UIButton {
        val button = UIButton.buttonWithType(UIButtonTypeCustom)
        button.translatesAutoresizingMaskIntoConstraints = false
        button.configuration = if (isGlass && hasGlassSupport())
            UIButtonConfiguration.glassButtonConfiguration()
        else
            UIButtonConfiguration.plainButtonConfiguration()
        button.setFrame(CGRectMake(0.0, 0.0, 44.0, 44.0))

        val finalImage = systemName?.let { name ->
            val config = UIImageSymbolConfiguration.configurationWithPointSize(20.0)
            UIImage.systemImageNamed(name, config)
        }

        finalImage?.let { button.setImage(it, UIControlStateNormal) }
        tintColor?.let { button.setTintColor(it) }
        if (!(isGlass && hasGlassSupport())) backgroundColor?.let { button.setBackgroundColor(it) }

        menu?.let { button.menu = it }
        showsMenuAsPrimaryAction.let { button.showsMenuAsPrimaryAction = it }

        if (isCircular) button.layer.cornerRadius = 44.0 / 2.0

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
