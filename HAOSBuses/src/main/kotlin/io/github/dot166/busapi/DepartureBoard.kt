package io.github.dot166.busapi

import java.time.LocalDateTime

class DepartureBoard(val atcoCode: String) {
    @JvmOverloads
    suspend fun getNextDepartures(time: LocalDateTime = LocalDateTime.now()): List<Departure> {
        val url = "https://bustimes.org/stops/$atcoCode/departures?date=${time.year}-${time.month.value}-${time.dayOfMonth}&time=${time.hour}%3A${time.minute}"

        val departuresHtml = try {
            fetchWithTimeout(
                url = url,
                timeoutMs = DEPARTURES_REQUEST_TIMEOUT_MS
            )
        } catch (_: Exception) {
            null
        }

        if (departuresHtml == null) {
            return emptyList()
        }

        val departures = try {
            parseDeparturesHtml(departuresHtml)
        } catch (_: Exception) {
            emptyList()
        }

        if (departures.isEmpty()) {
            return emptyList()
        }
        val timeMinutes = time.hour * 60 + time.minute

        return departures
            .mapIndexed { index, departure ->
                Triple(
                    departure,
                    index,
                    getDepartureSortDiff(departure, timeMinutes)
                )
            }
            .sortedWith(
                compareBy<Triple<Departure, Int, Int>> { it.third }
                    .thenBy { it.second }
            )
            .map { it.first }
    }

    private fun getDepartureSortDiff(
        departure: Departure,
        nowMinutes: Int
    ): Int {
        val displayTime = departure.expected ?: departure.scheduled
        val departureMinutes = timeToMinutesOrNull(displayTime)
            ?: return Int.MAX_VALUE

        return (departureMinutes - nowMinutes + 24 * 60) % (24 * 60)
    }

    private fun parseDeparturesHtml(html: String): List<Departure> {
        val table = TABLE_PATTERN.find(html)?.value
            ?: return emptyList()

        val departures = mutableListOf<Departure>()

        for (rowMatch in TABLE_ROW_PATTERN.findAll(table)) {
            val rowHtml = rowMatch.groupValues[1]

            if (TABLE_HEADER_PATTERN.containsMatchIn(rowHtml)) {
                continue
            }

            val cells = TABLE_CELL_PATTERN
                .findAll(rowHtml)
                .map { it.groupValues[1] }
                .toList()

            if (cells.size < 3) {
                continue
            }

            val service = stripHtml(cells[0])

            val destination = stripHtml(
                VEHICLE_DIV_PATTERN.replace(cells[1], " ")
            )

            val scheduled = extractTime(
                stripHtml(cells[2])
            )

            val expected = if (cells.size > 3) {
                extractTime(stripHtml(cells[3]))
            } else {
                null
            }

            val tripId = TRIP_ID_PATTERN
                .find(cells[2])
                ?.groupValues
                ?.get(1)
                ?.toLongOrNull()

            if (
                service.isEmpty() ||
                destination.isEmpty() ||
                scheduled == null
            ) {
                continue
            }

            departures += Departure(
                service = service,
                tripId = tripId,
                destination = destination,
                scheduled = scheduled,
                expected = expected
            )
        }

        return departures
    }
}