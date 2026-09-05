package dev.imanuel.abfuhr.database

import app.cash.sqldelight.db.SqlDriver
import dev.imanuel.abfuhr.database.AbfallDatabase

expect class DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): AbfallDatabase {
    val driver = driverFactory.createDriver()
    val database = AbfallDatabase(driver)

    return database
}