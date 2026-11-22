package io.github.dot166.libotwarchive

import android.content.Context
import java.io.File

object AO3Resources {
    private val fandoms = linkedMapOf(
        "anime_manga_fandoms" to "Anime%20*a*%20Manga",
        "books_literature_fandoms" to "Books%20*a*%20Literature",
        "cartoons_comics_graphicnovels_fandoms" to "Cartoons%20*a*%20Comics%20*a*%20Graphic%20Novels",
        "celebrities_real_people_fandoms" to "Celebrities%20*a*%20Real%20People",
        "movies_fandoms" to "Movies",
        "music_bands_fandoms" to "Music%20*a*%20Bands",
        "other_media_fandoms" to "Other%20Media",
        "theater_fandoms" to "Theater",
        "tvshows_fandoms" to "TV%20Shows",
        "videogames_fandoms" to "Video%20Games",
        "uncategorized_fandoms" to "Uncategorized%20Fandoms"
    )
    private val languages = mapOf("languages" to "")

    fun getResources(): Map<String, List<String>> =
        mapOf("fandoms" to fandoms.keys.toList(), "languages" to languages.keys.toList())

    fun hasResource(context: Context, resource: String): Boolean =
        File(File(context.filesDir, "ao3-resources"), "$resource.txt").isFile

    suspend fun download(context: Context, resource: String) {
        val url = when {
            resource == "languages" -> "$AO3_ORIGIN/languages"
            resource in fandoms -> "$AO3_ORIGIN/media/${fandoms.getValue(resource)}/fandoms"
            else -> throw IllegalArgumentException("'$resource' is not a valid AO3 resource")
        }
        val response = AO3Http.request(context, url)
        response.requireStatus()
        if (response.statusCode !in 200..299) {
            throw UnexpectedResponseError("Unable to download AO3 resource '$resource' (HTTP ${response.statusCode})")
        }
        val document = parseHtml(response.body)
        val values = if (resource == "languages") {
            document.select("dl.language.index.group dt").map { item ->
                val link = item.selectFirst("a[href]")
                item.text().trim() + "\t" + (link?.attr("href")?.substringAfterLast('/') ?: "")
            }
        } else {
            document.select("ol.alphabet.fandom.index.group a.tag").map { it.text().trim() }
        }
        if (values.isEmpty()) throw UnexpectedResponseError("AO3 returned no data for resource '$resource'")
        val directory = File(context.filesDir, "ao3-resources")
        if (!directory.exists() && !directory.mkdirs()) {
            throw UnexpectedResponseError("Could not create the AO3 resource directory")
        }
        File(directory, "$resource.txt").writeText(values.joinToString("\n"), Charsets.UTF_8)
    }

    suspend fun downloadAll(context: Context, redownload: Boolean = false) {
        getResources().values.flatten().forEach { resource ->
            if (redownload || !hasResource(context, resource)) download(context, resource)
        }
    }

    fun loadFandoms(context: Context): List<String> =
        getResources().getValue("fandoms").flatMap { resource ->
            readResource(context, resource)
        }

    fun loadLanguages(context: Context): List<Pair<String, String?>> =
        readResource(context, "languages").map { line ->
            val parts = line.split('\t', limit = 2)
            parts[0] to parts.getOrNull(1)?.takeIf(String::isNotBlank)
        }

    private fun readResource(context: Context, resource: String): List<String> {
        val file = File(File(context.filesDir, "ao3-resources"), "$resource.txt")
        if (!file.isFile) throw IllegalStateException("AO3 resource '$resource' is not downloaded")
        return file.readLines(Charsets.UTF_8)
    }
}

class AO3Extra(private val context: Context) {
    fun getResources(): Map<String, List<String>> = AO3Resources.getResources()
    fun hasResource(resource: String): Boolean = AO3Resources.hasResource(context, resource)
    suspend fun download(resource: String) = AO3Resources.download(context, resource)
    suspend fun downloadAll(redownload: Boolean = false) =
        AO3Resources.downloadAll(context, redownload)
}

class AO3Utils(private val context: Context) {
    private var fandomCache: List<String>? = null
    private var languageCache: List<Pair<String, String?>>? = null

    fun setRequestsPerWindow(value: Int) = AO3Http.setRateLimit(value)
    fun setTimeWindow(valueMs: Long) =
        AO3Http.setTimeWindow(valueMs)

    fun limitRequests(limit: Boolean = true) =
        AO3Http.setRateLimit(if (limit) 12 else -1)

    fun loadFandoms(): List<String> =
        AO3Resources.loadFandoms(context).also { fandomCache = it }

    fun loadLanguages(): List<Pair<String, String?>> =
        AO3Resources.loadLanguages(context).also { languageCache = it }

    fun getLanguages(): List<Pair<String, String?>> =
        languageCache?.toList() ?: throw UnloadedError("Call loadLanguages() before requesting languages")

    fun searchFandom(fandomString: String): List<String> =
        (fandomCache ?: throw UnloadedError("Call loadFandoms() before searching fandoms"))
            .filter { it.contains(fandomString, ignoreCase = true) }
}
