package dev.imanuel.abfuhr.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.imanuel.abfuhr.database.AbfallDatabase

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(AbfallDatabase.Schema, "abfall_database.db")
    }
}