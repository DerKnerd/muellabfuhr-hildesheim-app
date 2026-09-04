package dev.imanuel.abfuhr.api.client

import dev.imanuel.abfuhr.api.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val apiModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }
    }
    single { AbfuhrClient(get()) }
}

class AbfuhrClient(private val httpClient: HttpClient) {
    private val baseUrl = "https://abfuhr.imanuel.dev"

    /**
     * Performs a fuzzy search on waste encyclopedia entries by keyword matching across title, description, and tips in the selected language. Returns up to 5 best matching items.
     */
    suspend fun searchAbfallAbc(keyword: String, language: String = "de"): List<AbfallAbcWaste> {
        return httpClient.get("$baseUrl/api/abfall-abc") {
            parameter("keyword", keyword)
            parameter("language", language)
        }.body()
    }

    /**
     * Performs a fuzzy search for locations and their pickup schedules by matching the keyword against locality, street, and district names. Returns up to 5 matching locations with their scheduled pickup dates.
     */
    suspend fun searchAbfuhr(keyword: String): List<AbfuhrLocation> {
        return httpClient.get("$baseUrl/api/abfuhr") {
            parameter("keyword", keyword)
        }.body()
    }

    /**
     * Performs a search on drop-off facilities, container locations, and recycling centers by matching the keyword against location descriptions and/or filtering by location type.
     */
    suspend fun searchLocations(keyword: String? = null, type: String? = null): List<Location> {
        return httpClient.get("$baseUrl/api/location") {
            keyword?.let { parameter("keyword", it) }
            type?.let { parameter("type", it) }
        }.body()
    }

    /**
     * Exports all waste items, disposal routes, and their many-to-many relationship mappings. Responses are compressed with gzip using best compression.
     */
    suspend fun dumpAbfallAbc(): AbfallAbcDump {
        return httpClient.get("$baseUrl/dump/abfall-abc").body()
    }

    /**
     * Exports all collection locations and all scheduled waste pickups across all streets. Responses are compressed with gzip using best compression.
     */
    suspend fun dumpAbfuhr(): AbfuhrDump {
        return httpClient.get("$baseUrl/dump/abfuhr").body()
    }

    /**
     * Exports all waste drop-off locations, recycling centers, compost sites, and container points. Responses are compressed with gzip using best compression.
     */
    suspend fun dumpLocations(): List<Location> {
        return httpClient.get("$baseUrl/dump/location").body()
    }
}
