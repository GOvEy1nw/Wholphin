package com.github.damontecres.wholphin.util

import com.github.damontecres.wholphin.R
import org.jellyfin.sdk.model.api.CollectionType
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryIconResolverTest {
    @Test
    fun `classifies normalized library names before collection type fallback`() {
        val cases =
            listOf(
                Triple("4K TV", CollectionType.MOVIES, R.string.fa_tv),
                Triple("4k-shows", CollectionType.UNKNOWN, R.string.fa_tv),
                Triple("4K.Series", CollectionType.UNKNOWN, R.string.fa_tv),
                Triple("4K Movies", CollectionType.TVSHOWS, R.string.fa_film),
                Triple("4k_films", CollectionType.UNKNOWN, R.string.fa_film),
                Triple("3D Movies", CollectionType.UNKNOWN, R.string.fa_film),
                Triple("3d-films", CollectionType.UNKNOWN, R.string.fa_film),
                Triple("Reality", CollectionType.MOVIES, R.string.fa_tv),
                Triple("Reality TV", CollectionType.UNKNOWN, R.string.fa_tv),
                Triple("Reality Shows", CollectionType.UNKNOWN, R.string.fa_tv),
                Triple("Documentaries", CollectionType.MOVIES, R.string.fa_file_video),
                Triple("DOCS", CollectionType.UNKNOWN, R.string.fa_file_video),
                Triple("Kids", CollectionType.UNKNOWN, R.string.fa_image),
                Triple("Children", CollectionType.UNKNOWN, R.string.fa_image),
                Triple("Children's", CollectionType.UNKNOWN, R.string.fa_image),
                Triple("Family", CollectionType.UNKNOWN, R.string.fa_image),
                Triple("Concert", CollectionType.UNKNOWN, R.string.fa_music),
                Triple("Concerts", CollectionType.UNKNOWN, R.string.fa_music),
                Triple("Home Video", CollectionType.UNKNOWN, R.string.fa_video),
                Triple("Home--Videos", CollectionType.UNKNOWN, R.string.fa_video),
                Triple("TV", CollectionType.MOVIES, R.string.fa_tv),
                Triple("TV Shows", CollectionType.UNKNOWN, R.string.fa_tv),
                Triple("Shows", CollectionType.UNKNOWN, R.string.fa_tv),
                Triple("Series", CollectionType.UNKNOWN, R.string.fa_tv),
                Triple("Television", CollectionType.UNKNOWN, R.string.fa_tv),
                Triple("Movie", CollectionType.TVSHOWS, R.string.fa_film),
                Triple("Movies", CollectionType.UNKNOWN, R.string.fa_film),
                Triple("Film", CollectionType.UNKNOWN, R.string.fa_film),
                Triple("Films", CollectionType.UNKNOWN, R.string.fa_film),
                Triple("Cinema", CollectionType.UNKNOWN, R.string.fa_film),
            )

        cases.forEach { (name, type, expected) ->
            assertEquals(name, expected, resolveLibraryIcon(name, type))
        }
    }

    @Test
    fun `prioritizes qualifiers and preserves unmatched type fallbacks`() {
        val cases =
            listOf(
                Triple("Movies 4K TV", CollectionType.MOVIES, R.string.fa_tv),
                Triple("TV 3D Movies", CollectionType.UNKNOWN, R.string.fa_film),
                Triple("My Library", CollectionType.MOVIES, R.string.fa_film),
                Triple("My Library", CollectionType.TVSHOWS, R.string.fa_tv),
                Triple("My Library", CollectionType.HOMEVIDEOS, R.string.fa_video),
                Triple("My Library", CollectionType.LIVETV, R.drawable.gf_dvr),
                Triple("My Library", CollectionType.MUSIC, R.string.fa_music),
                Triple("My Library", CollectionType.BOXSETS, R.string.fa_open_folder),
                Triple("My Library", CollectionType.PLAYLISTS, R.string.fa_list_ul),
                Triple("My Library", CollectionType.UNKNOWN, R.string.fa_film),
            )

        cases.forEach { (name, type, expected) ->
            assertEquals(type.name, expected, resolveLibraryIcon(name, type))
        }
    }
}
