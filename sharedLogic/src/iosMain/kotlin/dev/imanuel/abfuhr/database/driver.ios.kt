package dev.imanuel.abfuhr.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.koin.core.module.Module
import org.koin.dsl.module

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(AbfallDatabase.Schema, "abfall_database.db")
    }
}

actual val databaseModule: Module = module {
    single { createDatabase(DriverFactory()) }
}