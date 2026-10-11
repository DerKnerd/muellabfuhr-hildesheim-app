package dev.imanuel.abfuhr.uikit.dsl

import platform.UIKit.*

@UIKitDsl
class DialogActionBuilder(
    var title: String,
    var style: UIAlertActionStyle = UIAlertActionStyleDefault
) {
    var isEnabled: Boolean = true
    private var actionHandler: ((UIAlertAction) -> Unit)? = null

    fun onTrigger(handler: (UIAlertAction) -> Unit) {
        this.actionHandler = handler
    }

    fun build(): UIAlertAction {
        val action = UIAlertAction.actionWithTitle(title, style) { alertAction ->
            if (alertAction != null) {
                actionHandler?.invoke(alertAction)
            }
        }
        action.setEnabled(isEnabled)
        return action
    }
}

@UIKitDsl
class DialogBuilder(
    var title: String? = null,
    var message: String? = null,
    var style: UIAlertControllerStyle = UIAlertControllerStyleAlert
) {
    private val actions = mutableListOf<UIAlertAction>()
    private val textFieldConfigs = mutableListOf<(UITextField) -> Unit>()
    var preferredActionIndex: Int? = null

    fun action(
        title: String,
        style: UIAlertActionStyle = UIAlertActionStyleDefault,
        handler: ((UIAlertAction) -> Unit)? = null
    ) {
        val b = DialogActionBuilder(title, style)
        if (handler != null) b.onTrigger(handler)
        actions.add(b.build())
    }

    fun okAction(
        title: String = "OK",
        handler: ((UIAlertAction) -> Unit)? = null
    ) {
        action(title, UIAlertActionStyleDefault, handler)
    }

    fun build(): UIAlertController {
        val controller = UIAlertController.alertControllerWithTitle(
            title = title,
            message = message,
            preferredStyle = style
        )

        for (action in actions) {
            controller.addAction(action)
        }

        if (style == UIAlertControllerStyleAlert) {
            for (tfConfig in textFieldConfigs) {
                controller.addTextFieldWithConfigurationHandler { tf ->
                    if (tf != null) {
                        tfConfig(tf)
                    }
                }
            }
        }

        preferredActionIndex?.let { index ->
            if (index in actions.indices) {
                controller.setPreferredAction(actions[index])
            }
        }

        return controller
    }

    fun show(
        inViewController: UIViewController,
        animated: Boolean = true,
        completion: (() -> Unit)? = null
    ): UIAlertController {
        val controller = build()
        inViewController.presentViewController(controller, animated = animated, completion = completion)
        return controller
    }
}

inline fun UIViewController.showAlert(
    title: String? = null,
    message: String? = null,
    builder: DialogBuilder.() -> Unit = {}
): UIAlertController {
    val b = DialogBuilder(title, message, UIAlertControllerStyleAlert)
    b.builder()
    return b.show(this)
}
