package io.github.dot166.libotwarchive

import android.content.Context
import org.jsoup.nodes.Document
import java.time.LocalDate

class Series(
    private val context: Context,
    val id: Long,
    session: Session = GuestSession(context)
) {
    private var currentSession = session
    val session: Session get() = currentSession
    private var document: Document? = null
    var name: String = ""
        internal set
    val loaded: Boolean get() = document != null
    val url: String get() = "$AO3_ORIGIN/series/$id"
    val authenticityToken: String?
        get() = document?.selectFirst("meta[name=csrf-token]")?.attr("content")
    val creators: List<User>
        get() = document?.select("dl.series.meta.group a[rel=author]")
            ?.map { User(context, it.text(), session) } ?: emptyList()
    val seriesBegun: LocalDate?
        get() = labelledValue("Series Begun:")?.toLocalDateOrNull()
    val seriesUpdated: LocalDate?
        get() = labelledValue("Series Updated:")?.toLocalDateOrNull()
    val words: Int get() = labelledValue("Words:", stats = true)?.toIntCount() ?: 0
    val nworks: Int get() = labelledValue("Works:", stats = true)?.toIntCount() ?: 0
    val complete: Boolean get() = labelledValue("Complete:", stats = true) == "Yes"
    val description: String get() = labelledValue("Description:").orEmpty()
    val notes: String get() = labelledValue("Notes:").orEmpty()
    val nbookmarks: Int get() = labelledValue("Bookmarks:", stats = true)?.toIntCount() ?: 0
    val workList: List<Work>
        get() = document?.select("ul.series.work.index.group li[role=article]")?.mapNotNull { item ->
            val link = item.selectFirst("h4 a[href^=/works/]") ?: return@mapNotNull null
            if (workIdFromUrl(link.attr("href")) == null) return@mapNotNull null
            getWorkFromBanner(context, item, session)
        } ?: emptyList()
    val isSubscribed: Boolean
        get() {
            requireAuthed()
            return document?.selectFirst("form[data-create-value=Subscribe] input[name=commit][value=Unsubscribe]") != null
        }

    init {
        require(id > 0) { "series id must be positive" }
    }

    suspend fun reload() {
        val response = AO3Http.request(context, url)
        response.requireStatus()
        val parsed = parseHtml(response.body)
        if (parsed.title().contains("Error 404", true)) throw InvalidIdError("Cannot find series $id")
        document = parsed
        name = parsed.selectFirst("div.series-show.region h2")?.text()?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
    }

    suspend fun subscribe() = AO3Actions.subscribe(this, "Series", session)
    suspend fun unsubscribe() {
        val subscriptionId = document?.selectFirst("form[data-create-value=Subscribe][action]")?.attr("action")
            ?.substringAfterLast('/')?.toLongOrNull() ?: throw AuthError("You are not subscribed to this series")
        AO3Actions.subscribe(this, "Series", session, unsubscribe = true, subscriptionId = subscriptionId)
    }
    suspend fun bookmark(
        notes: String = "",
        tags: List<String> = emptyList(),
        collections: List<String> = emptyList(),
        private: Boolean = false,
        recommend: Boolean = false,
        pseud: String? = null
    ) = AO3Actions.bookmark(this, session, notes, tags, collections, private, recommend, pseud)
    suspend fun deleteBookmark() {
        val bookmarkId = document?.selectFirst("#bookmark-form form[action]")?.attr("action")
            ?.substringAfterLast('/')?.toLongOrNull() ?: throw BookmarkError("No bookmark was found")
        AO3Actions.deleteBookmark(bookmarkId, session, authenticityToken)
    }

    private fun labelledValue(label: String, stats: Boolean = false): String? {
        val selector = if (stats) "dl.series.meta.group dl.stats" else "dl.series.meta.group"
        val container = document?.selectFirst(selector) ?: return null
        val dt = container.select("dt").firstOrNull { it.text().trim() == label } ?: return null
        return dt.nextElementSibling()?.text()?.trim()
    }

    private fun requireAuthed() {
        if (!session.isAuthed) throw AuthError("This operation requires an authenticated session")
    }

    fun setSession(session: Session) {
        currentSession = session
    }

    companion object {
        suspend fun load(context: Context, id: Long, session: Session = GuestSession(context)): Series =
            Series(context.applicationContext, id, session).also { it.reload() }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
private fun String.toIntCount(): Int = replace(",", "").toIntOrNull() ?: 0
