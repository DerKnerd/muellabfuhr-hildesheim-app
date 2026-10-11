@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.*
import platform.Foundation.NSAttributedString
import platform.UIKit.*

@UIKitDsl
class TextViewBuilder {
    val textView = UITextView()

    var font = UIFont.preferredFontForTextStyle(UIFontTextStyleBody)
    var text: String? = null
    var attributedText: NSAttributedString? = null
    var textColor: UIColor? = null
    var textAlignment: NSTextAlignment = NSTextAlignmentNatural
    var isSelectable: Boolean = true
    var isScrollEnabled: Boolean = false
    var insets: CValue<UIEdgeInsets>? = null

    var dataDetectorTypes: UIDataDetectorTypes = UIDataDetectorTypeNone

    fun padding(top: Double = 8.0, left: Double = 8.0, bottom: Double = 8.0, right: Double = 8.0) {
        this.insets = UIEdgeInsetsMake(top, left, bottom, right)
    }

    fun padding(all: Double) {
        this.insets = UIEdgeInsetsMake(all, all, all, all)
    }

    fun padding(horizontal: Double, vertical: Double) {
        this.insets = UIEdgeInsetsMake(vertical, horizontal, vertical, horizontal)
    }

    fun build(): UITextView {
        text?.let { textView.text = it }
        attributedText?.let { textView.attributedText = it }
        textColor?.let { textView.textColor = it }
        textView.font = font
        textView.setTextAlignment(textAlignment)
        textView.setSelectable(isSelectable)
        textView.setEditable(false)
        textView.setScrollEnabled(isScrollEnabled)
        textView.backgroundColor = UIColor.clearColor
        textView.setDataDetectorTypes(dataDetectorTypes)

        insets?.let {
            textView.setTextContainerInset(it)
        }

        return textView
    }
}

inline fun textView(
    text: String? = null,
    builder: TextViewBuilder.() -> Unit = {}
): UITextView {
    val b = TextViewBuilder()
    if (text != null) b.text = text
    b.builder()
    return b.build()
}
