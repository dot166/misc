package io.github.dot166.libotwarchive

import android.content.Context
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class Comment(
    private val context: Context,
    val id: Long,
    var parent: Any? = null,
    var session: Session = GuestSession(context),
    var authenticityToken: String? = null,
    var author: String? = null,
    var text: String = "",
    var parentComment: Comment? = null
) {
    private var document: Document? = null
    private var threadCache: List<Comment>? = null
    val firstParentComment: Comment
        get() = parentComment?.firstParentComment ?: this
    val fullWork: Boolean
        get() = parent is Work

    suspend fun reload() {
        val response = AO3Http.request(context, "$AO3_ORIGIN/comments/$id")
        response.requireStatus()
        val doc = parseHtml(response.body)
        val item = doc.getElementById("comment_$id")
            ?: throw InvalidIdError("Cannot find comment $id")
        document = doc
        text = item.selectFirst("blockquote")?.text().orEmpty()
        author = item.selectFirst("h4.heading.byline a")?.text()
        authenticityToken = doc.selectFirst("meta[name=csrf-token]")?.attr("content")
        if (parent == null) {
            val replyHref = item.selectFirst("li[id^=add_comment_reply_link_] a[href]")?.attr("href")
            val chapterId = replyHref?.substringAfter("chapter_id=")?.substringBefore('&')?.toLongOrNull()
            val workId = replyHref?.substringAfter("work_id=")?.substringBefore('&')?.toLongOrNull()
            if (chapterId != null) parent = chapterId
            else if (workId != null) parent = Work.load(context, workId, session)
        }
        parentComment = null
        threadCache = null
    }

    suspend fun getThread(): List<Comment> {
        threadCache?.let { return it }
        val doc = document ?: run {
            reload()
            document ?: return emptyList()
        }
        val thread = doc.selectFirst("ol.thread") ?: return emptyList<Comment>().also { threadCache = it }
        val root = mutableListOf<Comment>()
        parseThread(thread, null, root)
        threadCache = root
        return root
    }

    private fun parseThread(container: Element, parent: Comment?, destination: MutableList<Comment>) {
        for (item in container.children().filter { it.tagName() == "li" }) {
            if (item.hasAttr("role")) {
                val commentId = item.id().removePrefix("comment_").toLongOrNull() ?: continue
                val child = Comment(
                    context,
                    commentId,
                    parent = this.parent,
                    session = session,
                    authenticityToken = authenticityToken,
                    author = item.selectFirst("h4.heading.byline a")?.text(),
                    text = item.selectFirst("blockquote")?.text().orEmpty(),
                    parentComment = parent ?: this
                )
                destination += child
                item.selectFirst(":scope > ol.thread")?.let { parseThread(it, child, child.threadCacheOrList()) }
            }
        }
    }

    private fun Comment.threadCacheOrList(): MutableList<Comment> {
        val child = threadCache?.toMutableList() ?: mutableListOf()
        threadCache = child
        return child
    }

    suspend fun getThreadIterator(): List<Comment> {
        val result = mutableListOf<Comment>()
        fun flatten(comment: Comment) {
            result += comment
            comment.threadCache.orEmpty().forEach(::flatten)
        }
        getThread().forEach(::flatten)
        return result
    }

    suspend fun reply(commentText: String, email: String = "", name: String = "") {
        val target = parent ?: throw IllegalArgumentException("Comment parent cannot be null")
        AO3Actions.comment(target, commentText, session, fullWork, id, email, name)
    }

    suspend fun delete() {
        AO3Actions.deleteComment(this, session)
    }
}
