package dev.imanuel.abfuhr.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.koin.core.module.Module
import org.koin.dsl.module
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = AbfallDatabase.Schema,
            context = context,
            name = "abfall_database.db",
            factory = RequerySQLiteOpenHelperFactory()
        )
    }
}

actual val databaseModule: Module = module {
    single { createDatabase(DriverFactory(get())) }
}