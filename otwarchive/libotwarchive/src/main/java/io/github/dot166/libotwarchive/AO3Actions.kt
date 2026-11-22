package io.github.dot166.libotwarchive

object AO3Actions {
    suspend fun comment(
        commentable: Any,
        commentText: String,
        session: Session,
        fullWork: Boolean = false,
        commentId: Long? = null,
        email: String = "",
        name: String = "",
        pseud: String? = null
    ): AO3HttpResponse {
        require(commentText.length in 1..10_000) { "Comment must contain between 1 and 10,000 characters" }
        val context = session.context
        val token = token(commentable) ?: session.authenticityToken
            ?: throw AuthError("No authenticity token available; refresh the session token")
        val id = id(commentable)
        val full = fullWork || commentable is Work
        val form = linkedMapOf<String, Any?>(
            (if (full) "work_id" else "chapter_id") to id,
            "authenticity_token" to token,
            "comment[comment_content]" to commentText
        )
        if (commentId != null) form["comment_id"] = commentId
        if (session.isAuthed) {
            val pseudId = pseudId(commentable, session, pseud)
                ?: throw PseudError("Couldn't find a valid pseudonym")
            form["comment[pseud_id]"] = pseudId
        } else {
            require(email.isNotBlank() && name.isNotBlank()) { "Both email and name are required for guest comments" }
            form["comment[email]"] = email
            form["comment[name]"] = name
        }
        val pageUrl = if (full) "$AO3_ORIGIN/works/$id" else "$AO3_ORIGIN/chapters/$id"
        val response = session.post(
            "$AO3_ORIGIN/comments.js",
            form,
            mapOf(
                "X-Requested-With" to "XMLHttpRequest",
                "X-CSRF-Token" to token,
                "Referer" to pageUrl
            )
        )
        when (response.statusCode) {
            404 -> if (response.bodyBytes.isEmpty()) throw InvalidIdError("Invalid work or chapter ID")
            200 -> throw DuplicateCommentError("You have already left this comment here")
            422 -> {
                if (response.body.contains("auth_error")) throw AuthError("Invalid token; refresh the session token")
                throw UnexpectedResponseError("AO3 rejected the comment: ${response.body.take(500)}")
            }
            in 200..299 -> Unit
            else -> throw UnexpectedResponseError("Unexpected response posting comment (HTTP ${response.statusCode})")
        }
        return response
    }

    suspend fun deleteComment(comment: Comment, session: Session) {
        requireAuthed(session)
        val token = comment.authenticityToken ?: session.authenticityToken
            ?: throw AuthError("No authenticity token available")
        val response = session.post(
            "$AO3_ORIGIN/comments/${comment.id}",
            mapOf("authenticity_token" to token, "_method" to "delete")
        )
        val doc = parseHtml(response.body)
        if (doc.title().contains("auth error", true) || response.isAuthenticationRedirect()) {
            throw AuthError("Invalid authenticity token")
        }
        if (doc.selectFirst("#main")?.text()?.contains("you don't have permission", true) == true) {
            throw SecurityException("You don't have permission to delete this comment")
        }
        if (response.statusCode !in 200..399) {
            throw UnexpectedResponseError("Unexpected response deleting comment (HTTP ${response.statusCode})")
        }
    }

    suspend fun kudos(work: Work, session: Session): Boolean {
        val token = work.authenticityToken ?: session.authenticityToken
            ?: throw AuthError("No authenticity token available")
        val response = session.post(
            "$AO3_ORIGIN/kudos.js",
            mapOf(
                "authenticity_token" to token,
                "kudo[commentable_id]" to work.id,
                "kudo[commentable_type]" to "Work"
            ),
            mapOf(
                "X-CSRF-Token" to token,
                "X-Requested-With" to "XMLHttpRequest",
                "Referer" to work.url
            )
        )
        return when (response.statusCode) {
            201 -> true
            422 -> {
                val errors = response.body
                when {
                    "auth_error" in errors -> throw AuthError("Invalid authenticity token")
                    "no_commentable" in errors -> throw InvalidIdError("Invalid work ID")
                    "user_id" in errors || "ip_address" in errors -> false
                    else -> throw UnexpectedResponseError("AO3 rejected kudos: ${errors.take(500)}")
                }
            }
            else -> throw UnexpectedResponseError("Unexpected response leaving kudos (HTTP ${response.statusCode})")
        }
    }

    suspend fun subscribe(
        subscribable: Any,
        type: String,
        session: Session,
        unsubscribe: Boolean = false,
        subscriptionId: Long? = null
    ): AO3HttpResponse {
        requireAuthed(session)
        val id = id(subscribable)
        val token = token(subscribable) ?: session.authenticityToken
            ?: throw AuthError("No authenticity token available")
        val url = buildString {
            append("$AO3_ORIGIN/users/${encode(session.username)}/subscriptions")
            if (unsubscribe) {
                append("/")
                append(subscriptionId ?: throw InvalidIdError("Subscription ID is required to unsubscribe"))
            }
        }
        val fields = mutableMapOf<String, Any?>(
            "authenticity_token" to token,
            "subscription[subscribable_id]" to id,
            "subscription[subscribable_type]" to type
        )
        if (unsubscribe) fields["_method"] = "delete"
        val response = session.post(url, fields)
        if (response.isAuthenticationRedirect()) {
            throw AuthError("Invalid authenticity token")
        }
        if (!response.redirected && response.statusCode in 200..299) {
            throw InvalidIdError("AO3 did not accept the $type subscription request")
        }
        if (response.statusCode !in 200..399) {
            throw InvalidIdError("Unable to ${if (unsubscribe) "unsubscribe" else "subscribe"} to $type (HTTP ${response.statusCode})")
        }
        return response
    }

    suspend fun bookmark(
        bookmarkable: Any,
        session: Session,
        notes: String = "",
        tags: List<String> = emptyList(),
        collections: List<String> = emptyList(),
        private: Boolean = false,
        recommend: Boolean = false,
        pseud: String? = null
    ): AO3HttpResponse {
        requireAuthed(session)
        val token = token(bookmarkable) ?: session.authenticityToken
            ?: throw AuthError("No authenticity token available")
        val pseudId = pseudId(bookmarkable, session, pseud)
            ?: throw PseudError("Couldn't find a valid pseudonym")
        val fields = mutableMapOf<String, Any?>(
            "authenticity_token" to token,
            "bookmark[pseud_id]" to pseudId,
            "bookmark[tag_string]" to tags.joinToString(","),
            "bookmark[collection_names]" to collections.joinToString(","),
            "bookmark[private]" to if (private) 1 else 0,
            "bookmark[rec]" to if (recommend) 1 else 0,
            "commit" to "Create"
        )
        if (notes.isNotEmpty()) fields["bookmark[bookmarker_notes]"] = notes
        val response = session.post("${url(bookmarkable).trimEnd('/')}/bookmarks", fields)
        handleBookmarkResponse(response)
        return response
    }

    suspend fun deleteBookmark(id: Long, session: Session, authToken: String? = null): AO3HttpResponse {
        requireAuthed(session)
        val response = session.post(
            "$AO3_ORIGIN/bookmarks/$id",
            mapOf(
                "authenticity_token" to (authToken ?: session.authenticityToken
                    ?: throw AuthError("No authenticity token available")),
                "_method" to "delete"
            )
        )
        handleBookmarkResponse(response)
        return response
    }

    suspend fun collect(collectable: Any, session: Session, collections: List<String>): AO3HttpResponse {
        requireAuthed(session)
        val response = session.post(
            "${url(collectable).trimEnd('/')}/collection_items",
            mapOf(
                "authenticity_token" to (token(collectable) ?: session.authenticityToken
                    ?: throw AuthError("No authenticity token available")),
                "collection_names" to collections.joinToString(","),
                "commit" to "Add"
            )
        )
        if (response.isAuthenticationRedirect()) throw AuthError("Invalid authenticity token")
        val doc = parseHtml(response.body)
        val error = doc.selectFirst("div.error")
        if (error != null) throw CollectError(error.text().ifBlank { "Unable to add to the selected collections" })
        if (response.statusCode !in 200..399) {
            throw UnexpectedResponseError("Unexpected response adding collection (HTTP ${response.statusCode})")
        }
        return response
    }

    private suspend fun pseudId(target: Any, session: Session, specified: String?): String? {
        if (!session.isAuthed) throw AuthError("A logged-in session is required")
        val page = session.request(url(target))
        val direct = page.selectFirst("input[name*=pseud_id]")
        if (direct != null) return direct.attr("value").takeIf(String::isNotBlank)
        val options = page.select("select[name*=pseud_id] option")
        return if (specified == null) {
            options.firstOrNull { it.hasAttr("selected") }?.attr("value")
        } else {
            options.firstOrNull { it.text() == specified }?.attr("value")
        }
    }

    private fun token(target: Any): String? = when (target) {
        is Work -> target.authenticityToken
        is Chapter -> target.authenticityToken
        is Series -> target.authenticityToken
        is User -> target.authenticityToken
        is Comment -> target.authenticityToken
        else -> null
    }

    private fun id(target: Any): Long = when (target) {
        is Work -> target.id
        is Chapter -> if (target.id < 0) target.work.id else target.id
        is Series -> target.id
        is User -> target.id ?: throw InvalidIdError("Cannot determine user ID")
        is Comment -> target.id
        is Long -> target
        else -> throw IllegalArgumentException("Unsupported AO3 object: ${target::class.simpleName}")
    }

    private fun url(target: Any): String = when (target) {
        is Work -> target.url
        is Chapter -> target.url
        is Series -> target.url
        is User -> target.url
        is Long -> "$AO3_ORIGIN/chapters/$target"
        else -> throw IllegalArgumentException("Unsupported AO3 object: ${target::class.simpleName}")
    }

    private fun requireAuthed(session: Session) {
        if (!session.isAuthed) throw AuthError("An authenticated session is required")
    }

    private fun handleBookmarkResponse(response: AO3HttpResponse) {
        response.requireStatus()
        if (response.isAuthenticationRedirect()) {
            throw AuthError("Invalid authenticity token")
        }
        if (response.statusCode == 200 && !response.redirected) {
            val error = parseHtml(response.body).selectFirst("#error.error")
                ?: throw UnexpectedResponseError("AO3 returned an unexpected bookmark response")
            throw BookmarkError(error.select("li").joinToString(" ") { it.text() }.ifBlank { error.text() })
        }
        if (response.statusCode !in 200..399) {
            throw UnexpectedResponseError("Unexpected response saving bookmark (HTTP ${response.statusCode})")
        }
    }

    private fun AO3HttpResponse.isAuthenticationRedirect(): Boolean =
        url.contains("/auth_error", ignoreCase = true) ||
            url.contains("/users/login", ignoreCase = true) ||
            body.contains("auth_error", ignoreCase = true) ||
            parseHtml(body).selectFirst("form#new_user") != null
}
