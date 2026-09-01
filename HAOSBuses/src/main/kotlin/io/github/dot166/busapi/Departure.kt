package io.github.dot166.busapi

data class Departure(
    val destination: String,
    val expected: String?,
    val scheduled: String,
    val service: String,
    val tripId: Long?
)