package tachiyomi.domain.chapter.interactor

import exh.source.MERGED_SOURCE_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.manga.interactor.GetMergedReferencesById
import tachiyomi.domain.manga.model.MergedMangaReference

class GetMergedChaptersByMangaId(
    private val chapterRepository: ChapterRepository,
    private val getMergedReferencesById: GetMergedReferencesById,
) {

    suspend fun await(
        mangaId: Long,
        dedupe: Boolean = true,
        /** Filter excluded scanlators & bookmarked/unbookmarked filter */
        applyFilter: Boolean = false,
    ): List<Chapter> {
        return transformMergedChapters(
            getMergedReferencesById.await(mangaId),
            getFromDatabase(mangaId, applyFilter),
            dedupe,
        )
    }

    suspend fun subscribe(
        mangaId: Long,
        dedupe: Boolean = true,
        /** Filter excluded scanlators & bookmarked/unbookmarked filter */
        applyFilter: Boolean = false,
    ): Flow<List<Chapter>> {
        return try {
            chapterRepository.getMergedChapterByMangaIdAsFlow(mangaId, applyFilter)
                .combine(getMergedReferencesById.subscribe(mangaId)) { chapters, references ->
                    transformMergedChapters(references, chapters, dedupe)
                }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            flowOf(emptyList())
        }
    }

    private suspend fun getFromDatabase(
        mangaId: Long,
        /** Filter excluded scanlators & bookmarked/unbookmarked filter */
        applyFilter: Boolean = false,
    ): List<Chapter> {
        return try {
            chapterRepository.getMergedChapterByMangaId(mangaId, applyFilter)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    private fun transformMergedChapters(
        mangaReferences: List<MergedMangaReference>,
        chapterList: List<Chapter>,
        dedupe: Boolean,
    ): List<Chapter> {
        return if (dedupe) dedupeChapterList(mangaReferences, chapterList) else chapterList
    }

    private fun dedupeChapterList(
        mangaReferences: List<MergedMangaReference>,
        chapterList: List<Chapter>,
    ): List<Chapter> {
        return when (mangaReferences.firstOrNull { it.mangaSourceId == MERGED_SOURCE_ID }?.chapterSortMode) {
            MergedMangaReference.CHAPTER_SORT_NONE -> chapterList
            MergedMangaReference.CHAPTER_SORT_PRIORITY -> dedupeByPriority(mangaReferences, chapterList)
            MergedMangaReference.CHAPTER_SORT_MOST_CHAPTERS -> {
                findSourceWithMostChapters(chapterList)?.let { mangaId ->
                    chapterList.filter { it.mangaId == mangaId }
                } ?: chapterList
            }
            MergedMangaReference.CHAPTER_SORT_HIGHEST_CHAPTER_NUMBER -> {
                findSourceWithHighestChapterNumber(chapterList)?.let { mangaId ->
                    chapterList.filter { it.mangaId == mangaId }
                } ?: chapterList
            }
            else -> chapterList
        }
    }

    private fun findSourceWithMostChapters(chapterList: List<Chapter>): Long? {
        return chapterList.groupBy { it.mangaId }.maxByOrNull { it.value.size }?.key
    }

    private fun findSourceWithHighestChapterNumber(chapterList: List<Chapter>): Long? {
        return chapterList.maxByOrNull { it.chapterNumber }?.mangaId
    }

    private fun dedupeByPriority(
        mangaReferences: List<MergedMangaReference>,
        chapterList: List<Chapter>,
    ): List<Chapter> {
        val sortedChapterList = mutableListOf<Chapter>()
        val chapterNumberToIndex = mutableMapOf<Double, Int>()

        chapterList.groupBy { it.mangaId }
            .entries
            .sortedBy { (mangaId) ->
                mangaReferences.find { it.mangaId == mangaId }?.chapterPriority ?: Int.MAX_VALUE
            }
            .forEach { (mangaId, chapters) ->
                var currentPointer = -1
                chapters.forEach { chapter ->
                    if (chapter.isRecognizedNumber) {
                        val existingIndex = chapterNumberToIndex[chapter.chapterNumber]
                        if (existingIndex != null) {
                            val existingChapter = sortedChapterList[existingIndex]
                            if (existingChapter.mangaId != mangaId) {
                                currentPointer = existingIndex
                                return@forEach
                            }
                        }
                    }

                    val insertIndex = currentPointer + 1
                    sortedChapterList.add(insertIndex, chapter)

                    // Shift indices in map for all items at or after the insertion point
                    if (chapterNumberToIndex.isNotEmpty()) {
                        for (entry in chapterNumberToIndex.entries) {
                            if (entry.value >= insertIndex) {
                                entry.setValue(entry.value + 1)
                            }
                        }
                    }

                    if (chapter.isRecognizedNumber) {
                        chapterNumberToIndex[chapter.chapterNumber] = insertIndex
                    }
                    currentPointer = insertIndex
                }
            }

        return sortedChapterList.mapIndexed { index, chapter ->
            chapter.copy(sourceOrder = index.toLong())
        }
    }
}
