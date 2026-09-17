package dev.imanuel.abfuhr.database

fun fts5PrefixQuery(value: String): String =
    value
        .trim()
        .split(Regex("\\s+"))
        .filter(String::isNotEmpty)
        .joinToString(" ") { token ->
            "\"${token.replace("\"", "\"\"")}\"*"
        }

fun AbfallDatabase.searchAddressByKeyword(keyword: String): List<AbfuhrLocation> =
    abfuhrQueries.searchAbfuhrLocationByKeyword(fts5PrefixQuery(keyword)).executeAsList()

fun AbfallDatabase.searchAbfallAbcByKeyword(
    language: String,
    keyword: String
): List<AbfallAbcWaste> =
    abfallAbcQueries.searchAbfallAbcByKeyword(language, fts5PrefixQuery(keyword)).executeAsList()