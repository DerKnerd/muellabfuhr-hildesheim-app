package dev.imanuel.abfuhr.geo

private data object HildesheimBounds {
    const val UPPER_LAT : Double = 52.2969473
    const val LOWER_LAT : Double = 51.8968711
    const val UPPER_LON : Double = 10.2640159
    const val LOWER_LON : Double = 9.6280726
}

fun checkIfLocationInHildesheim(lat: Double, lon: Double): Boolean {
    return lat in HildesheimBounds.LOWER_LAT..HildesheimBounds.UPPER_LAT &&
            lon in HildesheimBounds.LOWER_LON..HildesheimBounds.UPPER_LON
}