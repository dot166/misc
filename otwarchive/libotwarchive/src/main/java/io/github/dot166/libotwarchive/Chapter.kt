package io.github.dot166.libotwarchive

import android.content.Context
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class Chapter internal constructor(
    private val context: Context,
    val id: Long,
    val work: Work,
    session: Session,
    html: String? = null
) {
    private var document: Document? = html?.let(::parseHtml)
    private var currentSession = session
    val session: Session get() = currentSession
    var title: String = ""
        private set
    var summary: String = ""
        private set
    var text: String = ""
        private set
    var loaded: Boolean = document != null
        private set
    var words: Int = 0
        private set
    var startNotes: String = ""
        private set
    var endNotes: String = ""
        private set
    val number: Int
        get() = document?.selectFirst("[id^=chapter-]")?.id()?.substringAfterLast('-')?.toIntOrNull()
            ?: if (id < 0) 1 else 0
    val url: String
        get() = if (id < 0) work.url else "${work.url}/chapters/$id"
    val authenticityToken: String?
        get() = work.authenticityToken

    init {
        require(id == -1L || id > 0) { "chapter id must be positive or -1 for a one-shot" }
        document?.let(::parseContent)
    }

    suspend fun reload() {
        if (id < 0) {
            work.reload()
            val matching = work.chapters.firstOrNull { it.id < 0 }
                ?: throw UnloadedError("Could not load the one-shot chapter")
            document = matching.document
        } else {
            val response = AO3Http.request(
                context,
                "$AO3_ORIGIN/chapters/$id?view_adult=true"
            )
            val parsed = parseHtml(response.body)
            response.requireNotCloudflare()
            response.requireStatus()
            document = parsed
        }
        loaded = true
        document?.let(::parseContent)
    }

    private fun parseContent(doc: Document) {
        val article = doc.selectFirst("div[role=article]") ?: doc.selectFirst("#chapter-$number") ?: doc
        val paragraphs = article.select("p, center")
        text = paragraphs.joinToString("\n") { it.text().replace("\n", "") }
        words = wordCount(text)
        val preface = doc.selectFirst("div.chapter.preface.group")
        val heading = preface?.selectFirst("h3.title")
        title = if (id < 0) work.title else heading?.ownText()?.removePrefix("Chapter ")?.trim()
            ?.takeIf(String::isNotBlank) ?: number.toString()
        summary = paragraphText(doc.selectFirst("#summary"), trim = false)
        startNotes = paragraphText(doc.selectFirst("#notes"), trim = true)
        endNotes = paragraphText(doc.selectFirst("#chapter_${number}_endnotes"), trim = false)
    }

    fun getImages(): Map<Int, List<Pair<String, Int>>> {
        val doc = document ?: throw UnloadedError("Chapter is not loaded")
        val article = doc.selectFirst("div[role=article]") ?: doc
        val images = article.select("p").mapIndexedNotNull { index, paragraph ->
            paragraph.select("img[src]").map { image -> image.attr("abs:src").ifBlank { image.attr("src") } to index + 1 }
        }.flatten()
        return if (images.isEmpty()) emptyMap() else mapOf(number to images)
    }

    suspend fun getComments(maximum: Int?): List<Comment> {
        if (id < 0) return work.getComments(maximum)
        if (!loaded) throw UnloadedError("Chapter is not loaded")
        val firstUrl = "$AO3_ORIGIN/chapters/$id?page=1&show_comments=true&view_adult=true"
        val first = parseHtml(AO3Http.request(context, firstUrl).also { it.requireStatus() }.body)
        val pages = first.select("#comments_placeholder ol.pagination.actions li")
            .mapNotNull { it.text().toIntOrNull() }.maxOrNull() ?: 1
        val result = mutableListOf<Comment>()
        for (page in 1..pages) {
            val doc = if (page == 1) first else parseHtml(
                AO3Http.request(
                    context,
                    "$AO3_ORIGIN/chapters/$id?page=$page&show_comments=true&view_adult=true"
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

    suspend fun comment(
        commentText: String,
        email: String,
        name: String,
        pseud: String?
    ): AO3HttpResponse {
        if (!loaded) throw UnloadedError("Chapter is not loaded")
        return AO3Actions.comment(
            this,
            commentText,
            session,
            id < 0,
            null,
            email,
            name,
            pseud
        )
    }

    fun setSession(session: Session) {
        currentSession = session
    }

    override fun toString(): String = title.ifBlank { "AO3 item $id" }

    companion object {
        suspend fun load(
            context: Context,
            id: Long,
            work: Work,
            session: Session = work.session
        ): Chapter = Chapter(context, id, work, session).also { it.reload() }
    }
}

private fun paragraphText(element: Element?, trim: Boolean): String =
    element?.select("p")?.map { if (trim) it.text().trim() else it.text() }
        ?.joinToString(separator = "\n", postfix = if (element.select("p").isEmpty()) "" else "\n")
        .orEmpty()
