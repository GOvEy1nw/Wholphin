package com.github.damontecres.wholphin.ui.library

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.wholphin.data.ServerRepository
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.HomeRowConfig
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.services.BackdropService
import com.github.damontecres.wholphin.services.FavoriteWatchManager
import com.github.damontecres.wholphin.services.HomeSettingsService
import com.github.damontecres.wholphin.services.MediaManagementService
import com.github.damontecres.wholphin.services.MediaReportService
import com.github.damontecres.wholphin.services.NavDrawerService
import com.github.damontecres.wholphin.services.NavigationManager
import com.github.damontecres.wholphin.services.UserPreferencesService
import com.github.damontecres.wholphin.services.deleteItem
import com.github.damontecres.wholphin.services.tvAccess
import com.github.damontecres.wholphin.ui.LocalContentTakeFocus
import com.github.damontecres.wholphin.ui.components.ContextMenuProvider
import com.github.damontecres.wholphin.ui.components.ErrorMessage
import com.github.damontecres.wholphin.ui.components.HeaderUtils
import com.github.damontecres.wholphin.ui.components.LoadingPage
import com.github.damontecres.wholphin.ui.components.rememberContextMenu
import com.github.damontecres.wholphin.ui.data.RowColumn
import com.github.damontecres.wholphin.ui.launchIO
import com.github.damontecres.wholphin.ui.main.HomePageContent
import com.github.damontecres.wholphin.ui.main.HomePageHeader
import com.github.damontecres.wholphin.ui.main.isContinueWatchingNextUp
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.util.EmptyStringProvider
import com.github.damontecres.wholphin.util.ExceptionHandler
import com.github.damontecres.wholphin.util.HomeRowLoadingState
import com.github.damontecres.wholphin.util.LoadingState
import com.github.damontecres.wholphin.util.WholphinDispatchers
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jellyfin.sdk.api.client.exception.InvalidStatusException
import org.jellyfin.sdk.model.api.CollectionType
import timber.log.Timber
import java.util.UUID

@HiltViewModel(assistedFactory = LibraryHubViewModel.Factory::class)
class LibraryHubViewModel
    @AssistedInject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val serverRepository: ServerRepository,
        private val navDrawerService: NavDrawerService,
        private val homeSettingsService: HomeSettingsService,
        private val userPreferencesService: UserPreferencesService,
        private val favoriteWatchManager: FavoriteWatchManager,
        private val backdropService: BackdropService,
        private val mediaManagementService: MediaManagementService,
        val mediaReportService: MediaReportService,
        val navigationManager: NavigationManager,
        @Assisted private val destination: Destination.LibraryHub,
    ) : ViewModel(), ContextMenuProvider {
        @AssistedFactory
        interface Factory {
            fun create(destination: Destination.LibraryHub): LibraryHubViewModel
        }

        private val _state = MutableStateFlow(LibraryHubState())
        val state: StateFlow<LibraryHubState> = _state

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launchIO {
                val previous = state.value
                val isRefresh = previous.loadingState == LoadingState.Success
                try {
                    val userDto = serverRepository.currentUserDto ?: return@launchIO
                    val libraries =
                        navDrawerService.state.value.allLibraries.ifEmpty {
                            navDrawerService.getAllUserLibraries(userDto.id, userDto.tvAccess)
                        }
                    val library = libraries.firstOrNull { it.itemId == destination.itemId }
                    if (library == null) {
                        _state.update {
                            it.copy(
                                loadingState = LoadingState.Error(IllegalStateException("Library is unavailable")),
                                refreshState = LoadingState.Error(IllegalStateException("Library is unavailable")),
                            )
                        }
                        return@launchIO
                    }

                    val configs = libraryHubFallbackRows(library.itemId, library.collectionType)
                    val genreSlot = configs.filterIsInstance<HomeRowConfig.Genres>().single()
                    val mediaConfigs = configs.filterNot { it is HomeRowConfig.Genres }
                    _state.update {
                        it.copy(
                            library = library,
                            genreSlot = genreSlot,
                            activeRows = List(mediaConfigs.size) { HomeRowLoadingState.Pending(EmptyStringProvider) },
                            focusedItem = null,
                            firstAvailable = null,
                            loadingState = if (isRefresh) LoadingState.Success else LoadingState.Loading,
                            refreshState = LoadingState.Loading,
                        )
                    }

                    val prefs = userPreferencesService.getCurrent().appPreferences.homePagePreferences
                    val semaphore = Semaphore(4)
                    val rows =
                        mediaConfigs
                            .map { row ->
                                viewModelScope.async(WholphinDispatchers.IO) {
                                    semaphore.withPermit {
                                        fetchRow(row, prefs, userDto, libraries, isRefresh)
                                    }
                                }
                            }.awaitAll()
                    val firstAvailable =
                        rows
                            .asSequence()
                            .filterIsInstance<HomeRowLoadingState.Success>()
                            .flatMap { it.items.asSequence() }
                            .firstOrNull()
                    _state.update {
                        it.copy(
                            activeRows = rows,
                            firstAvailable = firstAvailable,
                            loadingState = LoadingState.Success,
                            refreshState = LoadingState.Success,
                        )
                    }
                } catch (ex: Exception) {
                    Timber.e(ex, "Error loading library hub %s", destination.itemId)
                    _state.update {
                        it.copy(
                            loadingState = if (isRefresh) LoadingState.Success else LoadingState.Error(ex),
                            refreshState = LoadingState.Error(ex),
                        )
                    }
                }
            }
        }

        private suspend fun fetchRow(
            row: HomeRowConfig,
            prefs: com.github.damontecres.wholphin.preferences.HomePagePreferences,
            userDto: org.jellyfin.sdk.model.api.UserDto,
            libraries: List<com.github.damontecres.wholphin.ui.main.settings.Library>,
            isRefresh: Boolean,
        ): HomeRowLoadingState =
            try {
                homeSettingsService.fetchDataForRow(
                    row = row,
                    scope = viewModelScope,
                    prefs = prefs,
                    userDto = userDto,
                    libraries = libraries,
                    isRefresh = isRefresh,
                )
            } catch (ex: InvalidStatusException) {
                if (ex.status == 403 || ex.status == 404) {
                    Timber.w(ex, "%s on library hub row %s", ex.status, row)
                    HomeRowLoadingState.Success(
                        title = EmptyStringProvider,
                        items = emptyList(),
                        viewOptions = row.viewOptions,
                        rowType = row,
                    )
                } else {
                    HomeRowLoadingState.Error(EmptyStringProvider, exception = ex)
                }
            } catch (ex: Exception) {
                HomeRowLoadingState.Error(EmptyStringProvider, exception = ex)
            }

        fun updatePosition(position: RowColumn) {
            _state.update { it.copy(position = position) }
        }

        fun updateFocusedItem(item: BaseItem?) {
            _state.update { if (it.focusedItem == item) it else it.copy(focusedItem = item) }
        }

        fun updateBackdrop(item: BaseItem) {
            viewModelScope.launchIO { backdropService.submit(item) }
        }

        override fun setWatched(
            position: Int,
            itemId: UUID,
            played: Boolean,
        ) {
            viewModelScope.launch(ExceptionHandler() + WholphinDispatchers.IO) {
                favoriteWatchManager.setWatched(itemId, played)
                refresh()
            }
        }

        override fun setFavorite(
            position: Int,
            itemId: UUID,
            favorite: Boolean,
        ) {
            viewModelScope.launch(ExceptionHandler() + WholphinDispatchers.IO) {
                favoriteWatchManager.setFavorite(itemId, favorite)
                refresh()
            }
        }

        override fun deleteItem(
            index: Int,
            item: BaseItem,
        ) {
            deleteItem(context, mediaManagementService, item) { refresh() }
        }

        override fun isAdministrator(): Boolean = serverRepository.currentUserDto?.policy?.isAdministrator == true

        override fun navigateTo(destination: Destination) = navigationManager.navigateTo(destination)

        override fun canDelete(
            item: BaseItem,
            appPreferences: AppPreferences,
        ): Boolean = mediaManagementService.canDelete(item, appPreferences)

        override fun sendReportFor(itemId: UUID) = mediaReportService.sendReportFor(itemId)
    }

data class LibraryHubState(
    val library: com.github.damontecres.wholphin.ui.main.settings.Library? = null,
    val genreSlot: HomeRowConfig.Genres? = null,
    val activeRows: List<HomeRowLoadingState> = emptyList(),
    val firstAvailable: BaseItem? = null,
    val focusedItem: BaseItem? = null,
    val position: RowColumn = RowColumn(-1, -1),
    val loadingState: LoadingState = LoadingState.Pending,
    val refreshState: LoadingState = LoadingState.Pending,
) {
    val spotlight: BaseItem? get() = focusedItem ?: firstAvailable
}

@Composable
fun LibraryHub(
    preferences: UserPreferences,
    destination: Destination.LibraryHub,
    modifier: Modifier = Modifier,
    viewModel: LibraryHubViewModel =
        hiltViewModel<LibraryHubViewModel, LibraryHubViewModel.Factory>(
            key = destination.itemId.toString(),
        ) { it.create(destination) },
) {
    val state by viewModel.state.collectAsState()
    val firstMediaFocusRequester = remember { FocusRequester() }
    val contextMenu = rememberContextMenu(preferences, viewModel)
    val loadingState = state.loadingState

    when {
        loadingState is LoadingState.Error && state.activeRows.isEmpty() -> ErrorMessage(loadingState, modifier)
        loadingState == LoadingState.Pending || loadingState == LoadingState.Loading ->
            LoadingPage(modifier, focusEnabled = LocalContentTakeFocus.current)
        else -> HomePageContent(
            homeRows = state.activeRows,
            position = state.position,
            onFocusPosition = viewModel::updatePosition,
            onClickItem = { position, item ->
                viewModel.updatePosition(position)
                if (preferences.appPreferences.homePagePreferences.clickToPlay &&
                    state.activeRows.getOrNull(position.row).isContinueWatchingNextUp
                ) {
                    viewModel.navigateTo(Destination.Playback(item))
                } else {
                    viewModel.navigateTo(item.destination())
                }
            },
            onLongClickItem = { position, item ->
                viewModel.updatePosition(position)
                contextMenu.showContextMenu(position.column, item)
            },
            onClickPlay = { _, item -> viewModel.navigateTo(Destination.Playback(item)) },
            showClock = preferences.appPreferences.interfacePreferences.showClock,
            onUpdateBackdrop = viewModel::updateBackdrop,
            showLogo = preferences.appPreferences.interfacePreferences.showLogos,
            showViewMore = false,
            loadingState = state.refreshState,
            firstMediaFocusRequester = firstMediaFocusRequester,
            onFocusedItem = viewModel::updateFocusedItem,
            headerComposable = {
                HomePageHeader(
                    item = state.spotlight,
                    showLogo = preferences.appPreferences.interfacePreferences.showLogos,
                    modifier = HeaderUtils.modifier,
                )
            },
            modifier = modifier,
        )
    }
    contextMenu.Compose()
}

internal fun libraryHubFallbackRows(
    parentId: UUID,
    collectionType: CollectionType,
): List<HomeRowConfig> =
    buildList {
        add(HomeRowConfig.Genres(parentId))
        add(HomeRowConfig.ContinueWatching(parentId = parentId))
        if (collectionType == CollectionType.TVSHOWS) add(HomeRowConfig.NextUp(parentId = parentId))
        add(HomeRowConfig.RecentlyReleased(parentId))
        add(HomeRowConfig.RecentlyAdded(parentId))
        add(HomeRowConfig.TopUnwatched(parentId))
        add(HomeRowConfig.Suggestions(parentId))
        add(HomeRowConfig.Collections(parentId))
    }
