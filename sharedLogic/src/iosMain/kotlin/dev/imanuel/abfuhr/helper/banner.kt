package dev.imanuel.abfuhr.helper

import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

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
