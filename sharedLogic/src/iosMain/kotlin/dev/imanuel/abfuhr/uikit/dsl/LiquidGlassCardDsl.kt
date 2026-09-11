@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.*
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.QuartzCore.kCACornerCurveContinuous
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.sel_registerName
import platform.objc.OBJC_ASSOCIATION_RETAIN_NONATOMIC
import platform.objc.objc_setAssociatedObject

class CardTapTarget(
    private val action: () -> Unit
) : NSObject() {
    @ObjCAction
    fun handleTap(recognizer: UITapGestureRecognizer) {
        action()
    }
}

private val cardTapKey: COpaquePointer = nativeHeap.alloc<ByteVar>().ptr

@UIKitDsl
class LiquidGlassCardBuilder {
    val containerView: UIView = UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))

    var blurStyle: UIBlurEffectStyle = UIBlurEffectStyle.UIBlurEffectStyleSystemUltraThinMaterial
    var cornerRadius: Double = 16.0
    var borderWidth: Double = 1.0
    var borderColor: UIColor? = UIColor.whiteColor.colorWithAlphaComponent(0.25)
    var backgroundColor: UIColor? = null
    var tintColor: UIColor? = null

    // Shadow properties
    var shadowColor: UIColor? = UIColor.blackColor
    var shadowOpacity: Float = 0.12f
    var shadowRadius: Double = 12.0
    var shadowOffsetX: Double = 0.0
    var shadowOffsetY: Double = 6.0

    // Insets / Padding
    private var topPadding: Double = 16.0
    private var leftPadding: Double = 16.0
    private var bottomPadding: Double = 16.0
    private var rightPadding: Double = 16.0

    private val childViews = mutableListOf<UIView>()
    private var tapAction: (() -> Unit)? = null

    fun padding(all: Double) {
        topPadding = all
        leftPadding = all
        bottomPadding = all
        rightPadding = all
    }

    fun padding(horizontal: Double, vertical: Double) {
        topPadding = vertical
        bottomPadding = vertical
        leftPadding = horizontal
        rightPadding = horizontal
    }

    fun padding(top: Double, left: Double, bottom: Double, right: Double) {
        topPadding = top
        leftPadding = left
        bottomPadding = bottom
        rightPadding = right
    }

    fun add(view: UIView) {
        childViews.add(view)
    }

    fun label(
        text: String? = null,
        builder: LabelBuilder.() -> Unit = {}
    ): UILabel {
        val lbl = dev.imanuel.abfuhr.uikit.dsl.label(text, builder)
        childViews.add(lbl)
        return lbl
    }

    fun button(
        title: String? = null,
        buttonType: Long = UIButtonTypeSystem,
        builder: ButtonBuilder.() -> Unit = {}
    ): UIButton {
        val btn = dev.imanuel.abfuhr.uikit.dsl.button(title, buttonType, builder)
        childViews.add(btn)
        return btn
    }

    fun iconButton(
        systemName: String? = null,
        builder: IconButtonBuilder.() -> Unit = {}
    ): UIButton {
        val btn = dev.imanuel.abfuhr.uikit.dsl.iconButton(systemName, builder)
        childViews.add(btn)
        return btn
    }

    fun textField(
        placeholder: String? = null,
        text: String? = null,
        builder: TextFieldBuilder.() -> Unit = {}
    ): UITextField {
        val tf = dev.imanuel.abfuhr.uikit.dsl.singleLineTextField(placeholder, text, builder)
        childViews.add(tf)
        return tf
    }

    fun searchField(
        placeholder: String? = "Search",
        text: String? = null,
        builder: SearchFieldBuilder.() -> Unit = {}
    ): UISearchTextField {
        val sf = dev.imanuel.abfuhr.uikit.dsl.searchField(placeholder, text, builder)
        childViews.add(sf)
        return sf
    }

    fun textView(
        text: String? = null,
        placeholder: String? = null,
        builder: TextViewBuilder.() -> Unit = {}
    ): UITextView {
        val tv = dev.imanuel.abfuhr.uikit.dsl.textView(text, placeholder, builder)
        childViews.add(tv)
        return tv
    }

    fun activityIndicator(
        style: UIActivityIndicatorViewStyle = UIActivityIndicatorViewStyleMedium,
        color: UIColor? = null,
        isAnimating: Boolean = true,
        builder: ActivityIndicatorBuilder.() -> Unit = {}
    ): UIActivityIndicatorView {
        val cpi = dev.imanuel.abfuhr.uikit.dsl.activityIndicator(style, color, isAnimating, builder)
        childViews.add(cpi)
        return cpi
    }

    fun column(
        spacing: Double = 8.0,
        builder: ColumnBuilder.() -> Unit
    ): UIView {
        val col = dev.imanuel.abfuhr.uikit.dsl.column(spacing = spacing, builder = builder)
        childViews.add(col)
        return col
    }

    fun row(
        spacing: Double = 8.0,
        builder: RowBuilder.() -> Unit
    ): UIView {
        val r = dev.imanuel.abfuhr.uikit.dsl.row(spacing = spacing, builder = builder)
        childViews.add(r)
        return r
    }

    fun staggeredGrid(
        columns: Int = 2,
        spacing: Double = 8.0,
        builder: StaggeredGridBuilder.() -> Unit
    ): UIView {
        val grid = dev.imanuel.abfuhr.uikit.dsl.staggeredGrid(columns = columns, spacing = spacing, builder = builder)
        childViews.add(grid)
        return grid
    }

    fun onClick(action: () -> Unit) {
        this.tapAction = action
    }

    fun build(): UIView {
        containerView.setBackgroundColor(backgroundColor ?: UIColor.clearColor)
        tintColor?.let { containerView.setTintColor(it) }

        // Blur effect view
        val blurEffect = UIBlurEffect.effectWithStyle(blurStyle)
        val effectView = UIVisualEffectView(effect = blurEffect)
        effectView.setTranslatesAutoresizingMaskIntoConstraints(false)
        effectView.layer.cornerRadius = cornerRadius
        effectView.layer.cornerCurve = kCACornerCurveContinuous
        effectView.layer.masksToBounds = true
        effectView.layer.borderWidth = borderWidth
        borderColor?.let { effectView.layer.borderColor = it.CGColor }

        containerView.addSubview(effectView)

        // Shadow on container layer (outer)
        shadowColor?.let {
            containerView.layer.shadowColor = it.CGColor
            containerView.layer.shadowOpacity = shadowOpacity
            containerView.layer.shadowRadius = shadowRadius
            containerView.layer.shadowOffset = CGSizeMake(shadowOffsetX, shadowOffsetY)
            containerView.layer.masksToBounds = false
        }

        // Add child views to effectView's contentView
        val contentView = effectView.contentView
        for (child in childViews) {
            contentView.addSubview(child)
        }

        // Tap gesture recognizer if onClick provided
        tapAction?.let { action ->
            containerView.setUserInteractionEnabled(true)
            val target = CardTapTarget(action)
            val recognizer = UITapGestureRecognizer(target = target, action = sel_registerName("handleTap:"))
            containerView.addGestureRecognizer(recognizer)
            objc_setAssociatedObject(containerView, cardTapKey, target, OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }

        return containerView
    }
}

inline fun glassCard(
    blurStyle: UIBlurEffectStyle = UIBlurEffectStyle.UIBlurEffectStyleSystemUltraThinMaterial,
    cornerRadius: Double = 16.0,
    builder: LiquidGlassCardBuilder.() -> Unit = {}
): UIView {
    val b = LiquidGlassCardBuilder()
    b.blurStyle = blurStyle
    b.cornerRadius = cornerRadius
    b.builder()
    return b.build()
}
