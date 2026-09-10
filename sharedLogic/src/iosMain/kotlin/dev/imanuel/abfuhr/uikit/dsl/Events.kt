@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package dev.imanuel.abfuhr.uikit.dsl

import kotlinx.cinterop.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.sel_registerName
import platform.objc.OBJC_ASSOCIATION_RETAIN_NONATOMIC
import platform.objc.objc_getAssociatedObject
import platform.objc.objc_setAssociatedObject

class ControlActionTarget(
    private val action: (UIControl) -> Unit
) : NSObject() {
    @ObjCAction
    fun actionTriggered(sender: UIControl) {
        action(sender)
    }
}

private val targetHolderKey: COpaquePointer = nativeHeap.alloc<ByteVar>().ptr

fun UIControl.onEvent(event: UIControlEvents, action: (UIControl) -> Unit) {
    val target = ControlActionTarget(action)
    val selector = sel_registerName("actionTriggered:")
    this.addTarget(target, selector, event)

    @Suppress("UNCHECKED_CAST")
    val list = (objc_getAssociatedObject(this, targetHolderKey) as? MutableList<ControlActionTarget>)
        ?: mutableListOf<ControlActionTarget>().also {
            objc_setAssociatedObject(this, targetHolderKey, it, OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        }
    list.add(target)
}

fun UIControl.onClick(action: () -> Unit) {
    onEvent(UIControlEventTouchUpInside) { action() }
}

fun UIControl.onValueChanged(action: () -> Unit) {
    onEvent(UIControlEventValueChanged) { action() }
}

fun UIControl.onPrimaryAction(action: () -> Unit) {
    onEvent(UIControlEventPrimaryActionTriggered) { action() }
}
