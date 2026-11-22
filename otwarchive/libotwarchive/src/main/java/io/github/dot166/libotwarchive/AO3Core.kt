package io.github.dot166.libotwarchive

import android.content.Context
import kotlinx.coroutines.delay
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

const val AO3_ORIGIN = "https://archiveofourown.org"

open class AO3Exception(message: String, cause: Throwable? = null) : Exception(message, cause)
class LoginError(message: String) : AO3Exception(message)
class UnloadedError(message: String) : AO3Exception(message)
class UnexpectedResponseError(message: String) : AO3Exception(message)
class InvalidIdError(message: String) : AO3Exception(message)
class DownloadError(message: String) : AO3Exception(message)
class AuthError(message: String) : AO3Exception(message)
class DuplicateCommentError(message: String) : AO3Exception(message)
class PseudError(message: String) : AO3Exception(message)
class HttpError(message: String) : AO3Exception(message)
class BookmarkError(message: String) : AO3Exception(message)
class CollectError(message: String) : AO3Exception(message)
class CloudFailDetectedException : AO3Exception("AO3's browser challenge needs to be completed")

data class Constraint(val lowerBound: Int = 0, val upperBound: Int? = null) {
    val string: String
        get() = when {
            lowerBound == 0 -> "<$upperBound"
            upperBound == null -> ">$lowerBound"
            lowerBound == upperBound -> lowerBound.toString()
            else -> "$lowerBound-$upperBound"
        }

    override fun toString(): String = string
}

class Query {
    private val fields = mutableListOf<Pair<String, String>>()

    fun addField(name: String, value: Any) {
        fields += name to value.toString()
    }

    val string: String
        get() = fields.joinToString("&") { (name, value) ->
            "${encode(name)}=${encode(value)}"
        }
}

fun wordCount(text: String): Int =
    text.split(Regex("[ \\n\\t]+")).count(String::isNotEmpty)

fun workIdFromUrl(url: String): Long? {
    val uriPath = runCatching { URI(url).path }.getOrNull() ?: url.substringBefore('?')
    val parts = uriPath.split('/')
    val worksIndex = parts.indexOf("works")
    return parts.getOrNull(worksIndex + 1)?.toLongOrNull()
}

fun urlJoin(base: String, vararg parts: String): String =
    parts.fold(base.trimEnd('/')) { result, part -> "$result/${part.trim('/')}" }

fun encode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.name())

fun formBody(fields: Map<String, Any?>): String =
    fields.entries
        .filter { it.value != null }
        .joinToString("&") { (key, value) -> "${encode(key)}=${encode(value.toString())}" }

internal fun parseHtml(html: String): Document = Jsoup.parse(html)

internal fun Element?.textOrEmpty(): String = this?.text()?.trim().orEmpty()

internal fun Element?.numericText(default: Int = 0): Int =
    this?.text()?.replace(",", "")?.trim()?.toIntOrNull() ?: default

internal fun Element?.listItems(): List<String> =
    this?.select("li a")?.map { it.text().trim() } ?: emptyList()

abstract class Session(
    val context: Context,
    username: String = ""
) {
    abstract val isAuthed: Boolean
    var authenticityToken: String? = null
        protected set
    var username: String = username
        protected set
    val user: User
        get() = User(context, username, this)

    suspend fun get(url: String): AO3HttpResponse =
        AO3Http.request(context, url).also(AO3HttpResponse::requireStatus)

    suspend fun post(
        url: String,
        fields: Map<String, Any?>,
        headers: Map<String, String> = emptyMap()
    ): AO3HttpResponse =
        AO3Http.request(
            context,
            url,
            method = "POST",
            headers = headers + ("Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8"),
            body = formBody(fields)
        ).also(AO3HttpResponse::requireStatus)

    suspend fun request(url: String): Document = parseHtml(get(url).body)

    open suspend fun refreshAuthToken() {
        val page = request(if (isAuthed) "$AO3_ORIGIN/users/${encode(username)}" else AO3_ORIGIN)
        authenticityToken = page.selectFirst("input[name=authenticity_token]")?.attr("value")
            ?: page.selectFirst("meta[name=csrf-token]")?.attr("content")
                    ?: throw UnexpectedResponseError("Couldn't refresh the authenticity token")
    }

    suspend fun comment(
        commentable: Any,
        commentText: String,
        oneshot: Boolean = false,
        commentId: Long? = null,
        email: String = "",
        name: String = "",
        pseud: String? = null
    ): AO3HttpResponse = AO3Actions.comment(
        commentable,
        commentText,
        this,
        oneshot,
        commentId,
        email,
        name,
        pseud
    )

    suspend fun kudos(work: Work): Boolean = AO3Actions.kudos(work, this)

    open fun close() = Unit
}

class GuestSession(
    context: Context,
): Session(context, "") {
    override val isAuthed: Boolean = false
}

class LoggedInSession private constructor(
    context: Context,
    username: String
) : Session(context, username) {
    override val isAuthed: Boolean = true
    private val cacheLock = Any()
    private var subscriptionsCache: List<Any>? = null
    private var bookmarksCache: List<Work>? = null
    private var historyCache: List<HistoryEntry>? = null
    private var bookmarksCountCache: Int? = null

    suspend fun getSubscriptions(): List<Any> {
        subscriptionsCache?.let { return it }
        val result = mutableListOf<Any>()
        val first = request("$AO3_ORIGIN/users/${encode(username)}/subscriptions?page=1")
        val pages = paginationPages(first)
        for (page in 1..pages) {
            val document = if (page == 1) first else request(
                "$AO3_ORIGIN/users/${encode(username)}/subscriptions?page=$page"
            )
            document.select("dl.subscription.index.group dt").forEach { item ->
                val link = item.selectFirst("a[href]") ?: return@forEach
                val href = link.attr("href")
                when {
                    href.startsWith("/works/") -> {
                        val id = workIdFromUrl(href) ?: return@forEach
                        result += Work(context, id, this).apply { title = link.text() }
                    }
                    href.startsWith("/users/") -> result += User(context, link.text(), this)
                    href.startsWith("/series/") -> {
                        val id = href.substringAfterLast('/').toLongOrNull() ?: return@forEach
                        result += Series(context, id, this).apply { name = link.text() }
                    }
                }
            }
        }
        subscriptionsCache = result
        return result
    }

    suspend fun getWorkSubscriptions(): List<Work> = getSubscriptions().filterIsInstance<Work>()
    suspend fun getSeriesSubscriptions(): List<Series> = getSubscriptions().filterIsInstance<Series>()
    suspend fun getUserSubscriptions(): List<User> = getSubscriptions().filterIsInstance<User>()

    suspend fun getBookmarks(): List<Work> {
        bookmarksCache?.let { return it }
        val result = mutableListOf<Work>()
        val first = request("$AO3_ORIGIN/users/${encode(username)}/bookmarks?page=1")
        bookmarksCountCache = first.selectFirst("div.bookmarks-index h2")?.text()
            ?.replace(",", "")?.split(Regex("\\s+"))?.getOrNull(4)?.toIntOrNull() ?: 0
        for (page in 1..paginationPages(first)) {
            val document = if (page == 1) first else request(
                "$AO3_ORIGIN/users/${encode(username)}/bookmarks?page=$page"
            )
            document.select("ol.bookmark.index.group li[role=article]").forEach { item ->
                val link = item.selectFirst("h4 a[href^=/works/]") ?: return@forEach
                val id = workIdFromUrl(link.attr("href")) ?: return@forEach
                result += Work(context, id, this).apply {
                    title = link.text()
                    recommended = item.selectFirst("span[title=Rec]") != null
                }
            }
        }
        bookmarksCache = result
        return result
    }

    suspend fun getBookmarkCount(): Int {
        bookmarksCountCache?.let { return it }
        getBookmarks()
        return bookmarksCountCache ?: 0
    }

    suspend fun getHistory(
        startPage: Int = 1,
        maxPages: Int? = null,
        delayBetweenPagesMs: Long = 3_000L
    ): List<HistoryEntry> {
        require(startPage > 0) { "startPage must be greater than zero" }
        require(maxPages == null || maxPages >= 0) { "maxPages cannot be negative" }
        historyCache?.let { return it }
        val result = mutableListOf<HistoryEntry>()
        val first = request("$AO3_ORIGIN/users/${encode(username)}/readings?page=$startPage")
        val lastPage = paginationPages(first)
        val endPage = maxPages?.let { minOf(lastPage, startPage + it - 1) } ?: lastPage
        for (page in startPage..endPage) {
            if (page > startPage && delayBetweenPagesMs > 0) delay(delayBetweenPagesMs)
            val document = if (page == startPage) first else request(
                "$AO3_ORIGIN/users/${encode(username)}/readings?page=$page"
            )
            document.select("ol.reading.work.index.group li[role=article]").forEach { item ->
                val link = item.selectFirst("h4 a[href^=/works/]") ?: return@forEach
                val workId = workIdFromUrl(link.attr("href")) ?: return@forEach
                val heading = item.selectFirst("h4.viewed.heading")?.text().orEmpty()
                val visits = Regex("Visited (\\d+) times").find(heading)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                val date = Regex("Last visited: (\\d{2} \\w{3} \\d{4})").find(heading)
                    ?.groupValues?.get(1)
                    ?.let { runCatching { DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH).let { f -> LocalDate.parse(it, f) } }.getOrNull() }
                result += HistoryEntry(Work(context, workId, this).apply { title = link.text() }, visits, date)
            }
        }
        historyCache = result
        return result
    }

    suspend fun getStatistics(year: Int? = null): Map<String, Int> {
        val yearParam = year?.toString() ?: "All+Years"
        val document = request("$AO3_ORIGIN/users/${encode(username)}/stats?year=$yearParam")
        return document.select("dl.statistics.meta.group dt").mapNotNull { label ->
            val value = label.nextElementSibling()?.numericText(-1) ?: -1
            value.takeIf { it >= 0 }?.let {
                label.text().removeSuffix(":").lowercase().replace(' ', '_') to it
            }
        }.toMap()
    }

    suspend fun getMarkedForLater(delayBetweenPagesMs: Long = 1_000L): List<Work> {
        val first = request("$AO3_ORIGIN/users/${encode(username)}/readings?page=1&show=to-read")
        val lastPage = paginationPages(first)
        val works = mutableListOf<Work>()
        for (page in 1..lastPage) {
            if (page > 1 && delayBetweenPagesMs > 0) delay(delayBetweenPagesMs)
            val document = if (page == 1) first else request(
                "$AO3_ORIGIN/users/${encode(username)}/readings?page=$page&show=to-read"
            )
            document.select("li[role=article] h4 a[href^=/works/]").forEach { link ->
                workIdFromUrl(link.attr("href"))?.let { id ->
                    works += Work(context, id, this).apply { title = link.text() }
                }
            }
        }
        return works
    }

    fun clearCache() {
        synchronized(cacheLock) {
            subscriptionsCache = null
            bookmarksCache = null
            historyCache = null
            bookmarksCountCache = null
        }
    }

    companion object {
        suspend fun login(context: Context, username: String, password: String): LoggedInSession {
            val session = LoggedInSession(context.applicationContext, username)
            val loginPage = session.request("$AO3_ORIGIN/users/login")
            session.authenticityToken = loginPage.selectFirst("input[name=authenticity_token]")?.attr("value")
                ?: throw UnexpectedResponseError("AO3 login page did not include an authenticity token")
            val response = session.post(
                "$AO3_ORIGIN/users/login",
                mapOf(
                    "user[login]" to username,
                    "user[password]" to password,
                    "authenticity_token" to session.authenticityToken
                ),
                mapOf("Referer" to "$AO3_ORIGIN/users/login")
            )
            val page = parseHtml(response.body)
            if (!response.redirected ||
                page.selectFirst("#error, .error") != null ||
                page.selectFirst("input[name='user[login]']") != null
            ) {
                throw LoginError("AO3 login failed; check the username and password")
            }
            return session
        }

        private fun paginationPages(document: Document): Int =
            document.select("ol[title=pagination] li").mapNotNull { it.text().toIntOrNull() }.maxOrNull() ?: 1
    }
}

data class HistoryEntry(val work: Work, val visits: Int, val lastVisited: LocalDate?)

internal fun paginationPages(document: Document): Int =
    document.select("ol[title=pagination] li").mapNotNull { it.text().toIntOrNull() }.maxOrNull() ?: 1
