package com.github.damontecres.wholphin.ui.library

import com.github.damontecres.wholphin.ui.nav.Destination
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class LibraryTypeTest {
    @Test
    fun videoLibraryClassification_matchesCollectionAndItemTypes() {
        listOf(
            Case(CollectionType.MOVIES, BaseItemKind.COLLECTION_FOLDER, false, true),
            Case(CollectionType.TVSHOWS, BaseItemKind.COLLECTION_FOLDER, false, true),
            Case(CollectionType.HOMEVIDEOS, BaseItemKind.COLLECTION_FOLDER, false, true),
            Case(CollectionType.MUSICVIDEOS, BaseItemKind.COLLECTION_FOLDER, false, true),
            Case(CollectionType.MUSIC, BaseItemKind.COLLECTION_FOLDER, false, false),
            Case(CollectionType.PHOTOS, BaseItemKind.COLLECTION_FOLDER, false, false),
            Case(CollectionType.LIVETV, BaseItemKind.COLLECTION_FOLDER, false, false),
            Case(CollectionType.BOXSETS, BaseItemKind.COLLECTION_FOLDER, false, false),
            Case(CollectionType.PLAYLISTS, BaseItemKind.COLLECTION_FOLDER, false, false),
            Case(CollectionType.FOLDERS, BaseItemKind.COLLECTION_FOLDER, false, true),
            Case(CollectionType.UNKNOWN, BaseItemKind.USER_VIEW, false, true),
            Case(CollectionType.MUSICVIDEOS, BaseItemKind.FOLDER, false, false),
            Case(CollectionType.FOLDERS, BaseItemKind.FOLDER, false, false),
            Case(CollectionType.UNKNOWN, BaseItemKind.FOLDER, false, false),
            Case(CollectionType.MOVIES, BaseItemKind.COLLECTION_FOLDER, true, false),
        ).forEach { case ->
            assertEquals(
                case.expected,
                isVideoLibrary(case.type, case.collectionType, case.isRecordingFolder),
            )
        }
    }

    @Test
    fun libraryDestinations_roundTripThroughBackStackSerialization() {
        val itemId = UUID.randomUUID()
        val json = Json { classDiscriminator = "_type" }
        val destinations: List<Destination> =
            listOf(
                Destination.LibraryHub(itemId, BaseItemKind.COLLECTION_FOLDER, CollectionType.MOVIES),
                Destination.LibraryBrowse(itemId, BaseItemKind.USER_VIEW, CollectionType.TVSHOWS),
            )

        destinations.forEach { destination ->
            assertEquals(destination, json.decodeFromString<Destination>(json.encodeToString(destination)))
        }
    }

    private data class Case(
        val collectionType: CollectionType,
        val type: BaseItemKind,
        val isRecordingFolder: Boolean,
        val expected: Boolean,
    )
}
