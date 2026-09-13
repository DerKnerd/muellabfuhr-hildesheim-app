@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.*
import platform.Foundation.NSAttributedString
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.OBJC_ASSOCIATION_RETAIN_NONATOMIC
import platform.objc.objc_setAssociatedObject

class TextViewDelegateBridge(
    var onTextChangedAction: ((String) -> Unit)? = null,
) : NSObject(), UITextViewDelegateProtocol {

    override fun textViewDidChange(textView: UITextView) {
        val currentText = textView.text
        onTextChangedAction?.invoke(currentText)
    }
}

private val textViewDelegateKey: COpaquePointer = nativeHeap.alloc<ByteVar>().ptr

@UIKitDsl
class TextViewBuilder {
    val textView: UITextView = UITextView()

    var text: String? = null
    var attributedText: NSAttributedString? = null
    var placeholder: String? = null
    var placeholderColor: UIColor? = null
    var textColor: UIColor? = null
    var font: UIFont? = null
    var textAlignment: NSTextAlignment = NSTextAlignmentNatural
    var isEditable: Boolean = false
    var isSelectable: Boolean = true
    var isScrollEnabled: Boolean = false
    var backgroundColor: UIColor? = null
    var tintColor: UIColor? = null
    var cornerRadius: Double? = null
    var borderColor: UIColor? = null
    var borderWidth: Double? = null
    var insets: CValue<UIEdgeInsets>? = null

    var keyboardType: UIKeyboardType = UIKeyboardTypeDefault
    var returnKeyType: UIReturnKeyType = UIReturnKeyType.UIReturnKeyDefault
    var autocapitalizationType: UITextAutocapitalizationType =
        UITextAutocapitalizationType.UITextAutocapitalizationTypeNone
    var autocorrectionType: UITextAutocorrectionType = UITextAutocorrectionType.UITextAutocorrectionTypeDefault
    var dataDetectorTypes: UIDataDetectorTypes = UIDataDetectorTypeNone

    private var textChangedListener: ((String) -> Unit)? = null

    fun padding(top: Double = 8.0, left: Double = 8.0, bottom: Double = 8.0, right: Double = 8.0) {
        this.insets = UIEdgeInsetsMake(top, left, bottom, right)
    }

    fun padding(all: Double) {
        this.insets = UIEdgeInsetsMake(all, all, all, all)
    }

    fun padding(horizontal: Double, vertical: Double) {
        this.insets = UIEdgeInsetsMake(vertical, horizontal, vertical, horizontal)
    }

    fun onTextChanged(action: (String) -> Unit) {
        this.textChangedListener = action
    }

    fun build(): UITextView {
        text?.let { textView.setText(it) }
        attributedText?.let { textView.setAttributedText(it) }
        textColor?.let { textView.setTextColor(it) }
        font?.let { textView.setFont(it) }
        textView.setTextAlignment(textAlignment)
        textView.setEditable(isEditable)
        textView.setSelectable(isSelectable)
        textView.setScrollEnabled(isScrollEnabled)
        backgroundColor?.let { textView.setBackgroundColor(it) }
        tintColor?.let { textView.setTintColor(it) }
        textView.setKeyboardType(keyboardType)
        textView.setReturnKeyType(returnKeyType)
        textView.setAutocapitalizationType(autocapitalizationType)
        textView.setAutocorrectionType(autocorrectionType)
        textView.setDataDetectorTypes(dataDetectorTypes)

        cornerRadius?.let {
            textView.layer.cornerRadius = it
            textView.layer.masksToBounds = true
        }

        borderWidth?.let { textView.layer.borderWidth = it }
        borderColor?.let { textView.layer.borderColor = it.CGColor }

        insets?.let {
            textView.setTextContainerInset(it)
        }

        // Placeholder support
        var placeholderLabel: UILabel? = null
        if (!placeholder.isNullOrEmpty()) {
            val pLabel = UILabel()
            pLabel.text = placeholder
            pLabel.font = font ?: UIFont.systemFontOfSize(14.0)
            pLabel.textColor = placeholderColor ?: UIColor.lightGrayColor
            pLabel.numberOfLines = 0
            pLabel.setTranslatesAutoresizingMaskIntoConstraints(false)
            pLabel.setHidden(textView.text.isNotEmpty())

            textView.addSubview(pLabel)
            val effectiveInsets = insets ?: textView.textContainerInset
            val (topVal, leftVal, rightVal) = effectiveInsets.useContents {
                Triple(top, left, right)
            }
            NSLayoutConstraint.activateConstraints(
                listOf(
                    pLabel.topAnchor.constraintEqualToAnchor(textView.topAnchor, constant = topVal),
                    pLabel.leadingAnchor.constraintEqualToAnchor(textView.leadingAnchor, constant = leftVal + 5.0),
                    pLabel.trailingAnchor.constraintEqualToAnchor(textView.trailingAnchor, constant = -rightVal),
                    pLabel.widthAnchor.constraintLessThanOrEqualToAnchor(
                        textView.widthAnchor, constant = -(leftVal + rightVal + 10.0)
                    )
                )
            )
            placeholderLabel = pLabel
        }

        // Setup Delegate Bridge
        if (textChangedListener != null || placeholderLabel != null) {
            val delegateBridge = TextViewDelegateBridge(
                onTextChangedAction = textChangedListener,
            )
            textView.setDelegate(delegateBridge)
            objc_setAssociatedObject(textView, textViewDelegateKey, delegateBridge, OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }

        return textView
    }
}

inline fun textView(
    text: String? = null, placeholder: String? = null, builder: TextViewBuilder.() -> Unit = {}
): UITextView {
    val b = TextViewBuilder()
    if (text != null) b.text = text
    if (placeholder != null) b.placeholder = placeholder
    b.builder()
    return b.build()
}

inline fun scrollableTextView(
    text: String? = null, placeholder: String? = null, builder: TextViewBuilder.() -> Unit = {}
): UITextView {
    return textView(text = text, placeholder = placeholder) {
        isScrollEnabled = true
        builder()
    }
}
