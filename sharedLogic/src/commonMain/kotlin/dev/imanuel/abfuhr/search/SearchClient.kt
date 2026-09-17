package dev.imanuel.abfuhr.search

import dev.imanuel.abfuhr.api.client.AbfuhrClient
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.database.searchAbfallAbcByKeyword
import dev.imanuel.abfuhr.database.searchAddressByKeyword
import dev.imanuel.abfuhr.models.AbfallAbcDisposalRoute
import dev.imanuel.abfuhr.models.AbfallAbcDisposalRoutes
import dev.imanuel.abfuhr.models.AbfallAbcWaste
import dev.imanuel.abfuhr.models.AbfuhrLocation
import dev.imanuel.abfuhr.models.AbfuhrPickup
import dev.imanuel.abfuhr.models.Location
import dev.imanuel.abfuhr.sync.SyncClient
import org.koin.dsl.module
import kotlin.time.Instant

val searchModule = module {
    single { SearchClient(get(), get(), get()) }
}

class SearchClient(
    private val client: AbfuhrClient,
    private val database: AbfallDatabase,
    private val syncClient: SyncClient,
    private val isSyncCompletedAndSuccessful: Boolean = syncClient.isSuccess.value && !syncClient.isSyncing.value
) {
    suspend fun searchAbfallAbc(keyword: String, language: String = "de"): List<AbfallAbcWaste> {
        if (!isSyncCompletedAndSuccessful) {
            return client.searchAbfallAbc(keyword = keyword, language = language)
        }

        val wastes = database.searchAbfallAbcByKeyword(
            language = language,
            keyword = keyword,
        )

        return wastes.map { waste ->
            val tips = database.abfallAbcQueries.getWasteTipsByWasteId(waste.id, waste.language)
                .executeAsList()
            val routes =
                database.abfallAbcQueries.getDisposalRoutesByWasteId(waste.id, language)
                    .executeAsList()
                    .map { routes ->
                        val alternativeRoute = routes.alternativeRouteId?.let { id ->
                            database.abfallAbcQueries.getDisposalRouteById(id).executeAsOneOrNull()
                                ?.toModel()
                        }
                        val collection = routes.collectionId?.let { id ->
                            database.abfallAbcQueries.getDisposalRouteById(id).executeAsOneOrNull()
                                ?.toModel()
                        }
                        val dischargePoint = routes.dischargePointId?.let { id ->
                            database.abfallAbcQueries.getDisposalRouteById(id).executeAsOneOrNull()
                                ?.toModel()
                        }
                        AbfallAbcDisposalRoutes(
                            id = routes.id,
                            language = routes.language,
                            alternativeRoute = alternativeRoute,
                            collection = collection,
                            dischargePoint = dischargePoint,
                        )
                    }

            AbfallAbcWaste(
                id = waste.id,
                language = waste.language,
                title = waste.title,
                description = waste.description,
                tips = tips,
                routes = routes,
                symbol = null,
            )
        }
    }

    suspend fun searchAbfuhr(keyword: String): List<AbfuhrLocation> {
        if (!isSyncCompletedAndSuccessful) {
            return client.searchAbfuhr(keyword = keyword)
        }

        val locations = database.searchAddressByKeyword(keyword = keyword)

        return locations.map { location ->
            val pickups =
                database.abfuhrQueries.getPickupsByStreetId(location.streetId).executeAsList()
                    .map { pickup ->
                        AbfuhrPickup(
                            streetId = pickup.streetId,
                            date = Instant.fromEpochMilliseconds(pickup.date),
                            isPostponed = pickup.isPostponed == 1L,
                            type = pickup.type,
                        )
                    }
            AbfuhrLocation(
                street = location.street,
                locality = location.locality,
                district = location.district,
                pickups = pickups,
                streetId = location.streetId,
                localityId = location.localityId,
                districtId = location.districtId,
                streetLatitude = location.streetLatitude,
                streetLongitude = location.streetLongitude,
            )
        }
    }

    suspend fun searchAbfuhrByGeolocation(lat: Double, lon: Double): List<AbfuhrLocation> {
        if (!isSyncCompletedAndSuccessful) {
            // Searching by geolocation is not supported on the server for privacy reasons
            return emptyList()
        }

        val locations =
            database.abfuhrQueries.searchByGeolocation(lat = lat, lon = lon).executeAsList()

        return locations.map { location ->
            val pickups =
                database.abfuhrQueries.getPickupsByStreetId(location.streetId).executeAsList()
                    .map { pickup ->
                        AbfuhrPickup(
                            streetId = pickup.streetId,
                            date = Instant.fromEpochMilliseconds(pickup.date),
                            isPostponed = pickup.isPostponed == 1L,
                            type = pickup.type,
                        )
                    }
            AbfuhrLocation(
                street = location.street,
                locality = location.locality,
                district = location.district,
                pickups = pickups,
                streetId = location.streetId,
                localityId = location.localityId,
                districtId = location.districtId,
                streetLatitude = location.streetLatitude,
                streetLongitude = location.streetLongitude,
            )
        }
    }

    private fun dev.imanuel.abfuhr.database.AbfallAbcDisposalRoute.toModel(): AbfallAbcDisposalRoute {
        return AbfallAbcDisposalRoute(
            title = title,
            description = description,
            street = street,
            zipcode = zipcode,
            city = city,
            openingHours = openingHours,
            fees = fees,
            link1 = link1,
            link2 = link2,
            link3 = link3,
            descriptionLink1 = descriptionLink1,
            descriptionLink2 = descriptionLink2,
            descriptionLink3 = descriptionLink3,
            file1 = file1,
            file2 = file2,
            file3 = file3,
            descriptionFile1 = descriptionFile1,
            descriptionFile2 = descriptionFile2,
            descriptionFile3 = descriptionFile3,
            symbol = null,
        )
    }
}
