package dev.imanuel.abfuhr

import android.app.Application
import dev.imanuel.abfuhr.api.client.apiModule
import dev.imanuel.abfuhr.database.databaseModule
import dev.imanuel.abfuhr.search.searchModule
import dev.imanuel.abfuhr.sync.syncModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.dsl.module

private val appModule = module {
}

class AbfuhrApplication : Application(), KoinComponent {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@AbfuhrApplication)
            workManagerFactory()
            modules(appModule, apiModule, databaseModule, searchModule, syncModule)
        }
    }
}
