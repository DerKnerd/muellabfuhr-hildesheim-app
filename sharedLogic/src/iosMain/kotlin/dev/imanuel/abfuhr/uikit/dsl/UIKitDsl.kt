@file:OptIn(ExperimentalForeignApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.*

@UIKitDsl
class LabelBuilder {
    val label: UILabel = UILabel()

    var text: String? = null
    var textColor: UIColor? = null
    var font: UIFont? = null
    var textAlignment: NSTextAlignment = NSTextAlignmentNatural
    var numberOfLines: Long = 1L
    var lineBreakMode: NSLineBreakMode = NSLineBreakByTruncatingTail
    var backgroundColor: UIColor? = null

    fun build(): UILabel {
        text?.let { label.setText(it) }
        textColor?.let { label.setTextColor(it) }
        font?.let { label.setFont(it) }
        label.setTextAlignment(textAlignment)
        label.setNumberOfLines(numberOfLines)
        label.setLineBreakMode(lineBreakMode)
        backgroundColor?.let { label.setBackgroundColor(it) }
        return label
    }
}

inline fun label(
    text: String? = null,
    builder: LabelBuilder.() -> Unit = {}
): UILabel {
    val b = LabelBuilder()
    if (text != null) b.text = text
    b.builder()
    return b.build()
}

fun UIView.pinToEdges(parent: UIView, insets: Double = 0.0) {
    setTranslatesAutoresizingMaskIntoConstraints(false)
    NSLayoutConstraint.activateConstraints(
        listOf(
            topAnchor.constraintEqualToAnchor(parent.topAnchor, constant = insets),
            leadingAnchor.constraintEqualToAnchor(parent.leadingAnchor, constant = insets),
            trailingAnchor.constraintEqualToAnchor(parent.trailingAnchor, constant = -insets),
            bottomAnchor.constraintEqualToAnchor(parent.bottomAnchor, constant = -insets)
        )
    )
}
