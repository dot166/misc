package io.github.dot166.otwarchive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.dot166.libotwarchive.AO3
import io.github.dot166.libotwarchive.Chapter
import io.github.dot166.libotwarchive.CloudFailDetectedException
import io.github.dot166.libotwarchive.Search
import io.github.dot166.libotwarchive.Work
import io.github.dot166.libotwarchive.WorkSearch
import kotlinx.coroutines.flow.MutableStateFlow

class MainViewModel(ctx: Application) : AndroidViewModel(ctx) {
    private val workCache: MutableMap<Long, Work> = mutableMapOf()
    val currentChapter = MutableStateFlow<Chapter?>(null)
    private val ao3 = AO3(ctx)
    val isBlocked = MutableStateFlow(false)

    suspend fun loadWork(workId: Long): Work? {
        val cached = workCache[workId]
        if (cached != null && cached.loaded && (System.currentTimeMillis() - cached.lastReloaded) < 1000 * 60 * 60 * 24) { // cache for a day, if android doesn't kill it before then
            return cached
        }
        val workOrError = try {
            ao3.getWork(workId)
        } catch (_: CloudFailDetectedException) {
            isBlocked.value = true
            null
        }
        if (!isBlocked.value) {
            workCache[workId] = workOrError as Work
        }
        return workOrError
    }

    suspend fun search(query: WorkSearch = WorkSearch()): Search? = try {
        ao3.search(query)
    } catch (_: CloudFailDetectedException) {
        isBlocked.value = true
        null
    }

    fun dismissChallenge() {
        isBlocked.value = false
    }

    fun previousChapter() {
        val currChapter = currentChapter.value
        if (currChapter != null) {
            val prevChapter = currChapter.work.chapters.find { it.number == currChapter.number - 1 }
            if (prevChapter != null) {
                currentChapter.value = prevChapter
            }
        }
    }

    fun nextChapter() {
        val currChapter = currentChapter.value
        if (currChapter != null) {
            val nextChapter = currChapter.work.chapters.find { it.number == currChapter.number + 1 }
            if (nextChapter != null) {
                currentChapter.value = nextChapter
            }
        }
    }
}