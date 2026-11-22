package io.github.dot166.libotwarchive

import android.content.Context

class AO3(private val context: Context) {
    val utils = AO3Utils(context)
    val extra = AO3Extra(context)

    suspend fun getWork(
        workId: Long,
        session: Session = GuestSession(context),
        loadChapters: Boolean = true
    ): Work = Work.load(context, workId, session, loadChapters)

    suspend fun getChapter(
        chapterId: Long,
        workId: Long,
        session: Session = GuestSession(context)
    ): Chapter = Chapter.load(context, chapterId, Work.load(context, workId, session), session)

    suspend fun getUser(username: String, session: Session = GuestSession(context)): User =
        User.load(context, username, session)

    suspend fun getSeries(seriesId: Long, session: Session = GuestSession(context)): Series =
        Series.load(context, seriesId, session)

    suspend fun login(username: String, password: String): LoggedInSession =
        LoggedInSession.login(context, username, password)

    suspend fun search(query: WorkSearch = WorkSearch(), session: Session = GuestSession(context)): Search =
        Search.search(context, query, session)

    suspend fun downloadResource(resource: String) = AO3Resources.download(context, resource)
    suspend fun downloadAllResources(redownload: Boolean = false) =
        AO3Resources.downloadAll(context, redownload)

    fun getWorkIdFromUrl(url: String): Long? = workIdFromUrl(url)

    override fun toString(): String = "AO3"
}
