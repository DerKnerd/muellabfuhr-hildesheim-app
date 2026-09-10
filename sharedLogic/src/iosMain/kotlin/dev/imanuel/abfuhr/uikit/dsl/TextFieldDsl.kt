@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.*
import platform.CoreGraphics.CGRectMake
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.OBJC_ASSOCIATION_RETAIN_NONATOMIC
import platform.objc.objc_setAssociatedObject

class TextFieldDelegateBridge(
    var onReturnAction: (() -> Boolean)? = null,
    var onBeginEditingAction: (() -> Unit)? = null,
    var onEndEditingAction: (() -> Unit)? = null
) : NSObject(), UITextFieldDelegateProtocol {

    override fun textFieldShouldReturn(textField: UITextField): Boolean {
        return onReturnAction?.invoke() ?: run {
            textField.resignFirstResponder()
            true
        }
    }

    override fun textFieldDidBeginEditing(textField: UITextField) {
        onBeginEditingAction?.invoke()
    }

    override fun textFieldDidEndEditing(textField: UITextField) {
        onEndEditingAction?.invoke()
    }
}

private val textFieldDelegateKey: COpaquePointer = nativeHeap.alloc<ByteVar>().ptr

@UIKitDsl
class TextFieldBuilder {
    val textField: UITextField = UITextField()

    var text: String? = null
    var placeholder: String? = null
    var textAlignment: NSTextAlignment = NSTextAlignmentNatural
    var borderStyle: UITextBorderStyle = UITextBorderStyle.UITextBorderStyleRoundedRect
    var returnKeyType: UIReturnKeyType = UIReturnKeyType.UIReturnKeyDefault
    var autocapitalizationType: UITextAutocapitalizationType =
        UITextAutocapitalizationType.UITextAutocapitalizationTypeNone
    var autocorrectionType: UITextAutocorrectionType = UITextAutocorrectionType.UITextAutocorrectionTypeDefault
    var clearButtonMode: UITextFieldViewMode = UITextFieldViewMode.UITextFieldViewModeNever
    var isEnabled: Boolean = true

    var leftView: UIView? = null
    var leftViewMode: UITextFieldViewMode = UITextFieldViewMode.UITextFieldViewModeAlways
    var rightView: UIView? = null
    var rightViewMode: UITextFieldViewMode = UITextFieldViewMode.UITextFieldViewModeAlways
    var leftPadding: Double? = null
    var rightPadding: Double? = null

    private var textChangedListener: ((String) -> Unit)? = null
    private var returnListener: (() -> Boolean)? = null
    private var beginEditingListener: (() -> Unit)? = null
    private var endEditingListener: (() -> Unit)? = null

    fun onTextChanged(action: (String) -> Unit) {
        this.textChangedListener = action
    }

    fun onReturn(action: () -> Boolean) {
        this.returnListener = action
    }

    fun onReturnDismissKeyboard() {
        this.returnListener = {
            textField.resignFirstResponder()
            true
        }
    }

    fun onEditingBegan(action: () -> Unit) {
        this.beginEditingListener = action
    }

    fun onEditingEnded(action: () -> Unit) {
        this.endEditingListener = action
    }

    fun build(): UITextField {
        text?.let { textField.setText(it) }
        placeholder?.let { textField.setPlaceholder(it) }
        textField.setTextAlignment(textAlignment)
        textField.setBorderStyle(borderStyle)
        textField.setReturnKeyType(returnKeyType)
        textField.setAutocapitalizationType(autocapitalizationType)
        textField.setAutocorrectionType(autocorrectionType)
        textField.setClearButtonMode(clearButtonMode)
        textField.setEnabled(isEnabled)

        // Left view / padding
        if (leftPadding != null && leftPadding!! > 0.0) {
            val paddingView = UIView(frame = CGRectMake(0.0, 0.0, leftPadding!!, 1.0))
            textField.setLeftView(paddingView)
            textField.setLeftViewMode(UITextFieldViewMode.UITextFieldViewModeAlways)
        } else if (leftView != null) {
            textField.setLeftView(leftView)
            textField.setLeftViewMode(leftViewMode)
        }

        // Right view / padding
        if (rightPadding != null && rightPadding!! > 0.0) {
            val paddingView = UIView(frame = CGRectMake(0.0, 0.0, rightPadding!!, 1.0))
            textField.setRightView(paddingView)
            textField.setRightViewMode(UITextFieldViewMode.UITextFieldViewModeAlways)
        } else if (rightView != null) {
            textField.setRightView(rightView)
            textField.setRightViewMode(rightViewMode)
        }

        // Setup Delegate Bridge if listeners are set
        if (returnListener != null || beginEditingListener != null || endEditingListener != null) {
            val delegateBridge = TextFieldDelegateBridge(
                onReturnAction = returnListener,
                onBeginEditingAction = beginEditingListener,
                onEndEditingAction = endEditingListener
            )
            textField.setDelegate(delegateBridge)
            objc_setAssociatedObject(textField, textFieldDelegateKey, delegateBridge, OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }

        textChangedListener?.let { listener ->
            textField.onEvent(UIControlEventEditingChanged) {
                listener(textField.text ?: "")
            }
        }

        return textField
    }
}

inline fun textField(
    placeholder: String? = null,
    text: String? = null,
    builder: TextFieldBuilder.() -> Unit = {}
): UITextField {
    val b = TextFieldBuilder()
    if (placeholder != null) b.placeholder = placeholder
    if (text != null) b.text = text
    b.builder()
    return b.build()
}
