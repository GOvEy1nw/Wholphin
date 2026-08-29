package com.github.damontecres.wholphin.services

import com.github.damontecres.wholphin.ui.nav.Destination
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class NavigationManagerTest {
    @Test
    fun repeatedDrawerPreviewsLeaveOnePreviewEntry() {
        val navigationManager = NavigationManager()
        val previews = List(5) { libraryDestinations().first }

        previews.forEach(navigationManager::previewFromDrawer)

        assertEquals(listOf(Destination.Home(), previews.last()), navigationManager.backStack)
        val visiblePreview = navigationManager.backStack.last()
        navigationManager.previewFromDrawer(previews.last().copy())
        assertSame(visiblePreview, navigationManager.backStack.last())
        navigationManager.goBack()
        assertEquals(listOf(Destination.Home()), navigationManager.backStack)
    }

    @Test
    fun homePreviewReturnsToExistingHomeKey() {
        val navigationManager = NavigationManager()
        navigationManager.reloadHome()
        val existingHome = navigationManager.backStack.single()
        navigationManager.previewFromDrawer(Destination.Search())

        navigationManager.previewFromDrawer(Destination.Home())

        assertSame(existingHome, navigationManager.backStack.single())
    }

    @Test
    fun libraryBrowseBackReturnsToMatchingHub() {
        val navigationManager = NavigationManager()
        val (hub, browse) = libraryDestinations()
        navigationManager.previewFromDrawer(hub)

        navigationManager.openLibraryBrowse(hub, browse)
        navigationManager.goBack()

        assertEquals(listOf(Destination.Home(), hub), navigationManager.backStack)
    }

    @Test
    fun explicitHomeReloadAndGenericDrawerNavigationRemainSeparate() {
        val navigationManager = NavigationManager()
        val originalHome = navigationManager.backStack.single()

        navigationManager.navigateToFromDrawer(Destination.Favorites)
        assertEquals(listOf(originalHome, Destination.Favorites), navigationManager.backStack)
        navigationManager.goToHome()
        assertSame(originalHome, navigationManager.backStack.single())

        navigationManager.reloadHome()
        assertNotEquals(originalHome, navigationManager.backStack.single())
    }

    private fun libraryDestinations(): Pair<Destination.LibraryHub, Destination.LibraryBrowse> {
        val itemId = UUID.randomUUID()
        return Pair(
            Destination.LibraryHub(itemId, BaseItemKind.COLLECTION_FOLDER, CollectionType.MOVIES),
            Destination.LibraryBrowse(itemId, BaseItemKind.COLLECTION_FOLDER, CollectionType.MOVIES),
        )
    }
}
