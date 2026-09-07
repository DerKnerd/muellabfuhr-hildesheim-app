package dev.imanuel.abfuhr.sync

import dev.imanuel.abfuhr.api.client.AbfuhrClient
import dev.imanuel.abfuhr.database.AbfallAbcDisposalRoute
import dev.imanuel.abfuhr.database.AbfallAbcDisposalRoutes
import dev.imanuel.abfuhr.database.AbfallAbcWaste
import dev.imanuel.abfuhr.database.AbfallAbcWasteMapping
import dev.imanuel.abfuhr.database.AbfallAbcWasteTips
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.database.AbfuhrLocation
import dev.imanuel.abfuhr.database.AbfuhrPickup
import dev.imanuel.abfuhr.database.Location
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.dsl.module

val syncModule = module {
    single { SyncClient(get(), get()) }
}

class SyncClient(
    private val client: AbfuhrClient,
    private val database: AbfallDatabase,
) {
    private val _isSyncing = MutableStateFlow(true)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    suspend fun sync() {
        _isSyncing.value = true
        _isSuccess.value = false
        try {
            val abfallAbcDump = client.dumpAbfallAbc()
            val abfuhrDump = client.dumpAbfuhr()
            val locations = client.dumpLocations()

            database.transaction {
                database.abfallAbcQueries.clearWasteMapping()
                database.abfallAbcQueries.clearWasteTips()
                database.abfallAbcQueries.clearWaste()
                database.abfallAbcQueries.clearDisposalRoutes()
                database.abfallAbcQueries.clearDisposalRoute()

                database.abfuhrQueries.clearPickups()
                database.abfuhrQueries.clearLocations()

                database.locationQueries.clearLocations()

                for ((id, language, alternativeRoute, collection, dischargePoint) in abfallAbcDump.disposalRoutes) {
                    val alternativeRouteId = alternativeRoute?.let { route ->
                        database.abfallAbcQueries.insertDisposalRoute(
                            AbfallAbcDisposalRoute(
                                id = 0L,
                                title = route.title,
                                description = route.description,
                                street = route.street,
                                zipcode = route.zipcode,
                                city = route.city,
                                openingHours = route.openingHours,
                                fees = route.fees,
                                link1 = route.link1,
                                link2 = route.link2,
                                link3 = route.link3,
                                descriptionLink1 = route.descriptionLink1,
                                descriptionLink2 = route.descriptionLink2,
                                descriptionLink3 = route.descriptionLink3,
                                file1 = route.file1,
                                file2 = route.file2,
                                file3 = route.file3,
                                descriptionFile1 = route.descriptionFile1,
                                descriptionFile2 = route.descriptionFile2,
                                descriptionFile3 = route.descriptionFile3,
                            )
                        )
                        database.abfallAbcQueries.lastInsertRowId().executeAsOne()
                    }

                    val collectionId = collection?.let { route ->
                        database.abfallAbcQueries.insertDisposalRoute(
                            AbfallAbcDisposalRoute(
                                id = 0L,
                                title = route.title,
                                description = route.description,
                                street = route.street,
                                zipcode = route.zipcode,
                                city = route.city,
                                openingHours = route.openingHours,
                                fees = route.fees,
                                link1 = route.link1,
                                link2 = route.link2,
                                link3 = route.link3,
                                descriptionLink1 = route.descriptionLink1,
                                descriptionLink2 = route.descriptionLink2,
                                descriptionLink3 = route.descriptionLink3,
                                file1 = route.file1,
                                file2 = route.file2,
                                file3 = route.file3,
                                descriptionFile1 = route.descriptionFile1,
                                descriptionFile2 = route.descriptionFile2,
                                descriptionFile3 = route.descriptionFile3,
                            )
                        )
                        database.abfallAbcQueries.lastInsertRowId().executeAsOne()
                    }

                    val dischargePointId = dischargePoint?.let { route ->
                        database.abfallAbcQueries.insertDisposalRoute(
                            AbfallAbcDisposalRoute(
                                id = 0L,
                                title = route.title,
                                description = route.description,
                                street = route.street,
                                zipcode = route.zipcode,
                                city = route.city,
                                openingHours = route.openingHours,
                                fees = route.fees,
                                link1 = route.link1,
                                link2 = route.link2,
                                link3 = route.link3,
                                descriptionLink1 = route.descriptionLink1,
                                descriptionLink2 = route.descriptionLink2,
                                descriptionLink3 = route.descriptionLink3,
                                file1 = route.file1,
                                file2 = route.file2,
                                file3 = route.file3,
                                descriptionFile1 = route.descriptionFile1,
                                descriptionFile2 = route.descriptionFile2,
                                descriptionFile3 = route.descriptionFile3,
                            )
                        )
                        database.abfallAbcQueries.lastInsertRowId().executeAsOne()
                    }

                    database.abfallAbcQueries.insertDisposalRoutes(
                        AbfallAbcDisposalRoutes(
                            id = id,
                            language = language,
                            alternativeRouteId = alternativeRouteId,
                            collectionId = collectionId,
                            dischargePointId = dischargePointId,
                        )
                    )
                }
                for ((id, language, title, description, tips) in abfallAbcDump.wastes) {
                    database.abfallAbcQueries.insertWaste(
                        AbfallAbcWaste(
                            id = id,
                            language = language,
                            title = title,
                            description = description,
                        )
                    )

                    for (tip in tips) {
                        database.abfallAbcQueries.insertWasteTip(
                            AbfallAbcWasteTips(
                                wasteId = id,
                                tip = tip,
                                language = language
                            )
                        )
                    }
                }
                for ((wasteId, routesId) in abfallAbcDump.mapping) {
                    database.abfallAbcQueries.insertWasteMapping(
                        AbfallAbcWasteMapping(
                            wasteId = wasteId,
                            routesId = routesId,
                        )
                    )
                }

                for ((street, streetId, locality, localityId, district, districtId, streetLatitude, streetLongitude) in abfuhrDump.locations) {
                    val locationByStreetId =
                        database.abfuhrQueries.getLocationByStreetId(streetId).executeAsOneOrNull()
                    database.abfuhrQueries.insertLocation(
                        AbfuhrLocation(
                            street = street,
                            locality = locality,
                            district = district,
                            streetId = streetId,
                            localityId = localityId,
                            districtId = districtId,
                            streetLatitude = streetLatitude,
                            streetLongitude = streetLongitude,
                            hasReminder = locationByStreetId?.hasReminder ?: 0L
                        )
                    )
                }
                for ((streetId, date, isPostponed, type) in abfuhrDump.pickups) {
                    database.abfuhrQueries.insertPickup(
                        AbfuhrPickup(
                            streetId = streetId,
                            date = date.toEpochMilliseconds(),
                            isPostponed = if (isPostponed) 1 else 0,
                            type = type
                        )
                    )
                }

                for ((type, name, latitude, longitude, description, openingHours, mail, www, tel, fax) in locations) {
                    database.locationQueries.insertLocation(
                        Location(
                            id = 0,
                            type = type,
                            name = name,
                            latitude = latitude,
                            longitude = longitude,
                            description = description,
                            openingHours = openingHours,
                            mail = mail,
                            www = www,
                            tel = tel,
                            fax = fax,
                        )
                    )
                }
            }
            _isSuccess.value = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            _isSuccess.value = false
        } finally {
            _isSyncing.value = false
        }
    }
}
