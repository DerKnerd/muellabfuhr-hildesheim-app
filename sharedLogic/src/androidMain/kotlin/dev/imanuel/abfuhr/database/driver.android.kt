package dev.imanuel.abfuhr.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.imanuel.abfuhr.database.AbfallDatabase

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(AbfallDatabase.Schema, context, "abfall_database.db")
    }
}
