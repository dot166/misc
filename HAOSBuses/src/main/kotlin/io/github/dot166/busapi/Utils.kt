package io.github.dot166.busapi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL
import kotlin.time.Duration.Companion.milliseconds

internal const val DEPARTURES_REQUEST_TIMEOUT_MS = 4500L

private val TIME_PATTERN = Regex("""^(\d{1,2}):(\d{2})$""")
private val INLINE_TIME_PATTERN = Regex("""\b(\d{1,2}):(\d{2})\b""")
internal val TABLE_PATTERN = Regex("""<table>[\s\S]*?</table>""", RegexOption.IGNORE_CASE)
internal val TABLE_ROW_PATTERN = Regex("""<tr>([\s\S]*?)</tr>""", RegexOption.IGNORE_CASE)
internal val TABLE_HEADER_PATTERN = Regex("""<th\b""", RegexOption.IGNORE_CASE)
internal val TABLE_CELL_PATTERN =
    Regex("""<td[^>]*>([\s\S]*?)</td>""", RegexOption.IGNORE_CASE)
internal val VEHICLE_DIV_PATTERN =
    Regex("""<div class="vehicle">[\s\S]*?</div>""", RegexOption.IGNORE_CASE)
internal val TRIP_ID_PATTERN = Regex("""/trips/(\d+)""")

fun extractTime(text: String): String? {
    val match = INLINE_TIME_PATTERN.find(text) ?: return null

    val hours = match.groupValues[1].padStart(2, '0')
    val minutes = match.groupValues[2]

    return "$hours:$minutes"
}

fun timeToMinutesOrNull(time: String?): Int? {
    if (time.isNullOrEmpty()) {
        return null
    }

    val match = TIME_PATTERN.matchEntire(time) ?: return null

    val hours = match.groupValues[1].toIntOrNull() ?: return null
    val minutes = match.groupValues[2].toIntOrNull() ?: return null

    if (hours !in 0..23 || minutes !in 0..59) {
        return null
    }

    return hours * 60 + minutes
}

suspend fun fetchWithTimeout(
    url: String,
    timeoutMs: Long
): String? = withTimeoutOrNull(timeoutMs.milliseconds) {
    withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection

        try {
            connection.connectTimeout = timeoutMs.toInt()
            connection.readTimeout = timeoutMs.toInt()
            connection.requestMethod = "GET"

            if (connection.responseCode !in 200..299) {
                return@withContext null
            }

            connection.inputStream
                .bufferedReader()
                .use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

fun decodeHtmlEntities(text: String): String {
    return text
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
}

fun stripHtml(html: String): String {
    return decodeHtmlEntities(
        html
            .replace(Regex("""<[^>]+>"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    )
}