package dev.imanuel.abfuhr.database

import kotlin.time.Clock

fun fts5PrefixQuery(value: String): String =
    value
        .trim()
        .split(Regex("\\s+"))
        .filter(String::isNotEmpty)
        .joinToString(" ") { token ->
            "\"${token.replace("\"", "\"\"")}\"*"
        }

fun AbfallDatabase.searchAddressByKeyword(keyword: String): List<GetAllLocationsWithNextPickup> {
    return if (keyword.isEmpty()) {
        abfuhrQueries.getAllLocationsWithNextPickup(Clock.System.now().toEpochMilliseconds()).executeAsList()
    } else {
        abfuhrQueries.searchLocationsWithNextPickup(
            Clock.System.now().toEpochMilliseconds(),
            fts5PrefixQuery(keyword)
        ) { streetId,
            street,
            locality,
            localityId,
            district,
            districtId,
            streetLatitude,
            streetLongitude,
            hasReminder,
            pickupDate,
            pickupIsPostponed,
            pickupType ->
            GetAllLocationsWithNextPickup(
                streetId = streetId,
                street = street,
                locality = locality,
                localityId = localityId,
                district = district,
                districtId = districtId,
                streetLatitude = streetLatitude,
                streetLongitude = streetLongitude,
                hasReminder = hasReminder,
                pickupDate = pickupDate,
                pickupIsPostponed = pickupIsPostponed,
                pickupType = pickupType
            )
        }
            .executeAsList()
    }
}

fun AbfallDatabase.searchAbfallAbcByKeyword(
    language: String,
    keyword: String
): List<AbfallAbcWaste> {
    return if (keyword.isEmpty()) {
        abfallAbcQueries.getAllWasteByLanguage(language).executeAsList()
    } else if (keyword.length < 3) {
        abfallAbcQueries.searchAbfallAbcByShortKeyword(language, keyword).executeAsList()
    } else {
        abfallAbcQueries.searchAbfallAbcByKeyword(language, fts5PrefixQuery(keyword)).executeAsList()
    }
}
