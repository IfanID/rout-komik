package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaMergeRepository
import tachiyomi.domain.manga.repository.MangaRepository

class NetworkToLocalManga(
    private val mangaRepository: MangaRepository,
    private val mangaMergeRepository: MangaMergeRepository,
) {

    /**
     * @param updateInfo Default: `true`. If `true`, it will update the manga's information in the database if it already exists.
     * If `false`, it will only insert the manga if it doesn't exist in the database. This is used to avoid the case when fetching
     * related mangas from within `MangaScreenModel` would overwrite current manga's `getDetails` info with info from browsing.
     */
    suspend operator fun invoke(
        manga: Manga,
        // KMK -->
        updateInfo: Boolean = true,
        // KMK <--
    ): Manga {
        return invoke(
            listOf(manga),
            // KMK -->
            updateInfo,
            // KMK <--
        ).single()
    }

    /**
     * @param updateInfo Default: `true`. If `true`, it will update the manga's information in the database if it already exists.
     * If `false`, it will only insert the manga if it doesn't exist in the database. This is used to avoid the case when fetching
     * related mangas from within `MangaScreenModel` would overwrite current manga's `getDetails` info with info from browsing.
     */
    suspend operator fun invoke(
        manga: List<Manga>,
        // KMK -->
        updateInfo: Boolean = true,
        // KMK <--
    ): List<Manga> {
        val inserted = mangaRepository.insertNetworkManga(
            manga,
            // KMK -->
            updateInfo,
            // KMK <--
        )
        // SY -->
        return inserted.map { m ->
            if (m.favorite) return@map m
            val mergeId = mangaMergeRepository.getReferencesByMangaId(m.id).firstOrNull()?.mergeId
            val mergeManga = mergeId?.let { mangaRepository.getMangaById(it) }
            if (mergeManga?.favorite == true) {
                m.copy(favorite = true)
            } else {
                m
            }
        }
        // SY <--
    }
}
