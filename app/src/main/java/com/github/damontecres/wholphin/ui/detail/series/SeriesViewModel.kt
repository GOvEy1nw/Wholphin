package com.github.damontecres.wholphin.ui.detail.series

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.wholphin.data.ChosenStreams
import com.github.damontecres.wholphin.data.ExtrasItem
import com.github.damontecres.wholphin.data.ItemPlaybackRepository
import com.github.damontecres.wholphin.data.ServerRepository
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.DiscoverItem
import com.github.damontecres.wholphin.data.model.ItemPlayback
import com.github.damontecres.wholphin.data.model.Person
import com.github.damontecres.wholphin.data.model.Trailer
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.services.BackdropService
import com.github.damontecres.wholphin.services.ExtrasService
import com.github.damontecres.wholphin.services.FavoriteWatchManager
import com.github.damontecres.wholphin.services.MediaManagementService
import com.github.damontecres.wholphin.services.MediaReportService
import com.github.damontecres.wholphin.services.NavigationManager
import com.github.damontecres.wholphin.services.PeopleFavorites
import com.github.damontecres.wholphin.services.SeerrService
import com.github.damontecres.wholphin.services.StreamChoiceService
import com.github.damontecres.wholphin.services.ThemeSongPlayer
import com.github.damontecres.wholphin.services.TrailerService
import com.github.damontecres.wholphin.services.UserPreferencesService
import com.github.damontecres.wholphin.services.deleteItem
import com.github.damontecres.wholphin.ui.ItemRowFields
import com.github.damontecres.wholphin.ui.equalsNotNull
import com.github.damontecres.wholphin.ui.gt
import com.github.damontecres.wholphin.ui.launchDefault
import com.github.damontecres.wholphin.ui.launchIO
import com.github.damontecres.wholphin.ui.letNotEmpty
import com.github.damontecres.wholphin.ui.lt
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.showToast
import com.github.damontecres.wholphin.util.ApiRequestPager
import com.github.damontecres.wholphin.util.BlockingList
import com.github.damontecres.wholphin.util.DataLoadingState
import com.github.damontecres.wholphin.util.ExceptionHandler
import com.github.damontecres.wholphin.util.GetEpisodesRequestHandler
import com.github.damontecres.wholphin.util.GetItemsRequestHandler
import com.github.damontecres.wholphin.util.WholphinDispatchers
import com.github.damontecres.wholphin.util.successValue
import com.google.common.cache.CacheBuilder
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetEpisodesRequest
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.api.request.GetSimilarItemsRequest
import timber.log.Timber
import java.util.UUID

@HiltViewModel(assistedFactory = SeriesViewModel.Factory::class)
class SeriesViewModel
    @AssistedInject
    constructor(
        private val api: ApiClient,
        @param:ApplicationContext val context: Context,
        val serverRepository: ServerRepository,
        private val navigationManager: NavigationManager,
        private val itemPlaybackRepository: ItemPlaybackRepository,
        private val themeSongPlayer: ThemeSongPlayer,
        private val favoriteWatchManager: FavoriteWatchManager,
        private val peopleFavorites: PeopleFavorites,
        private val trailerService: TrailerService,
        private val extrasService: ExtrasService,
        val streamChoiceService: StreamChoiceService,
        val mediaReportService: MediaReportService,
        private val userPreferencesService: UserPreferencesService,
        private val backdropService: BackdropService,
        private val seerrService: SeerrService,
        private val mediaManagementService: MediaManagementService,
        @Assisted val seriesId: UUID,
        @Assisted val seasonEpisodeIds: SeasonEpisodeIds?,
        @Assisted val seriesPageType: SeriesPageType,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(
                seriesId: UUID,
                seasonEpisodeIds: SeasonEpisodeIds?,
                seriesPageType: SeriesPageType,
            ): SeriesViewModel
        }

        private val _state = MutableStateFlow(SeriesState())
        val state: StateFlow<SeriesState> = _state

        private var episodeLoadJob: Job? = null
        private var extrasLoadJob: Job? = null

        init {
            viewModelScope.launchIO {
                Timber.v("Start")
                addCloseable { themeSongPlayer.stop() }
                val series =
                    api.userLibraryApi
                        .getItem(seriesId)
                        .content
                        .let { BaseItem(it) }
                viewModelScope.launchDefault {
                    mediaManagementService.collectCanDelete(flowOf(series)) { canDelete ->
                        _state.update { it.copy(canDeleteSeries = canDelete) }
                    }
                }
                backdropService.submit(series)

                val seasonsDeferred = getSeasons(series, seasonEpisodeIds?.seasonNumber)
                val nextUpDeferred =
                    if (seriesPageType == SeriesPageType.OVERVIEW) {
                        viewModelScope.async(WholphinDispatchers.IO) {
                            val result by api.tvShowsApi.getNextUp(seriesId = seriesId, limit = 1)
                            result.items.firstOrNull()?.let(::BaseItem)
                        }
                    } else {
                        CompletableDeferred(null)
                    }
                val initial =
                    try {
                        val seasons = seasonsDeferred.await()
                        if (seriesPageType == SeriesPageType.OVERVIEW) {
                            val resolvedTarget =
                                resolveInitialSeasonTarget(seasonEpisodeIds, nextUpDeferred.await(), seasons)
                            val seasonIndex = resolvedTarget?.let { findSeasonIndex(seasons, it) } ?: 0
                            val selectedSeason = seasonAt(seasons, seasonIndex)
                            val target =
                                resolvedTarget?.takeIf { it.seasonId == selectedSeason?.id }
                                    ?: selectedSeason?.let {
                                        InitialSeasonTarget(it.id, it.indexNumber, null, null)
                                    }
                            val episodes =
                                target?.let {
                                    loadEpisodesInternal(it.seasonId, it.episodeId, it.episodeNumber)
                                } ?: EpisodeList.Error(message = "Could not determine season")
                            val extras = target?.let { getSeasonExtras(it.seasonId) }.orEmpty()
                            InitialSeriesLoad(seasons, target, seasonIndex, episodes, extras)
                        } else {
                            InitialSeriesLoad(seasons, null, 0, EpisodeList.Loading, emptyList())
                        }
                    } catch (ex: CancellationException) {
                        throw ex
                    } catch (ex: Exception) {
                        Timber.e(ex, "Exception fetching seasons/episodes for series %s", seriesId)
                        _state.update { it.copy(series = DataLoadingState.Error(ex)) }
                        return@launchIO
                    }
                Timber.v("Done")
                val remoteTrailers = trailerService.getRemoteTrailers(series)
                _state.update {
                    it.copy(
                        series = DataLoadingState.Success(series),
                        seasons = initial.seasons,
                        episodes = initial.episodes,
                        extras = initial.extras,
                        position =
                            SeriesOverviewPosition(
                                initial.seasonIndex.coerceAtLeast(0),
                                initial.target?.seasonId,
                                (initial.episodes as? EpisodeList.Success)?.initialEpisodeIndex ?: 0,
                            ),
                        initialFocusEpisode =
                            initial.target?.let { it.episodeId != null || it.episodeNumber != null } == true,
                        trailers = remoteTrailers,
                    )
                }

                if (seriesPageType == SeriesPageType.DETAILS) {
                    viewModelScope.launchIO {
                        trailerService.getLocalTrailers(series).letNotEmpty { localTrailers ->
                            _state.update { it.copy(trailers = localTrailers + remoteTrailers) }
                        }
                    }
                    viewModelScope.launchIO {
                        val people = peopleFavorites.getPeopleFor(series)
                        _state.update { it.copy(people = people) }
                    }
                    viewModelScope.launchIO {
                        val extras = extrasService.getExtras(series.id)
                        _state.update { it.copy(extras = extras) }
                    }
                    if (state.value.similar.isEmpty()) {
                        viewModelScope.launchIO {
                            val similar =
                                api.libraryApi
                                    .getSimilarItems(
                                        GetSimilarItemsRequest(
                                            userId = serverRepository.currentUser?.id,
                                            itemId = seriesId,
                                            fields = ItemRowFields,
                                            limit = 25,
                                        ),
                                    ).content.items
                                    .map { BaseItem(it, true) }
                            _state.update { it.copy(similar = similar) }
                        }
                    }
                    viewModelScope.launchIO {
                        val results = seerrService.similar(series).orEmpty()
                        _state.update { it.copy(discovered = results) }
                    }
                    viewModelScope.launchIO {
                        seerrService.active.collectLatest { active ->
                            val tv =
                                if (active) {
                                    try {
                                        seerrService
                                            .getTvSeries(series)
                                            ?.let { seerrService.createDiscoverItem(it) }
                                    } catch (ex: Exception) {
                                        Timber.e(ex)
                                        null
                                    }
                                } else {
                                    null
                                }
                            _state.update { it.copy(discoverSeries = tv) }
                        }
                    }
                }
                mediaManagementService.deletedItemFlow
                    .onEach { deletedItem ->
                        if (deletedItem.item.data.seriesId == seriesId) {
                            Timber.d(
                                "Item %s deleted from series %s",
                                deletedItem.item.id,
                                seriesId,
                            )
                            val seasons = getSeasons(series, seasonEpisodeIds?.seasonNumber).await()
                            _state.update { it.copy(seasons = seasons) }
                        }
                    }.catch { ex ->
                        Timber.e(ex, "Error refreshing after deleted item")
                    }.launchIn(viewModelScope)
            }
        }

        fun onResumePage() {
            state.value.series.successValue?.let { item ->
                viewModelScope.launchDefault { backdropService.submit(item) }
                viewModelScope.launchDefault {
                    themeSongPlayer.playThemeFor(seriesId)
                }
            }
        }

        fun refresh() {
            state.value.series.successValue?.let { item ->
                viewModelScope.launchIO {
                    (state.value.seasons as? ApiRequestPager<*>)?.refresh()
                }
            }
        }

        fun release() {
            themeSongPlayer.stop()
        }

        fun selectSeason(seasonTabIndex: Int) {
            val season = state.value.seasons.getOrNull(seasonTabIndex) ?: return
            loadSeasonEpisodes(season.id, seasonTabIndex)
        }

        fun selectEpisode(episodeRowIndex: Int) {
            _state.update {
                if (it.position.episodeRowIndex == episodeRowIndex) {
                    it
                } else {
                    it.copy(
                        position = it.position.copy(episodeRowIndex = episodeRowIndex),
                        chosenStreams = null,
                    )
                }
            }
        }

        private fun getSeasons(
            series: BaseItem,
            seasonNum: Int?,
        ): Deferred<List<BaseItem?>> =
            viewModelScope.async(WholphinDispatchers.IO) {
                Timber.v("getSeasons for %s", series.id)
                val request =
                    GetItemsRequest(
                        parentId = series.id,
                        recursive = false,
                        includeItemTypes = listOf(BaseItemKind.SEASON),
                        sortBy = listOf(ItemSortBy.INDEX_NUMBER),
                        sortOrder = listOf(SortOrder.ASCENDING),
                        enableUserData = true,
                        fields =
                            if (seriesPageType == SeriesPageType.DETAILS) {
                                listOf(
                                    ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                                    ItemFields.CAN_DELETE,
                                )
                            } else {
                                listOf(
                                    ItemFields.CAN_DELETE,
                                )
                            },
                    )
                val pager =
                    ApiRequestPager(
                        api,
                        request,
                        GetItemsRequestHandler,
                        viewModelScope,
                        pageSize = 20,
                    ).init(seasonNum ?: 0)
                pager
            }

        private suspend fun findSeasonIndex(
            seasons: List<BaseItem?>,
            target: InitialSeasonTarget,
        ): Int =
            ((seasons as? ApiRequestPager<*>)?.let {
                findIndexByNumberOrIdFast(target.seasonNumber, target.seasonId, it, null)
            } ?: seasons.indexOfFirst { it?.id == target.seasonId }).coerceAtLeast(0)

        private suspend fun seasonAt(
            seasons: List<BaseItem?>,
            index: Int,
        ): BaseItem? =
            if (index !in seasons.indices) {
                null
            } else {
                (seasons as? BlockingList<BaseItem?>)?.getBlocking(index) ?: seasons.getOrNull(index)
            }

        private suspend fun loadEpisodesInternal(
            seasonId: UUID,
            episodeId: UUID?,
            episodeNumber: Int?,
        ): EpisodeList {
            val request =
                GetEpisodesRequest(
                    seriesId = seriesId,
                    seasonId = seasonId,
                    sortBy = ItemSortBy.INDEX_NUMBER,
                    fields =
                        listOf(
                            ItemFields.MEDIA_SOURCES,
                            ItemFields.MEDIA_SOURCE_COUNT,
                            ItemFields.OVERVIEW,
                            ItemFields.CUSTOM_RATING,
                            ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                            ItemFields.CAN_DELETE,
                            ItemFields.PARENT_ID,
                        ),
                )
            Timber.v(
                "loadEpisodesInternal: episodeId=%s, episodeNumber=%s",
                episodeId,
                episodeNumber,
            )
            val pager = ApiRequestPager(api, request, GetEpisodesRequestHandler, viewModelScope)
            pager.init(episodeNumber ?: 0)
            val initialIndex =
                if (episodeId != null || episodeNumber != null) {
                    findIndexByNumberOrIdFast(episodeNumber, episodeId, pager, seasonId)
                        .coerceAtLeast(0)
                } else {
                    firstUnwatchedEpisodeIndex(pager)
                }
            Timber.v("Loaded ${pager.size} episodes for season $seasonId, initialIndex=$initialIndex")
            return EpisodeList.Success(seasonId, pager, initialIndex)
        }

        private fun loadSeasonEpisodes(
            seasonId: UUID,
            seasonTabIndex: Int,
        ) {
            episodeLoadJob?.cancel()
            extrasLoadJob?.cancel()
            var generation = 0L
            _state.update {
                generation = it.seasonLoadGeneration + 1
                it.copy(
                    position = SeriesOverviewPosition(seasonTabIndex, seasonId, 0),
                    seasonLoadGeneration = generation,
                    peopleInEpisode = PeopleInItem(),
                    episodes = EpisodeList.Loading,
                    extras = emptyList(),
                    chosenStreams = null,
                )
            }
            episodeLoadJob = viewModelScope.launchIO {
                val episodes =
                    try {
                        loadEpisodesInternal(seasonId, null, null)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.e(e, "Error loading episodes for $seriesId for season $seasonId")
                        EpisodeList.Error(e)
                    }
                _state.update {
                    if (canCommitSeasonLoad(it.seasonLoadGeneration, generation, it.position.seasonId, seasonId)) {
                        it.copy(
                            episodes = episodes,
                            position =
                                it.position.copy(
                                    episodeRowIndex =
                                        (episodes as? EpisodeList.Success)?.initialEpisodeIndex ?: 0,
                                ),
                            chosenStreams = null,
                        )
                    } else {
                        it
                    }
                }
            }
            extrasLoadJob = viewModelScope.launchIO {
                val extras = getSeasonExtras(seasonId)
                _state.update {
                    if (canCommitSeasonLoad(it.seasonLoadGeneration, generation, it.position.seasonId, seasonId)) {
                        it.copy(extras = extras)
                    } else {
                        it
                    }
                }
            }
        }

        private suspend fun getSeasonExtras(seasonId: UUID): List<ExtrasItem> =
            try {
                extrasService.getExtras(seasonId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error loading extras for $seriesId for season $seasonId")
                emptyList()
            }

        fun setWatched(
            itemId: UUID,
            played: Boolean,
            listIndex: Int?,
        ) = viewModelScope.launch(WholphinDispatchers.IO + ExceptionHandler()) {
            favoriteWatchManager.setWatched(itemId, played)
            listIndex?.let {
                refreshEpisode(itemId, listIndex)
            }
        }

        private fun updateSeries() {
            viewModelScope.launchIO {
                try {
                    val series =
                        api.userLibraryApi
                            .getItem(seriesId)
                            .content
                            .let(::BaseItem)
                    _state.update { it.copy(series = DataLoadingState.Success(series)) }
                    viewModelScope.launchIO {
                        val people = peopleFavorites.getPeopleFor(series)
                        _state.update { it.copy(people = people) }
                    }
                    viewModelScope.launchIO {
                        val seasons = getSeasons(series, null).await()
                        _state.update { it.copy(seasons = seasons) }
                    }
                } catch (ex: Exception) {
                    Timber.e(ex, "Error updating series")
                    showToast(context, "Error updating series")
                }
            }
        }

        fun setFavorite(
            itemId: UUID,
            favorite: Boolean,
            listIndex: Int?,
        ) = viewModelScope.launch(ExceptionHandler() + WholphinDispatchers.IO) {
            favoriteWatchManager.setFavorite(itemId, favorite)
            if (listIndex != null) {
                refreshEpisode(itemId, listIndex)
            } else {
                updateSeries()
            }
        }

        fun setSeasonWatched(
            seasonId: UUID,
            played: Boolean,
        ) = viewModelScope.launch(WholphinDispatchers.IO + ExceptionHandler()) {
            setWatched(seasonId, played, null)
            updateSeries()
        }

        fun setWatchedSeries(played: Boolean) =
            viewModelScope.launch(ExceptionHandler() + WholphinDispatchers.IO) {
                favoriteWatchManager.setWatched(seriesId, played)
                updateSeries()
            }

        fun refreshEpisode(
            itemId: UUID,
            listIndex: Int,
        ) = viewModelScope.launch(ExceptionHandler() + WholphinDispatchers.IO) {
            val eps = state.value.episodes
            if (eps is EpisodeList.Success) {
                val generation = state.value.seasonLoadGeneration
                eps.episodes.refreshItem(listIndex, itemId)
                _state.update {
                    if (canCommitSeasonLoad(it.seasonLoadGeneration, generation, it.position.seasonId, eps.seasonId)) {
                        it.copy(episodes = eps)
                    } else {
                        it
                    }
                }
            }
            // Kind of hack to ensure the backdrop is reloaded if needed
            state.value.series.successValue
                ?.let { backdropService.submit(it) }
        }

        /**
         * Play whichever episode is next up for series or else the first episode
         */
        fun playNextUp() {
            viewModelScope.launch(ExceptionHandler() + WholphinDispatchers.IO) {
                val result by api.tvShowsApi.getNextUp(seriesId = seriesId)
                val nextUp =
                    result.items.firstOrNull() ?: api.tvShowsApi
                        .getEpisodes(
                            seriesId,
                            limit = 1,
                        ).content.items
                        .firstOrNull()
                if (nextUp != null) {
                    withContext(WholphinDispatchers.Main) {
                        navigateTo(Destination.Playback(BaseItem(nextUp)))
                    }
                } else {
                    showToast(
                        context,
                        "Could not find an episode to play",
                        Toast.LENGTH_SHORT,
                    )
                }
            }
        }

        fun navigateTo(destination: Destination) {
            release()
            navigationManager.navigateTo(destination)
        }

        private var chosenStreamsJob: Job? = null

        fun lookUpChosenTracks(
            itemId: UUID,
            item: BaseItem,
        ) {
            chosenStreamsJob?.cancel()
            chosenStreamsJob =
                viewModelScope.launchIO {
                    val result =
                        itemPlaybackRepository.getSelectedTracks(
                            itemId,
                            item,
                            userPreferencesService.getCurrent(),
                        )
                    _state.update {
                        if (it.isSelectedEpisode(itemId)) {
                            it.copy(chosenStreams = result)
                        } else {
                            it
                        }
                    }
                }
        }

        fun savePlayVersion(
            item: BaseItem,
            sourceId: UUID,
        ) {
            viewModelScope.launchIO {
                val prefs = userPreferencesService.getCurrent()
                val plc = streamChoiceService.getPlaybackLanguageChoice(item.data)
                val result = itemPlaybackRepository.savePlayVersion(item.id, sourceId)
                val chosen =
                    result?.let {
                        itemPlaybackRepository.getChosenItemFromPlayback(item, result, plc, prefs)
                    }
                _state.update {
                    if (it.isSelectedEpisode(item.id)) {
                        it.copy(chosenStreams = chosen)
                    } else {
                        it
                    }
                }
            }
        }

        fun saveTrackSelection(
            item: BaseItem,
            itemPlayback: ItemPlayback?,
            trackIndex: Int,
            type: MediaStreamType,
        ) {
            viewModelScope.launchIO {
                val prefs = userPreferencesService.getCurrent()
                val plc = streamChoiceService.getPlaybackLanguageChoice(item.data)
                val result =
                    itemPlaybackRepository.saveTrackSelection(
                        item = item,
                        itemPlayback = itemPlayback,
                        trackIndex = trackIndex,
                        type = type,
                    )
                val chosen =
                    result?.let {
                        itemPlaybackRepository.getChosenItemFromPlayback(item, result, plc, prefs)
                    }
                _state.update {
                    if (it.isSelectedEpisode(item.id)) {
                        it.copy(chosenStreams = chosen)
                    } else {
                        it
                    }
                }
            }
        }

        private var peopleInEpisodeJob: Job? = null
        private val peopleInEpisodeCache =
            CacheBuilder
                .newBuilder()
                .maximumSize(25)
                .build<UUID, Deferred<PeopleInItem>>()

        suspend fun lookupPeopleInEpisode(item: BaseItem) {
            peopleInEpisodeJob?.cancel()
            if (state.value.peopleInEpisode.itemId != item.id) {
                _state.update { it.copy(peopleInEpisode = PeopleInItem()) }
                val result =
                    peopleInEpisodeCache
                        .get(item.id) {
                            viewModelScope.async(WholphinDispatchers.IO) {
                                val list =
                                    api.userLibraryApi
                                        .getItem(item.id)
                                        .content.people
                                        ?.map { Person.fromDto(context, it, api) }
                                        .orEmpty()

                                PeopleInItem(item.id, list)
                            }
                        }
                peopleInEpisodeJob =
                    viewModelScope.launch(ExceptionHandler()) {
                        delay(250)
                        val peopleInEpisode = result.await()
                        _state.update { it.copy(peopleInEpisode = peopleInEpisode) }
                    }
            }
        }

        fun clearChosenStreams(
            item: BaseItem,
            chosenStreams: ChosenStreams?,
        ) {
            viewModelScope.launchIO {
                itemPlaybackRepository.deleteChosenStreams(chosenStreams)
                lookUpChosenTracks(item.id, item)
            }
        }

        fun deleteItem(item: BaseItem) {
            deleteItem(context, mediaManagementService, item) {
                viewModelScope.launchDefault {
                    if (item.type == BaseItemKind.SERIES) {
                        navigationManager.goBack()
                    } else if (seriesPageType == SeriesPageType.DETAILS) {
                        state.value.series.successValue?.let { series ->
                            val seasons = getSeasons(series, null).await()
                            if (seasons.isEmpty()) {
                                navigationManager.goBack()
                            } else {
                                _state.update { it.copy(seasons = seasons) }
                            }
                        }
                    } else {
                        state.value.position.episodeRowIndex.let { episodeIndex ->
                            val eps = state.value.episodes as? EpisodeList.Success
                            if (eps != null) {
                                val pager = eps.episodes
                                val lastIndex = pager.lastIndex
                                pager.refreshPagesAfter(episodeIndex)
                                if (pager.isEmpty()) {
                                    navigationManager.goBack()
                                } else {
                                    if (episodeIndex == lastIndex) {
                                        // Deleted last episode, so need to move left
                                        _state.update {
                                            it.copy(
                                                episodes =
                                                    EpisodeList.Success(
                                                        eps.seasonId,
                                                        pager,
                                                        episodeIndex - 1,
                                                    ),
                                                position = it.position.copy(episodeRowIndex = episodeIndex - 1),
                                                chosenStreams = null,
                                            )
                                        }
                                    } else {
                                        _state.update {
                                            it.copy(
                                                episodes =
                                                    EpisodeList.Success(
                                                        eps.seasonId,
                                                        pager,
                                                        episodeIndex,
                                                    ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        suspend fun canDelete(item: BaseItem): Boolean = mediaManagementService.canDelete(item)

        fun canDelete(
            item: BaseItem,
            appPreferences: AppPreferences,
        ): Boolean = mediaManagementService.canDelete(item, appPreferences)
    }

sealed interface EpisodeList {
    data object Loading : EpisodeList

    data class Error(
        val message: String? = null,
        val exception: Throwable? = null,
    ) : EpisodeList {
        constructor(exception: Throwable) : this(null, exception)
    }

    data class Success(
        val seasonId: UUID,
        val episodes: ApiRequestPager<GetEpisodesRequest>,
        val initialEpisodeIndex: Int,
    ) : EpisodeList
}

data class PeopleInItem(
    val itemId: UUID? = null,
    val people: List<Person> = listOf(),
)

enum class SeriesPageType {
    DETAILS,
    OVERVIEW,
}

private fun checkNumberOrId(
    targetNum: Int?,
    targetId: UUID?,
    indexNumber: Int?,
    id: UUID?,
): Boolean =
    if (targetId != null) {
        equalsNotNull(targetId, id)
    } else {
        equalsNotNull(indexNumber, targetNum)
    }

/**
 * Find the index in the [list] where the item's `indexNumber`==[targetNum] or `id`==[targetId]
 *
 * If the list is smaller than its page size, then the entire dataset is cached and
 * [BlockingList.indexOfBlocking] is used, otherwise this function calls [findIndexByNumberOrId]
 */
suspend fun findIndexByNumberOrIdFast(
    targetNum: Int?,
    targetId: UUID?,
    list: ApiRequestPager<*>,
    parentId: UUID?,
): Int =
    if (list.size <= list.pageSize) {
        Timber.v("Using findIndexByNumberOrIdFast indexOfBlocking method")
        list.indexOfBlocking {
            checkNumberOrId(targetNum, targetId, it?.indexNumber, it?.id) &&
                if (parentId != null) {
                    it?.data?.parentId == parentId
                } else {
                    true
                }
        }
    } else {
        findIndexByNumberOrId(targetNum, targetId, list as BlockingList<BaseItem?>, parentId)
    }

/**
 * Find the index in the [list] where the item's `indexNumber`==[targetNum] or `id`==[targetId]
 *
 * This is necessary in cases where items are missing. E.g. if looking for episode 4 but
 * episodes 2 & 3 is missing, then the index in the list for episode 4 will be `1`.
 *
 * @param targetNum the 1-index season or episode number
 * @param targetId the season or episode ID
 *
 * @return the index within [list] that matches, or zero if no match is found
 */
suspend fun findIndexByNumberOrId(
    targetNum: Int?,
    targetId: UUID?,
    list: BlockingList<BaseItem?>,
    parentId: UUID? = null,
): Int {
    Timber.v("Using findIndexByNumberOrId")
    // Adjust for 1-based numbers
    val listIndex = targetNum?.minus(1)?.coerceAtLeast(0)
    val index =
        if (targetId != null && (targetNum == null || listIndex !in list.indices)) {
            // No hint info, so have to check everything
            list
                .indexOfBlocking {
                    checkNumberOrId(targetNum, targetId, it?.indexNumber, it?.id)
                }.coerceAtLeast(0)
        } else if (listIndex != null && listIndex in list.indices) {
            searchList(listIndex, targetNum, targetId, list, parentId)
        } else {
            0
        }
    return index
}

private suspend fun searchList(
    listIndex: Int,
    targetNum: Int,
    targetId: UUID?,
    list: BlockingList<BaseItem?>,
    parentId: UUID?,
): Int {
    val item = list.getBlocking(listIndex)
    if (parentId != null && item?.data?.parentId != parentId) {
        return if (listIndex - 1 in list.indices) {
            searchList(listIndex - 1, targetNum, targetId, list, parentId)
        } else if (listIndex + 1 in list.indices) {
            searchList(listIndex + 1, targetNum, targetId, list, parentId)
        } else {
            0
        }
    }
    val num = item?.indexNumber
    if (num.lt(targetNum)) {
        for (i in listIndex + 1 until list.size) {
            val item = list.getBlocking(i)
            if (checkNumberOrId(targetNum, targetId, item?.indexNumber, item?.id)) {
                return i
            }
        }
        return 0
    } else if (num.gt(targetNum)) {
        for (i in listIndex - 1 downTo 0) {
            val item = list.getBlocking(i)
            if (checkNumberOrId(targetNum, targetId, item?.indexNumber, item?.id)) {
                return i
            }
        }
        return 0
    } else {
        return list
            .indexOfBlocking {
                checkNumberOrId(targetNum, targetId, it?.indexNumber, it?.id)
            }.coerceAtLeast(0)
    }
}

data class SeriesState(
    val series: DataLoadingState<BaseItem> = DataLoadingState.Pending,
    val seasons: List<BaseItem?> = emptyList(),
    val episodes: EpisodeList = EpisodeList.Loading,
    val position: SeriesOverviewPosition = SeriesOverviewPosition(0, null, 0),
    val initialFocusEpisode: Boolean = false,
    val seasonLoadGeneration: Long = 0,
    val trailers: List<Trailer> = emptyList(),
    val extras: List<ExtrasItem> = emptyList(),
    val people: List<Person> = emptyList(),
    val similar: List<BaseItem> = emptyList(),
    val canDeleteSeries: Boolean = false,
    val peopleInEpisode: PeopleInItem = PeopleInItem(),
    val discovered: List<DiscoverItem> = emptyList(),
    val discoverSeries: DiscoverItem? = null,
    val chosenStreams: ChosenStreams? = null,
)

private fun SeriesState.isSelectedEpisode(itemId: UUID): Boolean {
    val currentEpisodes = episodes as? EpisodeList.Success ?: return false
    return canCommitChosenStreams(
        position.seasonId,
        currentEpisodes.seasonId,
        currentEpisodes.episodes.getOrNull(position.episodeRowIndex)?.id,
        itemId,
    )
}

internal data class InitialSeasonTarget(
    val seasonId: UUID,
    val seasonNumber: Int?,
    val episodeId: UUID?,
    val episodeNumber: Int?,
)

internal data class SeasonTargetCandidate(
    val target: InitialSeasonTarget,
    val unplayedItemCount: Int?,
)

internal fun selectInitialSeasonTarget(
    explicit: InitialSeasonTarget?,
    nextUp: InitialSeasonTarget?,
    seasons: List<SeasonTargetCandidate>,
): InitialSeasonTarget? =
    explicit ?: nextUp ?: seasons.firstOrNull { (it.unplayedItemCount ?: 0) > 0 }?.target ?: seasons.firstOrNull()?.target

internal fun canCommitSeasonLoad(
    currentGeneration: Long,
    responseGeneration: Long,
    currentSeasonId: UUID?,
    responseSeasonId: UUID,
): Boolean = currentGeneration == responseGeneration && currentSeasonId == responseSeasonId

internal fun canCommitChosenStreams(
    selectedSeasonId: UUID?,
    responseSeasonId: UUID,
    selectedEpisodeId: UUID?,
    responseEpisodeId: UUID,
): Boolean = selectedSeasonId == responseSeasonId && selectedEpisodeId == responseEpisodeId

private suspend fun resolveInitialSeasonTarget(
    explicit: SeasonEpisodeIds?,
    nextUp: BaseItem?,
    seasons: List<BaseItem?>,
): InitialSeasonTarget? {
    val explicitTarget = explicit?.let { InitialSeasonTarget(it.seasonId, it.seasonNumber, it.episodeId, it.episodeNumber) }
    val nextUpTarget =
        nextUp?.data?.seasonId?.let {
            InitialSeasonTarget(it, nextUp.data.parentIndexNumber, nextUp.id, nextUp.indexNumber)
        }
    return selectInitialSeasonTarget(
        explicitTarget,
        nextUpTarget,
        if (explicitTarget == null && nextUpTarget == null) seasonTargetCandidates(seasons) else emptyList(),
    )
}

private suspend fun seasonTargetCandidates(seasons: List<BaseItem?>): List<SeasonTargetCandidate> {
    val blockingSeasons = seasons as? BlockingList<BaseItem?>
    if (blockingSeasons == null) {
        return seasons.mapNotNull(::seasonTargetCandidate)
    }
    val first = if (blockingSeasons.isEmpty()) null else blockingSeasons.getBlocking(0)
    val firstUnplayed =
        blockingSeasons
            .indexOfBlocking { (it?.data?.userData?.unplayedItemCount ?: 0) > 0 }
            .takeIf { it >= 0 }
            ?.let { blockingSeasons.getBlocking(it) }
    return listOfNotNull(firstUnplayed, first).mapNotNull(::seasonTargetCandidate)
}

private fun seasonTargetCandidate(season: BaseItem?): SeasonTargetCandidate? =
    season?.let {
        SeasonTargetCandidate(
            InitialSeasonTarget(it.id, it.indexNumber, null, null),
            it.data.userData?.unplayedItemCount,
        )
    }

internal suspend fun firstUnwatchedEpisodeIndex(episodes: BlockingList<BaseItem?>): Int =
    episodes.indexOfBlocking { it?.data?.userData?.played != true }.coerceAtLeast(0)

private data class InitialSeriesLoad(
    val seasons: List<BaseItem?>,
    val target: InitialSeasonTarget?,
    val seasonIndex: Int,
    val episodes: EpisodeList,
    val extras: List<ExtrasItem>,
)
