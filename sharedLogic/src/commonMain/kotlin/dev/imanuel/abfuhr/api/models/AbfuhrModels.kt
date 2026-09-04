package dev.imanuel.abfuhr.api.models

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class AbfuhrPickup(
    val streetId: Int,
    val date: Instant,
    val isPostponed: Boolean,
    val type: String
)

@Serializable
data class AbfuhrLocation(
    val street: String,
    val locality: String,
    val district: String,
    val pickups: List<AbfuhrPickup>
)

@Serializable
data class AbfuhrDump(
    val locations: List<AbfuhrLocation>,
    val pickups: List<AbfuhrPickup>
)
