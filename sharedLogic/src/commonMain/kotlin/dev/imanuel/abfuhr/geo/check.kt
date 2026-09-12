package dev.imanuel.abfuhr.geo

private data object PeineBounds {
    val upperLat: Double = 52.4487018
    val lowerLat: Double = 52.1749969
    val upperLon: Double = 10.4544087
    val lowerLon: Double = 9.9955707
}

private data object HildesheimBounds {
    val upperLat: Double = 52.2969473
    val lowerLat: Double = 51.8968711
    val upperLon: Double = 10.2640159
    val lowerLon: Double = 9.6280726
}

fun checkIfLocationInHildesheim(lat: Double, lon: Double): Boolean {
    return lat in HildesheimBounds.lowerLat..HildesheimBounds.upperLat &&
            lon in HildesheimBounds.lowerLon..HildesheimBounds.upperLon
}

fun checkIfLocationInPeine(lat: Double, lon: Double): Boolean {
    return lat in PeineBounds.lowerLat..PeineBounds.upperLat &&
            lon in PeineBounds.lowerLon..PeineBounds.upperLon
}