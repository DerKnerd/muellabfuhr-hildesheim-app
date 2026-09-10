package dev.imanuel.abfuhr.preferences

import platform.Foundation.NSUserDefaults

/**
 * Storage keys for application preferences stored in iOS NSUserDefaults (the iOS equivalent of SharedPreferences).
 */
object PreferenceKeys {
    const val FIRST_SYNC = "firstSync"
}

/**
 * Top-level helper functions for convenient access across iOS code.
 */
fun NSUserDefaults.firstSyncHappened(): Boolean = boolForKey(PreferenceKeys.FIRST_SYNC)

fun NSUserDefaults.markFirstSync() {
    setBool(true, forKey = PreferenceKeys.FIRST_SYNC)
    synchronize()
}
