package dev.imanuel.abfuhr.database

fun fts5PrefixQuery(value: String): String =
    value
        .trim()
        .split(Regex("\\s+"))
        .filter(String::isNotEmpty)
        .joinToString(" ") { token ->
            "\"${token.replace("\"", "\"\"")}\"*"
        }

fun AbfallDatabase.searchAddressByKeyword(keyword: String): List<AbfuhrLocation> {
    return if (keyword.isEmpty()) {
        abfuhrQueries.getAllLocations().executeAsList()
    } else {
        abfuhrQueries.searchAbfuhrLocationByKeyword(fts5PrefixQuery(keyword)).executeAsList()
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
