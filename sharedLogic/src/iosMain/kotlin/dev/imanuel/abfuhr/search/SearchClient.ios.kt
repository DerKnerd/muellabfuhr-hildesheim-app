package dev.imanuel.abfuhr.search

import dev.imanuel.abfuhr.preferences.firstSyncHappened
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual val searchModule: Module
    get() = module {
        single {
            SearchClient(
                get(),
                get(),
                NSUserDefaults.standardUserDefaults.firstSyncHappened()
            )
        }
    }