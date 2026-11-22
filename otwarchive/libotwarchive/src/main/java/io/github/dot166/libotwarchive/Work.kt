package io.github.dot166.libotwarchive

import android.content.Context
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class Work(
    private val context: Context,
    val id: Long,
    session: Session = GuestSession(context),
    private val loadChaptersOnLoad: Boolean = true
) {
    private var document: Document? = null
    private var currentSession = session
    val session: Session get() = currentSession
    var chapters: List<Chapter> = emptyList()
        private set
    var title: String = ""
        internal set
    var summary: String = ""
        private set
    var text: String = ""
        private set
    var loaded: Boolean = false
        private set
    var words: Int = 0
        private set
    var startNotes: String = ""
        private set
    var endNotes: String = ""
        private set
    var authors: List<User> = emptyList()
        private set
    var series: List<Series> = emptyList()
        private set
    var language: String = "Unknown"
        private set
    var rating: String? = null
        private set
    var status: String = "Work in Progress"
        private set
    var complete: Boolean = false
        private set
    var restricted: Boolean = false
        private set
    var hits: Int = 0
        private set
    var kudos: Int = 0
        private set
    var comments: Int = 0
        private set
    var bookmarks: Int = 0
        private set
    var nchapters: Int = 0
        private set
    var expectedChapters: Int? = null
        private set
    var recommended: Boolean = false
        internal set
    var datePublished: LocalDate? = null
        private set
    var dateUpdated: LocalDate? = null
        private set
    var dateEdited: LocalDate? = null
        private set
    var tags: List<String> = emptyList()
        private set
    var characters: List<String> = emptyList()
        private set
    var relationships: List<String> = emptyList()
        private set
    var fandoms: List<String> = emptyList()
        private set
    var categories: List<String> = emptyList()
        private set
    var warnings: List<String> = emptyList()
        private set
    var collections: List<String> = emptyList()
        private set
    val oneshot: Boolean get() = nchapters == 1
    val url: String get() = "$AO3_ORIGIN/works/$id"
    var lastReloaded: Long = 0
        private set
    val authenticityToken: String?
        get() = document?.selectFirst("meta[name=csrf-token]")?.attr("content")
    val isSubscribed: Boolean
        get() {
            if (!session.isAuthed) throw AuthError("Subscription state requires an authenticated session")
            return document?.selectFirst("li.subscribe input[name=commit][value=Unsubscribe]") != null
        }
    val bookmarkId: Long?
        get() = document?.selectFirst("#bookmark-form form[action^=/bookmarks/]")?.attr("action")
            ?.substringAfterLast('/')?.toLongOrNull()
    val metadata: Map<String, Any?>
        get() = mapOf(
            "id" to id, "title" to title, "summary" to summary, "words" to words,
            "nchapters" to nchapters, "expected_chapters" to expectedChapters,
            "language" to language, "rating" to rating, "status" to status,
            "complete" to complete, "restricted" to restricted, "hits" to hits,
            "kudos" to kudos, "comments" to comments, "bookmarks" to bookmarks,
            "authors" to authors.map(User::username), "series" to series.map(Series::name),
            "tags" to tags, "characters" to characters, "relationships" to relationships,
            "fandoms" to fandoms, "categories" to categories, "warnings" to warnings,
            "collections" to collections
        )

    init {
        require(id > 0) { "work id must be positive" }
    }

    suspend fun reload() = reload(loadChaptersOnLoad)

    suspend fun reload(loadChapters: Boolean = true) {
        val response = AO3Http.request(
            context,
            "$url?view_adult=true&view_full_work=true"
        )
        val parsed = parseHtml(response.body)
        response.requireNotCloudflare()
        response.requireStatus()
        if (parsed.selectFirst("h2.heading")?.text()?.contains("Error 404", ignoreCase = true) == true ||
            parsed.selectFirst("#main")?.text()?.contains("Sorry, we couldn't find the work", ignoreCase = true) == true
        ) {
            throw InvalidIdError("Cannot find work $id")
        }
        document = parsed
        loaded = true
        lastReloaded = System.currentTimeMillis()
        parseMetadata(parsed)
        if (loadChapters) parseChapters(parsed) else chapters = emptyList()
    }

    private fun parseMetadata(doc: Document) {
        title = doc.selectFirst("div.preface.group h2.title")?.textOrBlank()
            ?: doc.selectFirst("h2.title")?.textOrBlank().orEmpty()
        summary = doc.selectFirst("div.preface.group blockquote.userstuff")?.text().orEmpty()
        startNotes = paragraphs(doc.selectFirst("div.notes.module"), trim = true)
        endNotes = paragraphs(doc.selectFirst("#work_endnotes"), trim = false)
        val stats = doc.selectFirst("dl.stats")
        nchapters = stats?.selectFirst("dd.chapters")?.text()?.substringBefore('/')?.number() ?: 0
        expectedChapters = stats?.selectFirst("dd.chapters")?.text()
            ?.substringAfter('/', "")?.numberOrNull()
        words = stats?.selectFirst("dd.words")?.text()?.number() ?: 0
        hits = stats?.selectFirst("dd.hits")?.text()?.number() ?: 0
        kudos = stats?.selectFirst("dd.kudos")?.text()?.number() ?: 0
        comments = stats?.selectFirst("dd.comments")?.text()?.number() ?: 0
        bookmarks = stats?.selectFirst("dd.bookmarks")?.text()?.number() ?: 0
        language = stats?.selectFirst("dd.language")?.textOrBlank() ?: "Unknown"
        rating = doc.selectFirst("dd.rating.tags a")?.text()
        restricted = doc.selectFirst("img[title=Restricted]") != null
        complete = nchapters > 0 && expectedChapters == nchapters
        status = if (complete) "Completed" else "Work in Progress"
        tags = doc.select("dd.freeform.tags li a").map { it.text() }
        characters = doc.select("dd.character.tags li a").map { it.text() }
        relationships = doc.select("dd.relationship.tags li a").map { it.text() }
        fandoms = doc.select("dd.fandom.tags li a").map { it.text() }
        categories = doc.select("dd.category.tags li a").map { it.text() }
        warnings = doc.select("dd.warning.tags li a").map { it.text() }
        collections = doc.select("dd.collections a").map { it.text() }
        authors = doc.select("h3.byline.heading").firstOrNull()?.text()
            ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)
            ?.map { User(context, it, session) } ?: emptyList()
        series = doc.select("dd.series span.position a[href^=/series/]").mapNotNull { link ->
            val seriesId = link.attr("href").substringAfterLast('/').toLongOrNull() ?: return@mapNotNull null
            Series(context, seriesId, session).apply { name = link.text() }
        }
        datePublished = doc.selectFirst("dd.published")?.text()?.toLocalDate()
        dateUpdated = doc.selectFirst("dd.status")?.text()?.toLocalDate() ?: datePublished
        val editedTimestamp = doc.selectFirst("li.download ul a[href]")?.attr("href")
            ?.substringAfterLast('=')?.toLongOrNull()
        dateEdited = editedTimestamp?.let {
            Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).toLocalDate()
        } ?: datePublished
    }

    private fun parseChapters(doc: Document) {
        val container = doc.getElementById("chapters")
        if (container == null) {
            chapters = emptyList()
            text = ""
            return
        }
        val chapterElements = container.select("div[id^=chapter-]")
        if (nchapters <= 1 || chapterElements.isEmpty()) {
            val chapter = Chapter(context, -1, this, session, html = container.outerHtml())
            chapters = listOf(chapter)
        } else {
            chapters = chapterElements.mapNotNull { element ->
                val href = element.selectFirst("div.chapter.preface.group h3.title a[href]")?.attr("href")
                val chapterId = href?.substringAfterLast('/')?.toLongOrNull() ?: return@mapNotNull null
                Chapter(context, chapterId, this, session, html = element.outerHtml())
            }
        }
        text = chapters.joinToString("\n") { it.text }
        words = if (words == 0) wordCount(text) else words
    }

    fun loadChapters() {
        val doc = document ?: throw UnloadedError("Work is not loaded")
        parseChapters(doc)
    }

    fun getImages(): Map<Int, List<Pair<String, Int>>> {
        checkLoaded()
        return chapters.flatMap { it.getImages().entries }
            .associate { it.key to it.value }
    }

    suspend fun download(fileType: String = "PDF"): ByteArray {
        checkLoaded()
        val link = document!!.select("li.download li a").firstOrNull {
            it.text().equals(fileType, ignoreCase = true)
        } ?: throw UnexpectedResponseError("Filetype '$fileType' is not available")
        val downloadUrl = link.absUrl("href").ifBlank { "$AO3_ORIGIN/${link.attr("href").trimStart('/')}" }
        val response = AO3Http.request(
            context,
            downloadUrl,
            binaryResponse = true
        )
        response.requireStatus()
        if (!response.isSuccessful) throw DownloadError("AO3 returned HTTP ${response.statusCode} while downloading")
        return response.bodyBytes
    }

    suspend fun downloadToFile(file: File, fileType: String = "PDF") {
        file.writeBytes(download(fileType))
    }

    suspend fun getComments(maximum: Int?): List<Comment> {
        checkLoaded()
        require(maximum == null || maximum >= 0) { "maximum cannot be negative" }
        val first = AO3Http.request(context, "$url?page=1&show_comments=true&view_adult=true&view_full_work=true")
        first.requireStatus()
        val firstDoc = parseHtml(first.body)
        val pages = firstDoc.select("#comments_placeholder ol.pagination.actions li")
            .mapNotNull { it.text().toIntOrNull() }.maxOrNull() ?: 1
        val result = mutableListOf<Comment>()
        for (page in 1..pages) {
            val doc = if (page == 1) firstDoc else parseHtml(
                AO3Http.request(
                    context,
                    "$url?page=$page&show_comments=true&view_adult=true&view_full_work=true"
                ).also { it.requireStatus() }.body
            )
            doc.select("ol.thread > li[role=article]").forEach { element ->
                if (maximum != null && result.size >= maximum) return result
                val commentId = element.id().removePrefix("comment_").toLongOrNull() ?: return@forEach
                result += Comment(
                    context, commentId, this, session, authenticityToken,
                    element.selectFirst("h4.heading.byline a")?.text(),
                    element.selectFirst("blockquote")?.text().orEmpty()
                )
            }
        }
        return result
    }

    suspend fun leaveKudos(): Boolean = AO3Actions.kudos(this, session)

    suspend fun subscribe() = AO3Actions.subscribe(this, "Work", session)
    suspend fun unsubscribe() {
        val subId = document?.selectFirst("li.subscribe form[action]")?.attr("action")
            ?.substringAfterLast('/')?.toLongOrNull()
            ?: throw AuthError("This work is not subscribed")
        AO3Actions.subscribe(this, "Work", session, unsubscribe = true, subscriptionId = subId)
    }

    suspend fun comment(
        commentText: String,
        email: String,
        name: String,
        pseud: String?
    ) = AO3Actions.comment(this, commentText, session, true, null, email, name, pseud)

    suspend fun bookmark(
        notes: String = "",
        tags: List<String> = emptyList(),
        collections: List<String> = emptyList(),
        private: Boolean = false,
        recommend: Boolean = false,
        pseud: String? = null
    ) = AO3Actions.bookmark(this, session, notes, tags, collections, private, recommend, pseud)

    suspend fun deleteBookmark() {
        checkLoaded()
        val id = document!!.selectFirst("#bookmark-form form[action]")?.attr("action")
            ?.substringAfterLast('/')?.toLongOrNull() ?: throw BookmarkError("No bookmark was found")
        AO3Actions.deleteBookmark(id, session, authenticityToken)
    }

    suspend fun collect(collections: List<String>) = AO3Actions.collect(this, session, collections)

    internal fun populateFromBanner(item: Element) {
        title = item.selectFirst("h4 a[href^=/works/]")?.text().orEmpty()
        authors = item.select("h4 a[rel~=author]").map {
            User(context, it.text(), session)
        }
        fandoms = item.select("h5.fandoms a").map { it.text() }
        val tagRoot = item.selectFirst("ul.tags")
        warnings = tagRoot?.select("li.warnings")?.map { it.text() }.orEmpty()
        relationships = tagRoot?.select("li.relationships")?.map { it.text() }.orEmpty()
        characters = tagRoot?.select("li.characters")?.map { it.text() }.orEmpty()
        tags = tagRoot?.select("li.freeforms")?.map { it.text() }.orEmpty()
        val required = item.selectFirst("ul.required-tags")
        rating = required?.selectFirst(".rating")?.text()
        categories = required?.selectFirst(".category")?.text()?.split(", ") ?: emptyList()
        summary = item.selectFirst(".userstuff.summary")?.text().orEmpty()
        val stats = item.selectFirst("dl.stats")
        words = stats?.selectFirst("dd.words")?.text()?.number() ?: 0
        bookmarks = stats?.selectFirst("dd.bookmarks")?.text()?.number() ?: 0
        hits = stats?.selectFirst("dd.hits")?.text()?.number() ?: 0
        kudos = stats?.selectFirst("dd.kudos")?.text()?.number() ?: 0
        comments = stats?.selectFirst("dd.comments")?.text()?.number() ?: 0
        language = stats?.selectFirst("dd.language")?.textOrBlank() ?: "Unknown"
        val chapterCount = stats?.selectFirst("dd.chapters")?.text()
        nchapters = chapterCount?.substringBefore('/')?.number() ?: 0
        expectedChapters = chapterCount?.substringAfter('/', "")?.numberOrNull()
        complete = nchapters > 0 && nchapters == expectedChapters
        status = if (complete) "Completed" else "Work in Progress"
        restricted = item.selectFirst("img[title=Restricted]") != null
        dateUpdated = item.selectFirst("p.datetime")?.text()?.let { runCatching {
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
                .parse(it, LocalDate::from)
        }.getOrNull() }
    }

    private fun checkLoaded() {
        if (!loaded || document == null) throw UnloadedError("Work is not loaded; call reload() first")
    }

    fun setSession(session: Session) {
        currentSession = session
    }

    override fun toString(): String = title.ifBlank { "AO3 item $id" }

    companion object {
        suspend fun load(
            context: Context,
            id: Long,
            session: Session = GuestSession(context),
            loadChapters: Boolean = true
        ): Work = Work(context, id, session, loadChaptersOnLoad = loadChapters).also {
            it.reload(loadChapters)
        }
    }
}

private fun String.number(): Int = replace(",", "").trim().toIntOrNull() ?: 0
private fun String.numberOrNull(): Int? = replace(",", "").trim().toIntOrNull()
private fun String.toLocalDate(): LocalDate? =
    runCatching { LocalDate.parse(trim()) }.getOrNull()
private fun Element.textOrBlank(): String = text().trim()
private fun paragraphs(element: Element?, trim: Boolean): String =
    element?.select("p")?.map { if (trim) it.text().trim() else it.text() }
        ?.joinToString(separator = "\n", postfix = if (element.select("p").isEmpty()) "" else "\n")
        .orEmpty()
