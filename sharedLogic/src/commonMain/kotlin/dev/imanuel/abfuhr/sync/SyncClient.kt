package dev.imanuel.abfuhr.sync

import dev.imanuel.abfuhr.AbfallAbcDisposalRoute
import dev.imanuel.abfuhr.AbfallAbcDisposalRoutes
import dev.imanuel.abfuhr.AbfallAbcWaste
import dev.imanuel.abfuhr.AbfallAbcWasteMapping
import dev.imanuel.abfuhr.AbfallAbcWasteTips
import dev.imanuel.abfuhr.AbfuhrLocation
import dev.imanuel.abfuhr.AbfuhrPickup
import dev.imanuel.abfuhr.Location
import dev.imanuel.abfuhr.api.client.AbfuhrClient
import dev.imanuel.abfuhr.database.AbfallDatabase
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
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()
    val didSucceed: StateFlow<Boolean> = isSuccess
    val isSuccessful: StateFlow<Boolean> = isSuccess

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

                for (disposalRoutes in abfallAbcDump.disposalRoutes) {
                    val alternativeRouteId = disposalRoutes.alternativeRoute?.let { route ->
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

                    val collectionId = disposalRoutes.collection?.let { route ->
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

                    val dischargePointId = disposalRoutes.dischargePoint?.let { route ->
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
                            id = disposalRoutes.id,
                            language = disposalRoutes.language,
                            alternativeRouteId = alternativeRouteId,
                            collectionId = collectionId,
                            dischargePointId = dischargePointId,
                        )
                    )
                }

                for (waste in abfallAbcDump.wastes) {
                    database.abfallAbcQueries.insertWaste(
                        AbfallAbcWaste(
                            id = waste.id,
                            language = waste.language,
                            title = waste.title,
                            description = waste.description,
                        )
                    )

                    for (tip in waste.tips) {
                        database.abfallAbcQueries.insertWasteTip(
                            AbfallAbcWasteTips(
                                wasteId = waste.id,
                                tip = tip,
                            )
                        )
                    }
                }

                for (mapping in abfallAbcDump.mapping) {
                    database.abfallAbcQueries.insertWasteMapping(
                        AbfallAbcWasteMapping(
                            wasteId = mapping.wasteId,
                            routesId = mapping.routesId,
                        )
                    )
                }

                for ((index, location) in abfuhrDump.locations.withIndex()) {
                    database.abfuhrQueries.insertLocation(
                        AbfuhrLocation(
                            street = location.street,
                            locality = location.locality,
                            district = location.district,
                            streetId = location.streetId,
                            localityId = location.localityId,
                            districtId = location.districtId,
                            streetLatitude = location.streetLatitude,
                            streetLongitude = location.streetLongitude,
                            districtLatitude = location.districtLatitude,
                            districtLongitude = location.districtLongitude,
                            localityLatitude = location.localityLatitude,
                            localityLongitude = location.localityLongitude,
                        )
                    )
                    for (pickup in location.pickups) {
                        database.abfuhrQueries.insertPickup(
                            AbfuhrPickup(
                                streetId = location.streetId,
                                date = pickup.date.toEpochMilliseconds(),
                                isPostponed = if (pickup.isPostponed) 1L else 0L,
                                type = pickup.type,
                            )
                        )
                    }
                }

                for (location in locations) {
                    database.locationQueries.insertLocation(
                        Location(
                            type = location.type,
                            name = location.name,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            description = location.description,
                            openingHours = location.openingHours,
                            mail = location.mail,
                            www = location.www,
                            tel = location.tel,
                            fax = location.fax,
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
