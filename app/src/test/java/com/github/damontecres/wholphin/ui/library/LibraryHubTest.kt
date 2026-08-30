package com.github.damontecres.wholphin.ui.library

import com.github.damontecres.wholphin.data.model.HomeRowConfig
import org.jellyfin.sdk.model.api.CollectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.UUID

class LibraryHubTest {
    @Test
    fun genreSelectionIsExplicitAndHomeResetClearsIt() {
        val genre = LibraryHubGenre(UUID.randomUUID(), "Drama")
        val selected = LibraryHubState().withActiveGenre(genre)

        assertSame(genre, selected.activeGenre)
        assertNull(selected.withActiveGenre(null).activeGenre)
    }

    @Test
    fun fallbackRows_keepApprovedGeneralAndTvOrderWithoutStudios() {
        val libraryId = UUID.randomUUID()

        val tvRows = libraryHubFallbackRows(libraryId, CollectionType.TVSHOWS)
        assertEquals(
            listOf(
                HomeRowConfig.Genres(libraryId),
                HomeRowConfig.ContinueWatching(parentId = libraryId),
                HomeRowConfig.RecentlyReleased(libraryId),
                HomeRowConfig.RecentlyAdded(libraryId),
                HomeRowConfig.TopUnwatched(libraryId),
                HomeRowConfig.Suggestions(libraryId),
                HomeRowConfig.Collections(libraryId),
            ),
            libraryHubFallbackRows(libraryId, CollectionType.MOVIES),
        )
        assertEquals(
            listOf(
                HomeRowConfig.Genres(libraryId),
                HomeRowConfig.ContinueWatching(parentId = libraryId),
                HomeRowConfig.NextUp(parentId = libraryId),
                HomeRowConfig.RecentlyReleased(libraryId),
                HomeRowConfig.RecentlyAdded(libraryId),
                HomeRowConfig.TopUnwatched(libraryId),
                HomeRowConfig.Suggestions(libraryId),
                HomeRowConfig.Collections(libraryId),
            ),
            tvRows,
        )
        assertFalse(tvRows.any { it is HomeRowConfig.Studios })
    }
}
