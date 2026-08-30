package com.github.damontecres.wholphin.util

import com.github.damontecres.wholphin.R
import org.jellyfin.sdk.model.api.CollectionType
import java.util.Locale

private val LIBRARY_NAME_SEPARATOR = Regex("[^\\p{L}\\p{N}]+")
private val LIBRARY_NAME_WHITESPACE = Regex("\\s+")

private val TV_NAMES = setOf("tv", "show", "shows", "series", "television")
private val MOVIE_NAMES = setOf("movie", "movies", "film", "films", "cinema")
private val DOCUMENTARY_NAMES = setOf("doc", "docs", "documentary", "documentaries")
private val FAMILY_NAMES = setOf("kid", "kids", "child", "children", "childrens", "family")
private val CONCERT_NAMES = setOf("concert", "concerts")
private val VIDEO_NAMES = setOf("video", "videos")

internal fun resolveLibraryIcon(
    libraryName: String,
    collectionType: CollectionType,
): Int {
    val tokens =
        libraryName
            .lowercase(Locale.ROOT)
            .replace(LIBRARY_NAME_SEPARATOR, " ")
            .trim()
            .split(LIBRARY_NAME_WHITESPACE)
            .filter(String::isNotEmpty)
            .toSet()
    fun containsAny(names: Set<String>) = tokens.any(names::contains)

    return when {
        "4k" in tokens && containsAny(TV_NAMES) -> R.string.fa_tv
        "4k" in tokens && containsAny(MOVIE_NAMES) -> R.string.fa_film
        "3d" in tokens && containsAny(MOVIE_NAMES) -> R.string.fa_film
        "reality" in tokens -> R.string.fa_tv
        containsAny(DOCUMENTARY_NAMES) -> R.string.fa_file_video
        containsAny(FAMILY_NAMES) -> R.string.fa_image
        containsAny(CONCERT_NAMES) -> R.string.fa_music
        "home" in tokens && containsAny(VIDEO_NAMES) -> R.string.fa_video
        containsAny(TV_NAMES) -> R.string.fa_tv
        containsAny(MOVIE_NAMES) -> R.string.fa_film
        else ->
            when (collectionType) {
                CollectionType.MOVIES -> R.string.fa_film
                CollectionType.TVSHOWS -> R.string.fa_tv
                CollectionType.HOMEVIDEOS -> R.string.fa_video
                CollectionType.LIVETV -> R.drawable.gf_dvr
                CollectionType.MUSIC -> R.string.fa_music
                CollectionType.BOXSETS -> R.string.fa_open_folder
                CollectionType.PLAYLISTS -> R.string.fa_list_ul
                else -> R.string.fa_film
            }
    }
}
