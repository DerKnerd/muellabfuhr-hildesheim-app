package dev.imanuel.abfuhr.uikit.dsl

import platform.UIKit.*

@UIKitDsl
class LabelBuilder {
    val label = UILabel()

    var text: String? = null
    var textAlignment = NSTextAlignmentNatural
    var textColor: UIColor? = null
    var numberOfLines = 1L
    var lineBreakMode = NSLineBreakByTruncatingTail
    var font = UIFont.preferredFontForTextStyle(UIFontTextStyleBody)

    fun build(): UILabel {
        label.font = font
        text?.let { label.text = it }
        textColor?.let { label.textColor = it }
        label.textAlignment = textAlignment
        label.numberOfLines = numberOfLines
        label.lineBreakMode = lineBreakMode
        return label
    }
}

inline fun label(
    text: String? = null,
    builder: LabelBuilder.() -> Unit = {},
): UILabel {
    val b = LabelBuilder()
    if (text != null) b.text = text
    b.builder()
    return b.build()
}
