package com.github.damontecres.wholphin.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.github.damontecres.wholphin.data.filter.DefaultTvFilterOptions
import com.github.damontecres.wholphin.data.model.CollectionFolderFilter
import com.github.damontecres.wholphin.data.model.GetItemsFilter
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.ui.components.CollectionFolderView
import com.github.damontecres.wholphin.ui.components.ViewOptionsPoster
import com.github.damontecres.wholphin.ui.components.baseItemKinds
import com.github.damontecres.wholphin.ui.data.MovieSortOptions
import com.github.damontecres.wholphin.ui.data.SeriesSortOptions
import com.github.damontecres.wholphin.ui.detail.CollectionFolderGeneric
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.preferences.PreferencesViewModel
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType

@Composable
fun LibraryBrowse(
    preferences: UserPreferences,
    destination: Destination.LibraryBrowse,
    modifier: Modifier = Modifier,
    preferencesViewModel: PreferencesViewModel = hiltViewModel(),
) {
    when (destination.collectionType) {
        CollectionType.MOVIES -> {
            CollectionFolderView(
                preferences = preferences,
                onClickItem = { _, item ->
                    preferencesViewModel.navigationManager.navigateTo(item.destination())
                },
                itemId = destination.itemId,
                viewModelKey = "${destination.itemId}_library",
                initialFilter =
                    CollectionFolderFilter(
                        filter = GetItemsFilter(includeItemTypes = listOf(BaseItemKind.MOVIE)),
                    ),
                showTitle = false,
                recursive = true,
                sortOptions = MovieSortOptions,
                defaultViewOptions = ViewOptionsPoster,
                modifier = modifier,
                playEnabled = true,
            )
        }

        CollectionType.TVSHOWS -> {
            CollectionFolderView(
                preferences = preferences,
                onClickItem = { _, item ->
                    preferencesViewModel.navigationManager.navigateTo(item.destination())
                },
                itemId = destination.itemId,
                initialFilter =
                    CollectionFolderFilter(
                        filter = GetItemsFilter(includeItemTypes = listOf(BaseItemKind.SERIES)),
                    ),
                showTitle = false,
                recursive = true,
                sortOptions = SeriesSortOptions,
                filterOptions = DefaultTvFilterOptions,
                defaultViewOptions = ViewOptionsPoster,
                modifier = modifier,
                playEnabled = false,
            )
        }

        else -> {
            CollectionFolderGeneric(
                preferences = preferences,
                itemId = destination.itemId,
                usePosters = destination.type == BaseItemKind.FOLDER,
                recursive = destination.type == BaseItemKind.USER_VIEW,
                playEnabled =
                    destination.collectionType == CollectionType.HOMEVIDEOS ||
                        destination.collectionType == CollectionType.MUSICVIDEOS,
                modifier = modifier,
                filter =
                    CollectionFolderFilter(
                        filter = GetItemsFilter(
                            includeItemTypes =
                                when (destination.collectionType) {
                                    CollectionType.HOMEVIDEOS,
                                    CollectionType.MUSICVIDEOS,
                                    -> destination.collectionType.baseItemKinds

                                    else -> emptyList()
                                },
                        ),
                    ),
            )
        }
    }
}
