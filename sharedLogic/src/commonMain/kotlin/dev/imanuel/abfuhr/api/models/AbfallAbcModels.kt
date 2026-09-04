package dev.imanuel.abfuhr.api.models

import kotlinx.serialization.Serializable

@Serializable
data class AbfallAbcSymbol(
    val title: String,
    val image: String,
    val largeImage: String
)

@Serializable
data class AbfallAbcDisposalRoute(
    val title: String,
    val description: String,
    val street: String,
    val zipcode: String,
    val city: String,
    val openingHours: String,
    val fees: String,
    val link1: String? = null,
    val link2: String? = null,
    val link3: String? = null,
    val descriptionLink1: String? = null,
    val descriptionLink2: String? = null,
    val descriptionLink3: String? = null,
    val file1: String? = null,
    val file2: String? = null,
    val file3: String? = null,
    val descriptionFile1: String? = null,
    val descriptionFile2: String? = null,
    val descriptionFile3: String? = null,
    val symbol: AbfallAbcSymbol? = null
)

@Serializable
data class AbfallAbcDisposalRoutes(
    val id: Int,
    val language: String,
    val alternativeRoute: AbfallAbcDisposalRoute? = null,
    val collection: AbfallAbcDisposalRoute? = null,
    val dischargePoint: AbfallAbcDisposalRoute? = null
)

@Serializable
data class AbfallAbcWaste(
    val id: Int,
    val language: String,
    val title: String,
    val description: String,
    val tips: List<String>,
    val routes: List<AbfallAbcDisposalRoutes>,
    val symbol: AbfallAbcSymbol? = null
)

@Serializable
data class AbfallAbcWasteMapping(
    val wasteId: Int,
    val routesId: Int
)

@Serializable
data class AbfallAbcDump(
    val wastes: List<AbfallAbcWaste>,
    val disposalRoutes: List<AbfallAbcDisposalRoutes>,
    val mapping: List<AbfallAbcWasteMapping>
)
