@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.*
import platform.CoreGraphics.CGRectMake
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.OBJC_ASSOCIATION_RETAIN_NONATOMIC
import platform.objc.objc_setAssociatedObject

class SingleLineTextFieldDelegateBridge(
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

fun UITextView.getLineHeight(maxLines: Int): Double {
    val activeFont = font ?: UIFont.systemFontOfSize(16.0)
    val fontHeight = activeFont.lineHeight

    // Total vertical internal padding inside the text container
    val verticalPadding = textContainerInset.useContents { top + bottom }

    return (fontHeight * maxLines) + verticalPadding
}

class MultiLineTextFieldDelegateBridge(
    var onBeginEditingAction: (() -> Unit)? = null,
    var onEndEditingAction: (() -> Unit)? = null
) : NSObject(), UITextViewDelegateProtocol {

    override fun textViewDidBeginEditing(textView: UITextView) {
        onBeginEditingAction?.invoke()
    }

    override fun textViewDidEndEditing(textView: UITextView) {
        onEndEditingAction?.invoke()
    }
}

private val textFieldDelegateKey: COpaquePointer = nativeHeap.alloc<ByteVar>().ptr

@UIKitDsl
class TextFieldBuilder {
    val singleLineTextField: UITextField = UITextField()
    val multiLineTextField: UITextView = UITextView()

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

    private var isMultiLine = false
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
            if (!isMultiLine) singleLineTextField.resignFirstResponder()
            if (isMultiLine) multiLineTextField.resignFirstResponder()
            true
        }
    }

    fun onEditingBegan(action: () -> Unit) {
        this.beginEditingListener = action
    }

    fun onEditingEnded(action: () -> Unit) {
        this.endEditingListener = action
    }

    fun buildSingleLine(): UITextField {
        text?.let { singleLineTextField.setText(it) }
        placeholder?.let { singleLineTextField.setPlaceholder(it) }
        singleLineTextField.setTextAlignment(textAlignment)
        singleLineTextField.setBorderStyle(borderStyle)
        singleLineTextField.setReturnKeyType(returnKeyType)
        singleLineTextField.setAutocapitalizationType(autocapitalizationType)
        singleLineTextField.setAutocorrectionType(autocorrectionType)
        singleLineTextField.setClearButtonMode(clearButtonMode)
        singleLineTextField.setEnabled(isEnabled)

        // Left view / padding
        if (leftPadding != null && leftPadding!! > 0.0) {
            val paddingView = UIView(frame = CGRectMake(0.0, 0.0, leftPadding!!, 1.0))
            singleLineTextField.setLeftView(paddingView)
            singleLineTextField.setLeftViewMode(UITextFieldViewMode.UITextFieldViewModeAlways)
        } else if (leftView != null) {
            singleLineTextField.setLeftView(leftView)
            singleLineTextField.setLeftViewMode(leftViewMode)
        }

        // Right view / padding
        if (rightPadding != null && rightPadding!! > 0.0) {
            val paddingView = UIView(frame = CGRectMake(0.0, 0.0, rightPadding!!, 1.0))
            singleLineTextField.setRightView(paddingView)
            singleLineTextField.setRightViewMode(UITextFieldViewMode.UITextFieldViewModeAlways)
        } else if (rightView != null) {
            singleLineTextField.setRightView(rightView)
            singleLineTextField.setRightViewMode(rightViewMode)
        }

        // Setup Delegate Bridge if listeners are set
        if (returnListener != null || beginEditingListener != null || endEditingListener != null) {
            val delegateBridge = SingleLineTextFieldDelegateBridge(
                onReturnAction = returnListener,
                onBeginEditingAction = beginEditingListener,
                onEndEditingAction = endEditingListener
            )
            singleLineTextField.setDelegate(delegateBridge)
            objc_setAssociatedObject(
                singleLineTextField,
                textFieldDelegateKey,
                delegateBridge,
                OBJC_ASSOCIATION_RETAIN_NONATOMIC
            )
        }

        textChangedListener?.let { listener ->
            singleLineTextField.onEvent(UIControlEventEditingChanged) {
                listener(singleLineTextField.text ?: "")
            }
        }

        return singleLineTextField
    }

    fun buildMultiLine(): UITextView {
        text?.let { multiLineTextField.setText(it) }
        multiLineTextField.setTextAlignment(textAlignment)
        multiLineTextField.setReturnKeyType(returnKeyType)
        multiLineTextField.setAutocapitalizationType(autocapitalizationType)
        multiLineTextField.setAutocorrectionType(autocorrectionType)
        multiLineTextField.setEditable(isEnabled)
        multiLineTextField.translatesAutoresizingMaskIntoConstraints = false
        multiLineTextField.font = UIFont.systemFontOfSize(16.0)
        multiLineTextField.layer.borderWidth = 1.0
        multiLineTextField.layer.borderColor = UIColor.systemGray4Color.CGColor
        multiLineTextField.layer.cornerRadius = 5.0
        multiLineTextField.scrollEnabled = true
        multiLineTextField.heightAnchor.constraintLessThanOrEqualToConstant(multiLineTextField.getLineHeight(4)).active = true
        multiLineTextField.heightAnchor.constraintGreaterThanOrEqualToConstant(multiLineTextField.getLineHeight(2)).active = true

        // Setup Delegate Bridge if listeners are set
        if (beginEditingListener != null || endEditingListener != null) {
            val delegateBridge = MultiLineTextFieldDelegateBridge(
                onBeginEditingAction = beginEditingListener,
                onEndEditingAction = endEditingListener
            )
            multiLineTextField.setDelegate(delegateBridge)
            objc_setAssociatedObject(
                multiLineTextField,
                textFieldDelegateKey,
                delegateBridge,
                OBJC_ASSOCIATION_RETAIN_NONATOMIC
            )
        }

        return multiLineTextField
    }
}

inline fun singleLineTextField(
    placeholder: String? = null,
    text: String? = null,
    builder: TextFieldBuilder.() -> Unit = {}
): UITextField {
    val b = TextFieldBuilder()
    if (placeholder != null) b.placeholder = placeholder
    if (text != null) b.text = text
    b.builder()
    return b.buildSingleLine()
}

inline fun multiLineTextField(
    placeholder: String? = null,
    text: String? = null,
    builder: TextFieldBuilder.() -> Unit = {}
): UITextView {
    val b = TextFieldBuilder()
    if (placeholder != null) b.placeholder = placeholder
    if (text != null) b.text = text
    b.builder()
    return b.buildMultiLine()
}
