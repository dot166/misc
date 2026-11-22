package io.github.dot166.libotwarchive

import android.content.Context
import org.jsoup.nodes.Element

fun getWorkFromBanner(
    context: Context,
    banner: Element,
    session: Session = GuestSession(context)
): Work {
    val link = banner.selectFirst("h4 a[href^=/works/]")
        ?: throw InvalidIdError("Work banner does not include a work link")
    val workId = workIdFromUrl(link.attr("href"))
        ?: throw InvalidIdError("Work banner includes an invalid work URL")
    return Work(context, workId, session).apply { populateFromBanner(banner) }
}