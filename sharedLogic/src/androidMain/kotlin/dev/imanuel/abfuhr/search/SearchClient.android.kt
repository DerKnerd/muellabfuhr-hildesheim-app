package dev.imanuel.abfuhr.search

import android.content.Context
import android.content.SharedPreferences
import org.koin.core.module.Module
import org.koin.dsl.module

fun Context.getSharedPrefs(): SharedPreferences =
    getSharedPreferences("muellabfuhr", Context.MODE_PRIVATE)

fun Context.firstSyncHappened() = getSharedPrefs().getBoolean("firstSync", false)

actual val searchModule: Module
    get() = module {
        single { SearchClient(get(), get(), get<Context>().firstSyncHappened()) }
    }