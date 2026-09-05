package dev.imanuel.abfuhr.models

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class AbfuhrPickup(
    val streetId: Long,
    val date: Instant,
    val isPostponed: Boolean,
    val type: String
)

@Serializable
data class AbfuhrLocation(
    val street: String,
    val streetId: Long,
    val locality: String,
    val localityId: Long,
    val district: String,
    val districtId: Long,
    val streetLatitude: Double,
    val streetLongitude: Double,
    val districtLatitude: Double,
    val districtLongitude: Double,
    val localityLatitude: Double,
    val localityLongitude: Double,
    val pickups: List<AbfuhrPickup>
)

@Serializable
data class AbfuhrDump(
    val locations: List<AbfuhrLocation>,
    val pickups: List<AbfuhrPickup>
)
