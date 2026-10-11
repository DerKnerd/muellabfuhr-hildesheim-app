@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.*
import platform.CoreGraphics.CGRectMake
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.OBJC_ASSOCIATION_RETAIN_NONATOMIC
import platform.objc.objc_setAssociatedObject

class SingleLineTextFieldDelegateBridge(var onReturnAction: (() -> Boolean)? = null) : NSObject(),
    UITextFieldDelegateProtocol {

    override fun textFieldShouldReturn(textField: UITextField): Boolean {
        return onReturnAction?.invoke() ?: run {
            textField.resignFirstResponder()
            true
        }
    }
}

fun UITextView.getLineHeight(maxLines: Int): Double {
    val activeFont = font ?: UIFont.systemFontOfSize(16.0)
    val fontHeight = activeFont.lineHeight

    // Total vertical internal padding inside the text container
    val verticalPadding = textContainerInset.useContents { top + bottom }

    return (fontHeight * maxLines) + verticalPadding
}

class MultiLineTextFieldDelegateBridge(var onTextChanged: ((String) -> Unit)? = null) : NSObject(),
    UITextViewDelegateProtocol {

    override fun textViewDidChange(textView: UITextView) {
        onTextChanged?.invoke(textView.text)
    }
}

private val textFieldDelegateKey: COpaquePointer = nativeHeap.alloc<ByteVar>().ptr

@UIKitDsl
class TextFieldBuilder {
    val singleLineTextField = UITextField()
    val multiLineTextField = UITextView()

    var placeholder: String? = null
    private val textAlignment = NSTextAlignmentNatural
    var autocapitalizationType = UITextAutocapitalizationType.UITextAutocapitalizationTypeNone
    var autocorrectionType = UITextAutocorrectionType.UITextAutocorrectionTypeDefault
    var rightView: UIView? = null
    var rightViewMode = UITextFieldViewMode.UITextFieldViewModeAlways
    var rightPadding: Double? = null

    private var textChangedListener: ((String) -> Unit)? = null

    fun onTextChanged(action: (String) -> Unit) {
        this.textChangedListener = action
    }

    fun buildSingleLine(): UITextField {
        placeholder?.let {
            singleLineTextField.setPlaceholder(it)
            singleLineTextField.setAccessibilityLabel(it)
        }

        if (rightView != null) {
            val accessory: UIView = if (rightPadding != null && rightPadding!! > 0.0) {
                // Wrap provided view and add spacer on the left to separate from text
                row(spacing = rightPadding!!, alignment = UIStackViewAlignmentCenter) {
                    add(this@TextFieldBuilder.rightView!!)
                }
            } else {
                rightView!!
            }
            singleLineTextField.setRightView(accessory)
            singleLineTextField.setRightViewMode(rightViewMode)
        } else if (rightPadding != null && rightPadding!! > 0.0) {
            val paddingView = UIView(frame = CGRectMake(0.0, 0.0, rightPadding!!, 1.0))
            singleLineTextField.setRightView(paddingView)
            singleLineTextField.setRightViewMode(UITextFieldViewMode.UITextFieldViewModeAlways)
        }

        singleLineTextField.textAlignment = textAlignment
        singleLineTextField.borderStyle = UITextBorderStyle.UITextBorderStyleRoundedRect
        singleLineTextField.autocapitalizationType = autocapitalizationType
        singleLineTextField.autocorrectionType = autocorrectionType

        textChangedListener?.let { listener ->
            singleLineTextField.onEvent(UIControlEventEditingChanged) {
                listener(singleLineTextField.text ?: "")
            }
        }

        return singleLineTextField
    }

    fun buildMultiLine(): UITextView {
        multiLineTextField.textAlignment = textAlignment
        multiLineTextField.autocapitalizationType = autocapitalizationType
        multiLineTextField.autocorrectionType = autocorrectionType
        multiLineTextField.borderStyle = UITextViewBorderStyle.UITextViewBorderStyleRoundedRect
        multiLineTextField.translatesAutoresizingMaskIntoConstraints = false
        multiLineTextField.layer.borderWidth = 1.0
        multiLineTextField.layer.borderColor = UIColor.systemGray4Color.CGColor
        multiLineTextField.layer.cornerRadius = 5.0
        multiLineTextField.scrollEnabled = true
        multiLineTextField.heightAnchor.constraintLessThanOrEqualToConstant(multiLineTextField.getLineHeight(4)).active =
            true
        multiLineTextField.heightAnchor.constraintGreaterThanOrEqualToConstant(multiLineTextField.getLineHeight(2)).active =
            true

        if (textChangedListener != null) {
            val delegateBridge = MultiLineTextFieldDelegateBridge(
                onTextChanged = textChangedListener
            )
            multiLineTextField.setDelegate(delegateBridge)
            objc_setAssociatedObject(
                multiLineTextField, textFieldDelegateKey, delegateBridge, OBJC_ASSOCIATION_RETAIN_NONATOMIC
            )
        }

        return multiLineTextField
    }
}

inline fun singleLineTextField(
    placeholder: String? = null,
    builder: TextFieldBuilder.() -> Unit = {}
): UITextField {
    val b = TextFieldBuilder()
    if (placeholder != null) b.placeholder = placeholder
    b.builder()
    return b.buildSingleLine()
}

inline fun multiLineTextField(
    builder: TextFieldBuilder.() -> Unit = {}
): UITextView {
    val b = TextFieldBuilder()
    b.builder()
    return b.buildMultiLine()
}
