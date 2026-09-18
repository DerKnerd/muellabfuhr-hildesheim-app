package dev.imanuel.abfuhr.search

import dev.imanuel.abfuhr.api.client.AbfuhrClient
import dev.imanuel.abfuhr.database.AbfallDatabase
import dev.imanuel.abfuhr.database.searchAbfallAbcByKeyword
import dev.imanuel.abfuhr.database.searchAddressByKeyword
import dev.imanuel.abfuhr.models.*
import dev.imanuel.abfuhr.sync.SyncClient
import org.koin.core.module.Module
import kotlin.time.Instant

expect val searchModule: Module

class SearchClient(
    private val client: AbfuhrClient,
    private val database: AbfallDatabase,
    private val isSyncCompletedAndSuccessful: Boolean
) {
    suspend fun searchAbfallAbc(keyword: String, language: String = "de"): List<AbfallAbcWaste> {
        if (!isSyncCompletedAndSuccessful) {
            return client.searchAbfallAbc(keyword = keyword, language = language)
        }

        val wastes = database.searchAbfallAbcByKeyword(
            language = language,
            keyword = keyword,
        )
        val routes = database.abfallAbcQueries.getAllDisposalRoutesByLanguage(language)
            .executeAsList()
        val route = database.abfallAbcQueries.getAllDisposalRouteByLanguage(language)
            .executeAsList().map { m ->
                Pair(
                    m.id,
                    AbfallAbcDisposalRoute(
                        title = m.title,
                        description = m.description,
                        street = m.street,
                        zipcode = m.zipcode,
                        city = m.city,
                        openingHours = m.openingHours,
                        fees = m.fees,
                        link1 = m.link1,
                        link2 = m.link2,
                        link3 = m.link3,
                        descriptionLink1 = m.descriptionLink1,
                        descriptionLink2 = m.descriptionLink2,
                        descriptionLink3 = m.descriptionLink3,
                        file1 = m.file1,
                        file2 = m.file2,
                        file3 = m.file3,
                        descriptionFile1 = m.descriptionFile1,
                        descriptionFile2 = m.descriptionFile2,
                        descriptionFile3 = m.descriptionFile3,
                        symbol = null,
                    )
                )
            }
        val mappings = database.abfallAbcQueries.getAllWasteMapping().executeAsList()

        return wastes.map { waste ->
            val tips = database.abfallAbcQueries.getWasteTipsByWasteId(waste.id, waste.language)
                .executeAsList()
            val routes = mappings.filter { f -> f.wasteId == waste.id }
                .mapNotNull { m -> routes.firstOrNull { r -> r.id == m.routesId } }
                .map { m ->
                    val alternativeRoute =
                        route.firstOrNull { r -> r.first == m.alternativeRouteId }?.second
                    val collection =
                        route.firstOrNull { r -> r.first == m.collectionId }?.second
                    val dischargePoint =
                        route.firstOrNull { r -> r.first == m.dischargePointId }?.second
                    AbfallAbcDisposalRoutes(
                        id = m.id,
                        language = m.language,
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

    fun searchAbfuhrByGeolocation(lat: Double, lon: Double): List<AbfuhrLocation> {
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
