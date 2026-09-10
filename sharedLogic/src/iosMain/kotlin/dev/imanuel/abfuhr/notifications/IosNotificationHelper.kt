@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package dev.imanuel.abfuhr.notifications

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.UserNotifications.*

/**
 * Handle to a scheduled notification that provides cancellation and metadata inspection.
 */
data class CancellableNotification(
    val id: String,
    val tags: Set<String> = emptySet(),
    val targetTimeMillis: Long? = null,
    val icon: String? = null,
    private val onCancel: (String) -> Unit = { IosNotificationManager.cancelNotifications(listOf(it)) }
) {
    /**
     * Cancels this specific notification from pending and delivered notification queues.
     */
    fun cancel() {
        onCancel(id)
    }
}

/**
 * Data class representing scheduled notification request details.
 */
data class ScheduledNotification(
    val id: String,
    val title: String,
    val body: String,
    val subtitle: String? = null,
    val tags: Set<String> = emptySet(),
    val targetTimeMillis: Long? = null,
    val icon: String? = null,
    val userInfo: Map<String, Any> = emptyMap()
)

/**
 * Default implementation backed by UNUserNotificationCenter.currentNotificationCenter().
 */
class NotificationCenterClient {
    private val center: UNUserNotificationCenter?
        get() = try {
            UNUserNotificationCenter.currentNotificationCenter()
        } catch (e: Throwable) {
            null
        }

    fun requestAuthorization(options: ULong, completion: (Boolean, NSError?) -> Unit) {
        val c = center
        if (c != null) {
            c.requestAuthorizationWithOptions(options) { granted, error ->
                completion(granted, error)
            }
        } else {
            completion(true, null)
        }
    }

    fun addNotificationRequest(request: UNNotificationRequest, completion: ((NSError?) -> Unit)?) {
        val c = center
        if (c != null) {
            c.addNotificationRequest(request) { error ->
                completion?.invoke(error)
            }
        } else {
            completion?.invoke(null)
        }
    }

    fun removePendingNotificationRequestsWithIdentifiers(identifiers: List<String>) {
        center?.removePendingNotificationRequestsWithIdentifiers(identifiers)
    }

    fun removeDeliveredNotificationsWithIdentifiers(identifiers: List<String>) {
        center?.removeDeliveredNotificationsWithIdentifiers(identifiers)
    }

    fun getPendingNotificationRequests(completion: (List<UNNotificationRequest>) -> Unit) {
        if (center != null) {
            center!!.getPendingNotificationRequestsWithCompletionHandler { requests ->
                completion(requests?.filterIsInstance<UNNotificationRequest>() ?: emptyList())
            }
        } else {
            completion(emptyList())
        }
    }

}

/**
 * Notification manager and scheduling engine for iOS using UNUserNotificationCenter.
 * Provides scheduling at specific times, cancellation, and tag-based management.
 */
object IosNotificationManager {

    const val TAGS_USER_INFO_KEY = "__notification_tags__"
    const val TARGET_TIMESTAMP_KEY = "__notification_target_timestamp__"
    const val CUSTOM_ICON_KEY = "__notification_custom_icon__"

    var client: NotificationCenterClient = NotificationCenterClient()

    /**
     * Request notification authorization from the user (Alert, Sound, Badge).
     */
    fun requestAuthorization(
        options: ULong = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
        completion: ((Boolean, NSError?) -> Unit)? = null
    ) {
        client.requestAuthorization(options) { granted, error ->
            completion?.invoke(granted, error)
        }
    }

    /**
     * Enqueues a notification for a specific epoch timestamp in milliseconds.
     *
     * @param epochMillis Target epoch timestamp in milliseconds when the notification should fire.
     * @param title Title of the notification.
     * @param body Body text of the notification.
     * @param tags Collection of string tags associated with this notification for filtering/cancellation.
     * @param id Unique identifier for the notification request. If omitted, a unique ID is generated.
     * @param subtitle Optional subtitle text.
     * @param customIcon Optional custom icon name, bundle asset name, or file path.
     * @param customIconUrl Optional file URL to custom icon/image attachment.
     * @param attachments Optional list of UNNotificationAttachment objects.
     * @param badge Optional badge count to set on the app icon.
     * @param sound Sound to play (defaults to default notification sound).
     * @param extraUserInfo Extra metadata dictionary to attach to the notification.
     * @param completion Completion handler invoked with success status and optional error.
     * @return A CancellableNotification handle with cancellation support.
     */
    fun enqueueNotification(
        epochMillis: Long,
        title: String,
        body: String,
        tags: Set<String> = emptySet(),
        id: String? = null,
        subtitle: String? = null,
        customIcon: String? = null,
        customIconUrl: NSURL? = null,
        attachments: List<UNNotificationAttachment> = emptyList(),
        badge: Long? = null,
        sound: UNNotificationSound? = UNNotificationSound.defaultSound(),
        extraUserInfo: Map<String, Any> = emptyMap(),
        completion: ((Boolean, NSError?) -> Unit)? = null
    ): CancellableNotification {
        val targetDate = NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
        val calendar = NSCalendar.currentCalendar
        val unitFlags = NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
                NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond
        val dateComponents = calendar.components(unitFlags, fromDate = targetDate)

        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents = dateComponents,
            repeats = false
        )

        val finalId = id ?: generateNotificationId(tags)
        val content = buildNotificationContent(
            title = title,
            body = body,
            subtitle = subtitle,
            tags = tags,
            customIcon = customIcon,
            customIconUrl = customIconUrl,
            attachments = attachments,
            notificationId = finalId,
            badge = badge,
            sound = sound,
            targetTimestampMillis = epochMillis,
            extraUserInfo = extraUserInfo
        )

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = finalId,
            content = content,
            trigger = trigger
        )

        client.addNotificationRequest(request) { error ->
            completion?.invoke(error == null, error)
        }

        return CancellableNotification(
            id = finalId,
            tags = tags,
            targetTimeMillis = epochMillis,
            icon = customIcon
        )
    }

    /**
     * Cancels multiple notifications by their identifiers.
     */
    fun cancelNotifications(ids: List<String>) {
        if (ids.isEmpty()) return
        client.removePendingNotificationRequestsWithIdentifiers(ids)
        client.removeDeliveredNotificationsWithIdentifiers(ids)
    }

    /**
     * Cancels all pending and delivered notifications that match any or all of the specified tags.
     *
     * @param tags Set of tags to match.
     * @param completion Optional callback invoked with the list of canceled notification IDs.
     */
    fun cancelNotificationsByTags(
        tags: Set<String>,
        completion: ((List<String>) -> Unit)? = null
    ) {
        if (tags.isEmpty()) {
            completion?.invoke(emptyList())
            return
        }

        client.getPendingNotificationRequests { pendingRequests ->
            val matchedIds = mutableListOf<String>()

            for (req in pendingRequests) {
                val reqTags = extractTags(req.content)
                val isMatch = tags.any { it in reqTags }
                if (isMatch) {
                    matchedIds.add(req.identifier)
                }
            }

            if (matchedIds.isNotEmpty()) {
                client.removePendingNotificationRequestsWithIdentifiers(matchedIds)
                client.removeDeliveredNotificationsWithIdentifiers(matchedIds)
            }

            completion?.invoke(matchedIds)
        }
    }

    /**
     * Builds UNMutableNotificationContent with title, body, sound, badge, tags, custom icon, and userInfo.
     */
    fun buildNotificationContent(
        title: String,
        body: String,
        subtitle: String? = null,
        tags: Set<String> = emptySet(),
        customIcon: String? = null,
        customIconUrl: NSURL? = null,
        attachments: List<UNNotificationAttachment> = emptyList(),
        notificationId: String = "notif_attachment",
        badge: Long? = null,
        sound: UNNotificationSound? = UNNotificationSound.defaultSound(),
        targetTimestampMillis: Long? = null,
        extraUserInfo: Map<String, Any> = emptyMap()
    ): UNMutableNotificationContent {
        val content = UNMutableNotificationContent()
        content.setTitle(title)
        content.setBody(body)
        subtitle?.let { content.setSubtitle(it) }
        sound?.let { content.setSound(it) }
        badge?.let { content.setBadge(NSNumber(longLong = it)) }

        val userInfoMap = mutableMapOf<Any?, Any>()
        extraUserInfo.forEach { (k, v) ->
            userInfoMap[k] = v
        }
        if (tags.isNotEmpty()) {
            val tagsString = tags.joinToString(separator = ",")
            userInfoMap[TAGS_USER_INFO_KEY] = tagsString
        }
        targetTimestampMillis?.let {
            userInfoMap[TARGET_TIMESTAMP_KEY] = NSNumber(longLong = it)
        }
        customIcon?.let {
            userInfoMap[CUSTOM_ICON_KEY] = it
        }
        content.setUserInfo(userInfoMap)

        // Resolve attachments
        val resolvedAttachments = mutableListOf<UNNotificationAttachment>()
        resolvedAttachments.addAll(attachments)

        // Try creating attachment from customIconUrl or customIcon file path if provided and file exists
        val effectiveUrl: NSURL? = customIconUrl ?: when {
            customIcon != null && (customIcon.startsWith("/") || customIcon.startsWith("file://")) -> {
                NSURL(fileURLWithPath = customIcon)
            }

            customIcon != null -> {
                NSBundle.mainBundle.URLForResource(customIcon, withExtension = null)
                    ?: NSBundle.mainBundle.URLForResource(customIcon, withExtension = "png")
            }

            else -> null
        }

        if (effectiveUrl != null) {
            val filePath = effectiveUrl.path
            if (filePath != null && NSFileManager.defaultManager.fileExistsAtPath(filePath)) {
                try {
                    memScoped {
                        val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                        val att = UNNotificationAttachment.attachmentWithIdentifier(
                            identifier = "${notificationId}_icon",
                            URL = effectiveUrl,
                            options = null,
                            error = errorPtr.ptr
                        )
                        if (att != null && errorPtr.value == null) {
                            resolvedAttachments.add(att)
                        }
                    }
                } catch (e: Throwable) {
                    // Ignore attachment error in environments where attachments cannot be loaded
                }
            }
        }

        if (resolvedAttachments.isNotEmpty()) {
            content.setAttachments(resolvedAttachments)
        }

        return content
    }

    /**
     * Extracts tags from UNNotificationContent userInfo dictionary.
     */
    fun extractTags(content: UNNotificationContent): Set<String> {
        val userInfo = content.userInfo
        val rawTags = userInfo[TAGS_USER_INFO_KEY]?.toString() ?: return emptySet()
        return rawTags.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    fun generateNotificationId(tags: Set<String>): String {
        val timestamp = platform.posix.time(null)
        val rand = (1..100000).random()
        val tagPrefix = if (tags.isNotEmpty()) "${tags.first()}_" else ""
        return "notif_${tagPrefix}${timestamp}_${rand}"
    }
}
