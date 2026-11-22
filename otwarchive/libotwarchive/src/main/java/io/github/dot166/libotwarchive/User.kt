package io.github.dot166.libotwarchive

import android.content.Context
import org.jsoup.nodes.Document

class User(
    private val context: Context,
    val username: String,
    session: Session = GuestSession(context)
) {
    private var currentSession = session
    val session: Session get() = currentSession
    private var profile: Document? = null
    private var worksPage: Document? = null
    private var bookmarksPage: Document? = null
    private var _works: List<Work>? = null
    private var _bookmarks: List<Work>? = null
    var authenticityToken: String? = null
        private set
    val loaded: Boolean get() = profile != null
    val url: String get() = "$AO3_ORIGIN/users/${encode(username)}"
    val id: Long?
        get() = profile?.selectFirst("#subscription_subscribable_id")?.attr("value")?.toLongOrNull()
    val userId: Long
        get() {
            requireAuthed()
            return profile?.selectFirst("input[name='subscription[subscribable_id]']")?.attr("value")
                ?.toLongOrNull() ?: throw UnexpectedResponseError("Couldn't fetch user ID")
        }
    val bio: String
        get() = profile?.selectFirst("div.bio.module blockquote.userstuff")?.text().orEmpty()
    val works: Int
        get() = worksPage?.selectFirst("div.works-index h2")?.text()?.replace(",", "")
            ?.split(Regex("\\s+"))?.getOrNull(4)?.toIntOrNull() ?: 0
    val bookmarks: Int
        get() = bookmarksPage?.selectFirst("div.bookmarks-index h2")?.text()?.replace(",", "")
            ?.split(Regex("\\s+"))?.getOrNull(4)?.toIntOrNull() ?: 0
    val isSubscribed: Boolean
        get() {
            requireAuthed()
            return profile?.selectFirst("div.primary.header.module input[name=commit][value=Unsubscribe]") != null
        }
    val workPages: Int
        get() = worksPage?.let(::paginationPages) ?: 1

    init {
        require(username.isNotBlank()) { "username cannot be blank" }
    }

    suspend fun reload() {
        val name = encode(username)
        val pages = listOf(
            AO3Http.request(context, "$AO3_ORIGIN/users/$name/profile"),
            AO3Http.request(context, "$AO3_ORIGIN/users/$name/works"),
            AO3Http.request(context, "$AO3_ORIGIN/users/$name/bookmarks")
        )
        pages.forEach { it.requireStatus() }
        profile = parseHtml(pages[0].body)
        worksPage = parseHtml(pages[1].body)
        bookmarksPage = parseHtml(pages[2].body)
        authenticityToken = profile?.selectFirst("meta[name=csrf-token]")?.attr("content")
            ?: profile?.selectFirst("input[name=authenticity_token]")?.attr("value")
        if (profile?.selectFirst("#main")?.text()?.contains("not found", true) == true) {
            throw InvalidIdError("Cannot find AO3 user '$username'")
        }
        _works = null
        _bookmarks = null
    }

    suspend fun getWorks(): List<Work> {
        _works?.let { return it }
        val first = worksPage ?: requestPage("$url/works").also { worksPage = it }
        val result = mutableListOf<Work>()
        for (page in 1..paginationPages(first)) {
            val doc = if (page == 1) first else requestPage("$url/works?page=$page")
            doc.select("ol.work.index.group li[role=article]").forEach { item ->
                val link = item.selectFirst("h4 a[href^=/works/]") ?: return@forEach
                if (workIdFromUrl(link.attr("href")) == null) return@forEach
                result += getWorkFromBanner(context, item, session)
            }
        }
        _works = result
        return result
    }

    suspend fun getBookmarks(): List<Work> {
        _bookmarks?.let { return it }
        val first = bookmarksPage ?: requestPage("$url/bookmarks").also { bookmarksPage = it }
        val result = mutableListOf<Work>()
        for (page in 1..paginationPages(first)) {
            val doc = if (page == 1) first else requestPage("$url/bookmarks?page=$page")
            doc.select("ol.bookmark.index.group li[role=article]").forEach { item ->
                val link = item.selectFirst("h4 a[href^=/works/]") ?: return@forEach
                if (workIdFromUrl(link.attr("href")) == null) return@forEach
                result += getWorkFromBanner(context, item, session).apply {
                    recommended = item.selectFirst("span[title=Rec]") != null
                }
            }
        }
        _bookmarks = result
        return result
    }

    suspend fun getAvatar(): Pair<String, ByteArray> {
        val avatar = (profile ?: requestPage("$url/profile")).selectFirst("p.icon img[src]")
            ?: throw UnexpectedResponseError("AO3 user profile has no avatar")
        val imageUrl = avatar.absUrl("src")
        val response = AO3Http.request(context, imageUrl, binaryResponse = true)
        response.requireStatus()
        if (!response.isSuccessful) throw UnexpectedResponseError("Unable to download user avatar")
        return imageUrl.substringBefore('?').substringAfterLast('/') to response.bodyBytes
    }

    suspend fun subscribe() = AO3Actions.subscribe(this, "User", session)
    suspend fun unsubscribe() {
        requireAuthed()
        val id = profile?.selectFirst("div.primary.header.module form[action]")?.attr("action")
            ?.substringAfterLast('/')?.toLongOrNull()
            ?: throw AuthError("You are not subscribed to this user")
        AO3Actions.subscribe(this, "User", session, unsubscribe = true, subscriptionId = id)
    }

    private suspend fun requestPage(url: String): Document =
        parseHtml(AO3Http.request(context, url).also { it.requireStatus() }.body)

    private fun requireAuthed() {
        if (!session.isAuthed) throw AuthError("This operation requires an authenticated session")
    }

    fun setSession(session: Session) {
        currentSession = session
    }

    companion object {
        suspend fun load(context: Context, username: String, session: Session = GuestSession(context)): User =
            User(context.applicationContext, username, session).also { it.reload() }
    }
}
