package io.github.dot166.libotwarchive

import android.content.Context
import kotlin.math.ceil

const val DEFAULT = "_score"
const val BEST_MATCH = "_score"
const val AUTHOR = "authors_to_sort_on"
const val TITLE = "title_to_sort_on"
const val DATE_POSTED = "created_at"
const val DATE_UPDATED = "revised_at"
const val WORD_COUNT = "word_count"
const val RATING = "rating_ids"
const val HITS = "hits"
const val BOOKMARKS = "bookmarks_count"
const val COMMENTS = "comments_count"
const val KUDOS = "kudos_count"
const val DESCENDING = "desc"
const val ASCENDING = "asc"

data class WorkSearch(
    val anyField: String = "",
    val title: String = "",
    val author: String = "",
    val singleChapter: Boolean = false,
    val wordCount: Constraint? = null,
    val language: String = "",
    val fandoms: String = "",
    val rating: Int? = null,
    val hits: Constraint? = null,
    val kudos: Constraint? = null,
    val crossovers: Boolean? = null,
    val bookmarks: Constraint? = null,
    val excludedTags: String = "",
    val comments: Constraint? = null,
    val completionStatus: Boolean? = null,
    val page: Int = 1,
    val sortColumn: String = "",
    val sortDirection: String = "",
    val revisedAt: String = "",
    val characters: String = "",
    val relationships: String = "",
    val tags: String = ""
)

class Search(
    private val context: Context,
    val query: WorkSearch = WorkSearch(),
    private val session: Session = GuestSession(context)
) {
    var results: List<Work>? = null
        private set
    var pages: Int = 0
        private set
    var totalResults: Int = 0
        private set

    suspend fun update() {
        require(query.page > 0) { "Search page must be greater than zero" }
        val params = Query().apply {
            addField("work_search[query]", query.anyField.ifEmpty { " " })
            if (query.page != 1) addField("page", query.page)
            if (query.title.isNotEmpty()) addField("work_search[title]", query.title)
            if (query.author.isNotEmpty()) addField("work_search[creators]", query.author)
            if (query.singleChapter) addField("work_search[single_chapter]", 1)
            query.wordCount?.let { addField("work_search[word_count]", it.string) }
            if (query.language.isNotEmpty()) addField("work_search[language_id]", query.language)
            if (query.fandoms.isNotEmpty()) addField("work_search[fandom_names]", query.fandoms)
            if (query.characters.isNotEmpty()) addField("work_search[character_names]", query.characters)
            if (query.relationships.isNotEmpty()) addField("work_search[relationship_names]", query.relationships)
            if (query.tags.isNotEmpty()) addField("work_search[freeform_names]", query.tags)
            query.rating?.let { addField("work_search[rating_ids]", it) }
            query.hits?.let { addField("work_search[hits]", it.string) }
            query.kudos?.let { addField("work_search[kudos_count]", it.string) }
            query.crossovers?.let { addField("work_search[crossover]", if (it) "T" else "F") }
            query.bookmarks?.let { addField("work_search[bookmarks_count]", it.string) }
            if (query.excludedTags.isNotEmpty()) addField("work_search[excluded_tag_names]", query.excludedTags)
            query.comments?.let { addField("work_search[comments_count]", it.string) }
            query.completionStatus?.let { addField("work_search[complete]", if (it) "T" else "F") }
            if (query.sortColumn.isNotEmpty()) addField("work_search[sort_column]", query.sortColumn)
            if (query.sortDirection.isNotEmpty()) addField("work_search[sort_direction]", query.sortDirection)
            if (query.revisedAt.isNotEmpty()) addField("work_search[revised_at]", query.revisedAt)
        }
        val response = AO3Http.request(
            context,
            "https://archiveofourown.org/works/search?${params.string}"
        )
        response.requireNotCloudflare()
        response.requireStatus()
        val doc = parseHtml(response.body)
        if (doc.selectFirst("p")?.text()?.contains("No results found", true) == true) {
            results = emptyList()
            totalResults = 0
            pages = 0
            return
        }
        val list = doc.selectFirst("ol.work.index.group")
            ?: throw UnexpectedResponseError("AO3 search results were not present in the response")
        results = list.select("li[role=article]").mapNotNull { item ->
            val link = item.selectFirst("h4 a[href^=/works/]") ?: return@mapNotNull null
            if (workIdFromUrl(link.attr("href")) == null) return@mapNotNull null
            getWorkFromBanner(context, item, session)
        }
        val countText = doc.selectFirst("#main.works-search h3.heading")?.text()
            ?.replace(",", "")?.replace(".", "")
        totalResults = Regex("\\d+").find(countText.orEmpty())?.value?.toIntOrNull() ?: results.orEmpty().size
        pages = if (totalResults == 0) 0 else ceil(totalResults / 20.0).toInt()
    }

    companion object {
        suspend fun search(
            context: Context,
            query: WorkSearch = WorkSearch(),
            session: Session = GuestSession(context)
        ): Search = Search(context.applicationContext, query, session).also { it.update() }
    }
}
