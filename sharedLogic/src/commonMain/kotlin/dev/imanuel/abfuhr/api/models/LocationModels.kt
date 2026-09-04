package dev.imanuel.abfuhr.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val type: String,
    val name: String,
    val latitude: String,
    val longitude: String,
    val description: String,
    val openingHours: String,
    val mail: String,
    val www: String,
    val tel: String,
    val fax: String
)
