package dev.imanuel.abfuhr.ui.dsl

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSSelectorFromString
import platform.UIKit.NSLineBreakByTruncatingTail
import platform.UIKit.NSLineBreakByWordWrapping
import platform.UIKit.NSLineBreakMode
import platform.UIKit.NSTextAlignment
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.NSTextAlignmentLeft
import platform.UIKit.NSTextAlignmentNatural
import platform.UIKit.NSTextAlignmentRight
import platform.UIKit.UIButton
import platform.UIKit.UIButtonType
import platform.UIKit.UIButtonTypeSystem
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventEditingChanged
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIControlStateNormal
import platform.UIKit.UIEdgeInsets
import platform.UIKit.UIEdgeInsetsMake
import platform.UIKit.UIFont
import platform.UIKit.UIFontWeight
import platform.UIKit.UIFontWeightBold
import platform.UIKit.UIFontWeightMedium
import platform.UIKit.UIFontWeightRegular
import platform.UIKit.UIFontWeightSemibold
import platform.UIKit.UIImage
import platform.UIKit.UIKeyboardType
import platform.UIKit.UIKeyboardTypeDefault
import platform.UIKit.UIKeyboardTypeEmailAddress
import platform.UIKit.UIKeyboardTypeNumberPad
import platform.UIKit.UIKeyboardTypePhonePad
import platform.UIKit.UILabel
import platform.UIKit.UIReturnKeyDone
import platform.UIKit.UIReturnKeySearch
import platform.UIKit.UIReturnKeyType
import platform.UIKit.UITextAutocapitalizationType
import platform.UIKit.UITextAutocapitalizationTypeNone
import platform.UIKit.UITextAutocorrectionType
import platform.UIKit.UITextAutocorrectionTypeNo
import platform.UIKit.UITextBorderStyle
import platform.UIKit.UITextBorderStyleRoundedRect
import platform.UIKit.UITextField
import platform.UIKit.UITextFieldDelegateProtocol
import platform.UIKit.UITextFieldViewMode
import platform.UIKit.UITextFieldViewModeWhileEditing
import platform.darwin.NSObject

// =============================================================================
// 1. TEXT DISPLAY (UILabel)
// =============================================================================

/**
 * DSL Builder for configuring a text display ([UILabel]).
 */
@UIKitDslMarker
class LabelBuilder(
    val label: UILabel = UILabel()
) {
    init {
        label.translatesAutoresizingMaskIntoConstraints = false
        label.numberOfLines = 0
        label.textColor = DslColors.textPrimary
        label.font = DslTypography.body
    }

    var text: String?
        get() = label.text
        set(value) {
            label.text = value
        }

    var textColor: UIColor?
        get() = label.textColor
        set(value) {
            label.textColor = value
        }

    var font: UIFont?
        get() = label.font
        set(value) {
            label.font = value
        }

    var textAlignment: NSTextAlignment
        get() = label.textAlignment
        set(value) {
            label.textAlignment = value
        }

    var numberOfLines: Long
        get() = label.numberOfLines
        set(value) {
            label.numberOfLines = value
        }

    var lineBreakMode: NSLineBreakMode
        get() = label.lineBreakMode
        set(value) {
            label.lineBreakMode = value
        }

    var backgroundColor: UIColor?
        get() = label.backgroundColor
        set(value) {
            label.backgroundColor = value
        }

    var alpha: Double
        get() = label.alpha
        set(value) {
            label.alpha = value
        }

    fun fontSize(size: Double, weight: UIFontWeight = UIFontWeightRegular) {
        label.font = DslTypography.system(size, weight)
    }

    fun bold(size: Double? = null) {
        val currentSize = size ?: label.font?.pointSize ?: 17.0
        label.font = DslTypography.bold(currentSize)
    }

    fun italic(size: Double? = null) {
        val currentSize = size ?: label.font?.pointSize ?: 17.0
        label.font = DslTypography.italic(currentSize)
    }

    fun alignCenter() {
        textAlignment = NSTextAlignmentCenter
    }

    fun alignLeft() {
        textAlignment = NSTextAlignmentLeft
    }

    fun alignRight() {
        textAlignment = NSTextAlignmentRight
    }

    fun singleLine(truncateTail: Boolean = true) {
        numberOfLines = 1
        if (truncateTail) {
            lineBreakMode = NSLineBreakByTruncatingTail
        }
    }

    fun multiline() {
        numberOfLines = 0
        lineBreakMode = NSLineBreakByWordWrapping
    }

    // Typography presets
    fun largeTitle() {
        font = DslTypography.largeTitle
    }

    fun title1() {
        font = DslTypography.title1
    }

    fun title2() {
        font = DslTypography.title2
    }

    fun title3() {
        font = DslTypography.title3
    }

    fun headline() {
        font = DslTypography.headline
    }

    fun body() {
        font = DslTypography.body
    }

    fun subhead() {
        font = DslTypography.subhead
    }

    fun footnote() {
        font = DslTypography.footnote
        textColor = DslColors.textSecondary
    }

    fun caption() {
        font = DslTypography.caption
        textColor = DslColors.textSecondary
    }

    fun build(): UILabel = label
}

/**
 * Creates a text display ([UILabel]) inside the current container scope.
 */
fun ViewScope.label(
    text: String = "",
    init: LabelBuilder.() -> Unit = {}
): UILabel {
    val builder = LabelBuilder().apply {
        this.text = text
        init()
    }
    val view = builder.build()
    add(view)
    return view
}

/**
 * Alias for [label].
 */
fun ViewScope.text(
    text: String = "",
    init: LabelBuilder.() -> Unit = {}
): UILabel = label(text, init)

// =============================================================================
// 2. BUTTONS (UIButton)
// =============================================================================

/**
 * DSL Builder for configuring a [UIButton].
 */
@OptIn(ExperimentalForeignApi::class)
@UIKitDslMarker
class ButtonBuilder(
    val button: UIButton = UIButton.buttonWithType(UIButtonTypeSystem)
) {
    init {
        button.translatesAutoresizingMaskIntoConstraints = false
        button.titleLabel?.font = DslTypography.headline
    }

    var title: String?
        get() = button.titleForState(UIControlStateNormal)
        set(value) {
            button.setTitle(value, forState = UIControlStateNormal)
        }

    var titleColor: UIColor?
        get() = button.titleColorForState(UIControlStateNormal)
        set(value) {
            button.setTitleColor(value, forState = UIControlStateNormal)
        }

    var backgroundColor: UIColor?
        get() = button.backgroundColor
        set(value) {
            button.backgroundColor = value
        }

    var font: UIFont?
        get() = button.titleLabel?.font
        set(value) {
            button.titleLabel?.font = value
        }

    var cornerRadius: Double
        get() = button.layer.cornerRadius
        set(value) {
            button.layer.cornerRadius = value
            button.layer.masksToBounds = value > 0.0
        }

    var isEnabled: Boolean
        get() = button.enabled
        set(value) {
            button.enabled = value
        }

    var tintColor: UIColor?
        get() = button.tintColor
        set(value) {
            button.tintColor = value
        }

    fun border(width: Double, color: UIColor) {
        button.layer.borderWidth = width
        button.layer.borderColor = color.CGColor
    }

    fun contentInsets(top: Double = 0.0, left: Double = 0.0, bottom: Double = 0.0, right: Double = 0.0) {
        button.contentEdgeInsets = UIEdgeInsetsMake(top, left, bottom, right)
    }

    fun contentInsets(horizontal: Double, vertical: Double) {
        contentInsets(top = vertical, left = horizontal, bottom = vertical, right = horizontal)
    }

    fun image(image: UIImage?, forState: ULong = UIControlStateNormal) {
        button.setImage(image, forState = forState)
    }

    /**
     * Attaches an on-click / touch-up-inside handler.
     */
    fun onClick(action: () -> Unit) {
        button.onEvent(UIControlEventTouchUpInside, action)
    }

    fun onTap(action: () -> Unit) = onClick(action)

    // Preset button styles
    fun primaryStyle(
        bg: UIColor = DslColors.primary,
        fg: UIColor = UIColor.whiteColor,
        radius: Double = 10.0
    ) {
        backgroundColor = bg
        titleColor = fg
        cornerRadius = radius
        contentInsets(horizontal = 16.0, vertical = 12.0)
    }

    fun secondaryStyle(
        bg: UIColor = DslColors.secondaryBackground,
        fg: UIColor = DslColors.primary,
        radius: Double = 10.0
    ) {
        backgroundColor = bg
        titleColor = fg
        cornerRadius = radius
        contentInsets(horizontal = 16.0, vertical = 12.0)
    }

    fun outlineStyle(
        borderColor: UIColor = DslColors.primary,
        textColor: UIColor = DslColors.primary,
        borderWidth: Double = 1.5,
        radius: Double = 10.0
    ) {
        backgroundColor = UIColor.clearColor
        titleColor = textColor
        cornerRadius = radius
        border(borderWidth, borderColor)
        contentInsets(horizontal = 16.0, vertical = 12.0)
    }

    fun textStyle(textColor: UIColor = DslColors.primary) {
        backgroundColor = UIColor.clearColor
        titleColor = textColor
        contentInsets(horizontal = 8.0, vertical = 8.0)
    }

    fun build(): UIButton = button
}

/**
 * Creates a button ([UIButton]) inside the current container scope.
 */
fun ViewScope.button(
    title: String = "",
    type: UIButtonType = UIButtonTypeSystem,
    init: ButtonBuilder.() -> Unit = {}
): UIButton {
    val builder = ButtonBuilder(UIButton.buttonWithType(type)).apply {
        this.title = title
        init()
    }
    val view = builder.build()
    add(view)
    return view
}

// =============================================================================
// 3. TEXTFIELDS (UITextField)
// =============================================================================

@OptIn(ExperimentalForeignApi::class)
internal class DslTextFieldDelegate(
    var onTextChanged: ((String) -> Unit)? = null,
    var onEditingDidBegin: (() -> Unit)? = null,
    var onEditingDidEnd: (() -> Unit)? = null,
    var onReturn: (() -> Boolean)? = null
) : NSObject(), UITextFieldDelegateProtocol {

    @ObjCAction
    fun onEditingChanged(sender: UITextField) {
        onTextChanged?.invoke(sender.text ?: "")
    }

    override fun textFieldDidBeginEditing(textField: UITextField) {
        onEditingDidBegin?.invoke()
    }

    override fun textFieldDidEndEditing(textField: UITextField) {
        onEditingDidEnd?.invoke()
    }

    override fun textFieldShouldReturn(textField: UITextField): Boolean {
        val handled = onReturn?.invoke()
        if (handled == null || handled) {
            textField.resignFirstResponder()
            return true
        }
        return false
    }
}

/**
 * DSL Builder for configuring a text field ([UITextField]).
 */
@OptIn(ExperimentalForeignApi::class)
@UIKitDslMarker
class TextFieldBuilder(
    val textField: UITextField = UITextField()
) {
    private val delegate = DslTextFieldDelegate()

    init {
        textField.translatesAutoresizingMaskIntoConstraints = false
        textField.borderStyle = UITextBorderStyleRoundedRect
        textField.font = DslTypography.body
        textField.textColor = DslColors.textPrimary
        textField.delegate = delegate
        DslActionRegistry.retain(textField, delegate)

        textField.addTarget(
            target = delegate,
            action = NSSelectorFromString("onEditingChanged:"),
            forControlEvents = UIControlEventEditingChanged
        )
    }

    var text: String?
        get() = textField.text
        set(value) {
            textField.text = value
        }

    var placeholder: String?
        get() = textField.placeholder
        set(value) {
            textField.placeholder = value
        }

    var textColor: UIColor?
        get() = textField.textColor
        set(value) {
            textField.textColor = value
        }

    var font: UIFont?
        get() = textField.font
        set(value) {
            textField.font = value
        }

    var borderStyle: UITextBorderStyle
        get() = textField.borderStyle
        set(value) {
            textField.borderStyle = value
        }

    var isSecureTextEntry: Boolean
        get() = textField.secureTextEntry
        set(value) {
            textField.secureTextEntry = value
        }

    var keyboardType: UIKeyboardType
        get() = textField.keyboardType
        set(value) {
            textField.keyboardType = value
        }

    var returnKeyType: UIReturnKeyType
        get() = textField.returnKeyType
        set(value) {
            textField.returnKeyType = value
        }

    var clearButtonMode: UITextFieldViewMode
        get() = textField.clearButtonMode
        set(value) {
            textField.clearButtonMode = value
        }

    var autocapitalizationType: UITextAutocapitalizationType
        get() = textField.autocapitalizationType
        set(value) {
            textField.autocapitalizationType = value
        }

    var autocorrectionType: UITextAutocorrectionType
        get() = textField.autocorrectionType
        set(value) {
            textField.autocorrectionType = value
        }

    var backgroundColor: UIColor?
        get() = textField.backgroundColor
        set(value) {
            textField.backgroundColor = value
        }

    var cornerRadius: Double
        get() = textField.layer.cornerRadius
        set(value) {
            textField.layer.cornerRadius = value
            textField.layer.masksToBounds = value > 0.0
        }

    fun border(width: Double, color: UIColor) {
        textField.layer.borderWidth = width
        textField.layer.borderColor = color.CGColor
    }

    fun onTextChanged(action: (String) -> Unit) {
        delegate.onTextChanged = action
    }

    fun onEditingDidBegin(action: () -> Unit) {
        delegate.onEditingDidBegin = action
    }

    fun onEditingDidEnd(action: () -> Unit) {
        delegate.onEditingDidEnd = action
    }

    fun onReturn(action: () -> Boolean) {
        delegate.onReturn = action
    }

    fun onReturnDismiss() {
        delegate.onReturn = {
            textField.resignFirstResponder()
            true
        }
    }

    // Keyboard configurations
    fun emailKeyboard() {
        keyboardType = UIKeyboardTypeEmailAddress
        autocapitalizationType = UITextAutocapitalizationTypeNone
        autocorrectionType = UITextAutocorrectionTypeNo
    }

    fun numberKeyboard() {
        keyboardType = UIKeyboardTypeNumberPad
    }

    fun phoneKeyboard() {
        keyboardType = UIKeyboardTypePhonePad
    }

    fun passwordField() {
        isSecureTextEntry = true
        autocapitalizationType = UITextAutocapitalizationTypeNone
        autocorrectionType = UITextAutocorrectionTypeNo
    }

    fun build(): UITextField = textField
}

/**
 * Creates a textfield ([UITextField]) inside the current container scope.
 */
fun ViewScope.textField(
    placeholder: String = "",
    text: String = "",
    init: TextFieldBuilder.() -> Unit = {}
): UITextField {
    val builder = TextFieldBuilder().apply {
        if (placeholder.isNotEmpty()) this.placeholder = placeholder
        if (text.isNotEmpty()) this.text = text
        init()
    }
    val view = builder.build()
    add(view)
    return view
}
