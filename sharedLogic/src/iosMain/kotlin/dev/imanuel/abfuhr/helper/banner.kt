package dev.imanuel.abfuhr.helper

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGAffineTransformIdentity
import platform.CoreGraphics.CGAffineTransformMakeTranslation
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.UIKit.*

fun notifyDone(success: Boolean) {
    val feedbackGenerator = UINotificationFeedbackGenerator()
    feedbackGenerator.prepare()
    val feedbackType = if (success) {
        UINotificationFeedbackType.UINotificationFeedbackTypeSuccess
    } else {
        UINotificationFeedbackType.UINotificationFeedbackTypeError
    }
    feedbackGenerator.notificationOccurred(feedbackType)

    if (success) {
        // System sound ID for the iOS Mail Sent sound is 1001
        AudioServicesPlaySystemSound(1001u)
    }
}

@OptIn(ExperimentalForeignApi::class)
fun UIViewController.showToast(
    message: String,
    isSuccess: Boolean = true,
    duration: Double = 3.0
) {
    // 1. Trigger iOS Haptic Feedback
    val feedbackGenerator = UINotificationFeedbackGenerator()
    feedbackGenerator.prepare()
    val feedbackType = if (isSuccess) {
        UINotificationFeedbackType.UINotificationFeedbackTypeSuccess
    } else {
        UINotificationFeedbackType.UINotificationFeedbackTypeError
    }
    feedbackGenerator.notificationOccurred(feedbackType)

    // 2. Build floating pill container
    val container = UIView().apply {
        translatesAutoresizingMaskIntoConstraints = false
        backgroundColor = if (isSuccess) {
            UIColor.systemGreenColor
        } else {
            UIColor.systemRedColor
        }
        layer.cornerRadius = 16.0
        layer.masksToBounds = true
        alpha = 0.0
    }

    // 3. Build icon
    val iconName = if (isSuccess) "checkmark.circle.fill" else "exclamationmark.circle.fill"
    val iconView = UIImageView(image = UIImage.systemImageNamed(iconName)).apply {
        translatesAutoresizingMaskIntoConstraints = false
        tintColor = UIColor.whiteColor
        contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
    }

    // 4. Build text label
    val label = UILabel().apply {
        translatesAutoresizingMaskIntoConstraints = false
        text = message
        textColor = UIColor.whiteColor
        font = UIFont.boldSystemFontOfSize(14.0)
        numberOfLines = 2
        lineBreakMode = NSLineBreakByTruncatingTail
    }

    // 5. Build horizontal stack
    val stack = UIStackView().apply {
        translatesAutoresizingMaskIntoConstraints = false
        axis = UILayoutConstraintAxisHorizontal
        spacing = 8.0
        alignment = UIStackViewAlignmentCenter
        distribution = UIStackViewDistributionFill
    }

    stack.addArrangedSubview(iconView)
    stack.addArrangedSubview(label)
    container.addSubview(stack)
    view.addSubview(container)

    // 6. Layout constraints (Floating pill near top safe area)
    NSLayoutConstraint.activateConstraints(
        listOf(
            container.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor, constant = 12.0),
            container.centerXAnchor.constraintEqualToAnchor(view.centerXAnchor),
            container.leadingAnchor.constraintGreaterThanOrEqualToAnchor(view.leadingAnchor, constant = 20.0),
            container.trailingAnchor.constraintLessThanOrEqualToAnchor(view.trailingAnchor, constant = -20.0),

            stack.topAnchor.constraintEqualToAnchor(container.topAnchor, constant = 10.0),
            stack.bottomAnchor.constraintEqualToAnchor(container.bottomAnchor, constant = -10.0),
            stack.leadingAnchor.constraintEqualToAnchor(container.leadingAnchor, constant = 16.0),
            stack.trailingAnchor.constraintEqualToAnchor(container.trailingAnchor, constant = -16.0),

            iconView.widthAnchor.constraintEqualToConstant(20.0),
            iconView.heightAnchor.constraintEqualToConstant(20.0)
        )
    )

    // 7. Slide / Fade In animation
    container.transform = CGAffineTransformMakeTranslation(0.0, -20.0)
    UIView.animateWithDuration(
        duration = 0.3,
        delay = 0.0,
        options = UIViewAnimationOptionCurveEaseOut,
        animations = {
            container.alpha = 1.0
            container.transform = CGAffineTransformIdentity.readValue()
        },
        completion = { _ ->
            // 8. Auto-dismiss after timeout
            UIView.animateWithDuration(
                duration = 0.3,
                delay = duration,
                options = UIViewAnimationOptionCurveEaseIn,
                animations = {
                    container.alpha = 0.0
                    container.transform = CGAffineTransformMakeTranslation(0.0, -20.0)
                },
                completion = { _ ->
                    container.removeFromSuperview()
                }
            )
        }
    )
}