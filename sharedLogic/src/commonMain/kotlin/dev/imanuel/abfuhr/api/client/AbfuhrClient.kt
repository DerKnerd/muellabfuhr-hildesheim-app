package dev.imanuel.abfuhr.api.client

import dev.imanuel.abfuhr.models.AbfallAbcDump
import dev.imanuel.abfuhr.models.AbfallAbcWaste
import dev.imanuel.abfuhr.models.AbfuhrDump
import dev.imanuel.abfuhr.models.AbfuhrLocation
import dev.imanuel.abfuhr.models.Location
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.h4
import kotlinx.html.head
import kotlinx.html.meta
import kotlinx.html.span
import kotlinx.html.stream.appendHTML
import kotlinx.html.table
import kotlinx.html.td
import kotlinx.html.title
import kotlinx.html.tr
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
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

@Serializable
@XmlSerialName("configuration")
data class abfallAbcConfiguration(
    @XmlElement(true)
    @XmlSerialName("contentFilesURL")
    val contentFilesURL: String
)

@Serializable
data class reportWaste(
    val subject: String,
    val body: String,
    val attachment: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as reportWaste

        if (subject != other.subject) return false
        if (body != other.body) return false
        if (!attachment.contentEquals(other.attachment)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = subject.hashCode()
        result = 31 * result + body.hashCode()
        result = 31 * result + (attachment?.contentHashCode() ?: 0)
        return result
    }
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

    suspend fun getContentFilesUrl(language: String): String {
        val baseUrl = when (language) {
            "en" -> "https://rest-hildesheim-en.epresto-orange.de"
            "fr" -> "https://rest-hildesheim-fr.epresto-orange.de"
            "ku" -> "https://rest-hildesheim-ku.epresto-orange.de"
            "ar" -> "https://rest-hildesheim-ar.epresto-orange.de"
            else -> "https://rest-hildesheim.epresto-orange.de"
        }
        val response = httpClient.get("$baseUrl/abfall-abc/configuration/configuration.aspx")
        val body = response.bodyAsText()
        val config = XML.v1.decodeFromString<abfallAbcConfiguration>(body)
        return config.contentFilesURL
    }

    suspend fun reportWaste(
        lat: Double,
        lon: Double,
        address: String,
        comment: String,
        picture: ByteArray?
    ): Boolean {
        val body = buildString {
            append("<html>")
            appendHTML(prettyPrint = false, xhtmlCompatible = false)
                .head {
                    meta {
                        attributes["http-equiv"] = "Content-Type"
                        attributes["content"] = "text/html"
                    }
                    title { +"ZAH App Verschmutzung gemeldet" }
                }
                appendHTML()
                .body {
                    if (picture?.isNotEmpty() == true) {
                        h4 { +"Bitte siehe Bild im Anhang." }
                    }
                    table {
                        tr {
                            td {
                                attributes["width"] = "100"
                                attributes["valign"] = "top"
                                attributes["style"] =
                                    "width: 75.0pt; background: #666666; padding: .75pt .75pt .75pt 6.0pt"

                                span {
                                    attributes["style"] =
                                        "font-size: 11.0pt; font-family: \"Calibri\", sans-serif; color: #F2F2F2"

                                    +"Koordinaten"
                                }
                            }
                            td {
                                attributes["style"] = "padding: .75pt .75pt .75pt 6.0pt"
                                span {
                                    attributes["style"] =
                                        "font-size: 11.0pt; font-family: \"Calibri\", sans-serif; color: #333333"

                                    a(href = "https://maps.google.de/maps?q=$lat,$lon&hl=de") {
                                        +"$lat,$lon"
                                    }
                                }
                            }
                        }
                        tr {
                            td {
                                attributes["width"] = "100"
                                attributes["valign"] = "top"
                                attributes["style"] =
                                    "width: 75.0pt; background: #666666; padding: .75pt .75pt .75pt 6.0pt"

                                span {
                                    attributes["style"] =
                                        "font-size: 11.0pt; font-family: \"Calibri\", sans-serif; color: #F2F2F2"

                                    +"Ort oder Straße"
                                }
                            }
                            td {
                                attributes["style"] = "padding: .75pt .75pt .75pt 6.0pt"
                                span {
                                    attributes["style"] =
                                        "font-size: 11.0pt; font-family: \"Calibri\", sans-serif; color: #333333"
                                    +address
                                }
                            }
                        }
                        tr {
                            td {
                                attributes["width"] = "100"
                                attributes["valign"] = "top"
                                attributes["style"] =
                                    "width: 75.0pt; background: #666666; padding: .75pt .75pt .75pt 6.0pt"
                                span {
                                    attributes["style"] =
                                        "font-size: 11.0pt; font-family: \"Calibri\", sans-serif; color: #F2F2F2"
                                    +"Kommentar"
                                }
                            }
                            td {
                                attributes["style"] = "padding: .75pt .75pt .75pt 6.0pt"
                                span {
                                    +comment
                                }
                            }
                        }
                    }
                }
            append("</html>")
        }

        val response =
            httpClient.post(
                "https://zah.351.compra.de/v4.4.0/zahwebservice.asmx/SendMailWithAttachment"
//                "https://httpbin.org/post"
            ) {
                val body = reportWaste(
                    subject = "ZAH App Verschmutzung gemeldet",
                    body = body,
                    attachment = picture
                )
                setBody(body)
                header("Content-Type", "application/json")
                header("Accept", "application/json")
            }

        return response.status.isSuccess()
    }
}
