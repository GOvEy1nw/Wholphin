package com.github.damontecres.wholphin.ui.detail.series

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.ChosenStreams
import com.github.damontecres.wholphin.data.ExtrasItem
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.Person
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.ui.AspectRatios
import com.github.damontecres.wholphin.ui.cards.BannerCard
import com.github.damontecres.wholphin.ui.cards.ExtrasRow
import com.github.damontecres.wholphin.ui.cards.PersonRow
import com.github.damontecres.wholphin.ui.components.ErrorMessage
import com.github.damontecres.wholphin.ui.components.HeaderUtils
import com.github.damontecres.wholphin.ui.components.LoadingPage
import com.github.damontecres.wholphin.ui.components.TabDetails
import com.github.damontecres.wholphin.ui.components.TabRow
import com.github.damontecres.wholphin.ui.components.TitleOrLogo
import com.github.damontecres.wholphin.ui.handleDPadKeyEvents
import com.github.damontecres.wholphin.ui.ifElse
import com.github.damontecres.wholphin.ui.logTab
import com.github.damontecres.wholphin.ui.playback.isPlayKeyUp
import com.github.damontecres.wholphin.ui.tryRequestFocus
import com.github.damontecres.wholphin.ui.util.StringStringProvider
import com.github.damontecres.wholphin.ui.util.rememberDelayedNestedScroll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.PersonKind
import kotlin.time.Duration

private const val SEASON_PREVIEW_DELAY_MS = 200L

private sealed interface SeriesFocusTarget {
    data class Season(val index: Int) : SeriesFocusTarget

    data class Episode(val seasonIndex: Int) : SeriesFocusTarget

    data object External : SeriesFocusTarget
}

@Composable
fun SeriesOverviewContent(
    preferences: UserPreferences,
    series: BaseItem,
    seasons: List<BaseItem?>,
    episodes: EpisodeList,
    seasonExtras: List<ExtrasItem>,
    chosenStreams: ChosenStreams?,
    peopleInEpisode: List<Person>,
    position: SeriesOverviewPosition,
    initialFocusEpisode: Boolean,
    firstItemFocusRequester: FocusRequester,
    episodeRowFocusRequester: FocusRequester,
    castCrewRowFocusRequester: FocusRequester,
    guestStarRowFocusRequester: FocusRequester,
    extrasRowFocusRequester: FocusRequester,
    onChangeSeason: (Int) -> Unit,
    onFocusEpisode: (Int) -> Unit,
    onClick: (BaseItem) -> Unit,
    onLongClick: (BaseItem) -> Unit,
    playOnClick: (Duration) -> Unit,
    watchOnClick: () -> Unit,
    favoriteOnClick: () -> Unit,
    moreOnClick: () -> Unit,
    overviewOnClick: () -> Unit,
    seriesOverviewOnClick: () -> Unit,
    personOnClick: (Person) -> Unit,
    canDelete: (BaseItem) -> Boolean,
    onConfirmDelete: (BaseItem) -> Unit,
    onClickExtra: (Int, ExtrasItem) -> Unit,
    onChooseVersion: (BaseItem, MediaSourceInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val selectedTabIndex = position.seasonTabIndex
    LaunchedEffect(selectedTabIndex) {
        logTab("series_overview", selectedTabIndex)
    }
    val focusedEpisode =
        (episodes as? EpisodeList.Success)?.episodes?.getOrNull(position.episodeRowIndex)
    var pageHasFocus by remember { mutableStateOf(false) }
    var cardRowHasFocus by remember { mutableStateOf(false) }
    var episodeSpotlight by remember(initialFocusEpisode) { mutableStateOf(initialFocusEpisode) }
    val dimming by animateFloatAsState(if (pageHasFocus && !cardRowHasFocus) .4f else 1f)

    val scrollState = rememberScrollState()
    val scrollConnection = rememberDelayedNestedScroll()
    var focusedSeasonIndex by remember { mutableStateOf<Int?>(null) }
    var focusTarget by
        remember(initialFocusEpisode) {
            mutableStateOf<SeriesFocusTarget>(
                if (initialFocusEpisode) {
                    SeriesFocusTarget.Episode(position.seasonTabIndex)
                } else {
                    SeriesFocusTarget.Season(position.seasonTabIndex)
                },
            )
        }
    var focusRequestVersion by remember { mutableIntStateOf(0) }

    val seasonStr = stringResource(R.string.tv_season)
    val tabFocusRequesters = remember(seasons) { List(seasons.size) { FocusRequester() } }
    val contentFocusRequesters = remember(seasons) { List(seasons.size) { FocusRequester() } }
    val tabs =
        seasons.mapIndexed { index, season ->
            TabDetails(
                title =
                    StringStringProvider(
                        season?.name
                            ?: season?.data?.indexNumber?.let { "$seasonStr $it" }
                            ?: "",
                    ),
                tabFocusRequester = tabFocusRequesters[index],
                contentFocusRequester = contentFocusRequesters[index],
            )
        }

    val currentOnChangeSeason by rememberUpdatedState(onChangeSeason)
    val currentSelectedTabIndex by rememberUpdatedState(selectedTabIndex)

    val episodeFocusKey =
        (episodes as? EpisodeList.Success)?.let {
            if (it.seasonId == position.seasonId) {
                it.seasonId to position.episodeRowIndex
            } else {
                null
            }
        }
    val focusAvailabilityKey =
        when (val target = focusTarget) {
            is SeriesFocusTarget.Season -> selectedTabIndex
            is SeriesFocusTarget.Episode ->
                if (selectedTabIndex == target.seasonIndex) episodeFocusKey else null
            SeriesFocusTarget.External -> null
        }
    LaunchedEffect(focusTarget, focusAvailabilityKey, focusRequestVersion) {
        when (val target = focusTarget) {
            is SeriesFocusTarget.Season -> {
                val scrollPosition = scrollState.value
                if (tabFocusRequesters.getOrNull(target.index)?.tryRequestFocus() == true) {
                    withFrameNanos { }
                    scrollState.scrollTo(scrollPosition)
                }
            }
            is SeriesFocusTarget.Episode ->
                if (focusAvailabilityKey != null) {
                    episodeRowFocusRequester.tryRequestFocus()
                }
            SeriesFocusTarget.External -> Unit
        }
    }

    LaunchedEffect(focusedSeasonIndex) {
        val focusedIndex = focusedSeasonIndex ?: return@LaunchedEffect
        if (focusedIndex != currentSelectedTabIndex) {
            delay(SEASON_PREVIEW_DELAY_MS)
            if (focusedSeasonIndex == focusedIndex && focusedIndex != currentSelectedTabIndex) {
                focusTarget = SeriesFocusTarget.Season(focusedIndex)
                currentOnChangeSeason(focusedIndex)
            }
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp)
                    .focusGroup()
                    .nestedScroll(scrollConnection)
                    .verticalScroll(scrollState)
                    .onFocusChanged { pageHasFocus = it.hasFocus },
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier =
                    Modifier
                        .focusGroup()
                        .bringIntoViewRequester(bringIntoViewRequester),
            ) {
                val paddingValues =
                    remember(preferences.appPreferences.interfacePreferences.showClock) {
                        if (preferences.appPreferences.interfacePreferences.showClock) {
                            PaddingValues(start = 0.dp, end = 184.dp)
                        } else {
                            PaddingValues(start = 0.dp, end = 16.dp)
                        }
                    }
                if (episodeSpotlight) {
                    TitleOrLogo(
                        item = series,
                        showLogo = preferences.appPreferences.interfacePreferences.showLogos,
                        modifier = Modifier.padding(start = HeaderUtils.startPadding),
                    )
                    FocusedEpisodeHeader(
                        preferences = preferences,
                        ep = focusedEpisode,
                        chosenStreams = chosenStreams,
                        overviewOnClick = overviewOnClick,
                        overviewOnFocus = {
                            if (it.isFocused) {
                                scope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(.6f),
                    )
                } else {
                    SeriesDetailsHeader(
                        series = series,
                        showLogo = preferences.appPreferences.interfacePreferences.showLogos,
                        overviewOnClick = seriesOverviewOnClick,
                        bringIntoViewRequester = bringIntoViewRequester,
                    )
                }
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    tabs = tabs,
                    onClick =
                        remember {
                            {
                                focusedSeasonIndex = null
                                focusTarget = SeriesFocusTarget.Episode(it)
                                if (it != currentSelectedTabIndex) {
                                    currentOnChangeSeason(it)
                                }
                            }
                        },
                    onFocusedTabIndexChange = {
                        focusedSeasonIndex = it
                        if (it != null) {
                            focusTarget = SeriesFocusTarget.Season(it)
                        }
                    },
                    onDown = {
                        focusTarget = SeriesFocusTarget.Episode(it)
                        if (it != currentSelectedTabIndex) {
                            currentOnChangeSeason(it)
                        }
                    },
                    modifier =
                        Modifier
                            .padding(paddingValues)
                            .padding(bottom = 4.dp)
                            .fillMaxWidth(),
                )

//                key(position.seasonTabIndex) {
                when (val eps = episodes) {
                    EpisodeList.Loading -> {
                        LoadingPage(focusEnabled = false)
                    }

                    is EpisodeList.Error -> {
                        ErrorMessage(eps.message, eps.exception)
                    }

                    is EpisodeList.Success -> {
                        val state = rememberLazyListState(position.episodeRowIndex)
                        LazyRow(
                            state = state,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier =
                                Modifier
                                    .focusRestorer(firstItemFocusRequester)
                                    .onFocusChanged {
                                        cardRowHasFocus = it.hasFocus
                                        if (it.hasFocus) {
                                            focusTarget = SeriesFocusTarget.Episode(selectedTabIndex)
                                        }
                                    },
                        ) {
                            itemsIndexed(eps.episodes) { episodeIndex, episode ->
                                val interactionSource = remember { MutableInteractionSource() }
                                if (interactionSource.collectIsFocusedAsState().value) {
                                    onFocusEpisode.invoke(episodeIndex)
                                }
                                BannerCard(
                                    name = episode?.name,
                                    item = episode,
                                    aspectRatio =
                                        episode
                                            ?.aspectRatio
                                            ?.coerceAtLeast(AspectRatios.FOUR_THREE)
                                            ?: (AspectRatios.WIDE),
                                    cornerText = episode?.ui?.episodeCornerText,
                                    played = episode?.data?.userData?.played ?: false,
                                    playPercent =
                                        episode?.data?.userData?.playedPercentage
                                            ?: 0.0,
                                    onClick = {
                                        if (episode != null) onClick.invoke(episode)
                                    },
                                    onLongClick = {
                                        if (episode != null) onLongClick.invoke(episode)
                                    },
                                    modifier =
                                        Modifier
                                            .handleDPadKeyEvents(
                                                onUp = {
                                                    focusTarget = SeriesFocusTarget.Season(selectedTabIndex)
                                                },
                                            )
                                            .ifElse(
                                                episodeIndex == position.episodeRowIndex,
                                                Modifier
                                                    .focusRequester(firstItemFocusRequester),
                                            ).ifElse(
                                                episodeIndex == position.episodeRowIndex,
                                                Modifier.focusRequester(episodeRowFocusRequester),
                                            ).background(
                                                if (episodeIndex != position.episodeRowIndex) {
                                                    Color.Black
                                                } else {
                                                    Color.Transparent
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                            ).graphicsLayer {
                                                alpha =
                                                    if (episodeIndex != position.episodeRowIndex) {
                                                        dimming
                                                    } else {
                                                        1f
                                                    }
                                            }.onFocusChanged {
                                                if (it.isFocused) {
                                                    focusTarget = SeriesFocusTarget.Episode(selectedTabIndex)
                                                    episodeSpotlight = true
                                                    scope.launch {
                                                        bringIntoViewRequester.bringIntoView()
                                                    }
                                                }
                                            }.onKeyEvent {
                                                if (episode != null && isPlayKeyUp(it)) {
                                                    onClick.invoke(episode)
                                                    return@onKeyEvent true
                                                }
                                                return@onKeyEvent false
                                            },
                                    interactionSource = interactionSource,
                                    cardHeight = 120.dp,
                                    useSeriesForPrimary = false,
                                )
                            }
                        }
                    }
//                    }
                }

                focusedEpisode?.let { ep ->
                    FocusedEpisodeFooter(
                        preferences = preferences,
                        ep = ep,
                        chosenStreams = chosenStreams,
                        playOnClick = playOnClick,
                        moreOnClick = moreOnClick,
                        watchOnClick = {
                            watchOnClick.invoke()
                            focusTarget = SeriesFocusTarget.Episode(selectedTabIndex)
                            focusRequestVersion++
                        },
                        favoriteOnClick = favoriteOnClick,
                        buttonOnFocusChanged = {
                            if (it.isFocused) {
                                scope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                        canDelete = canDelete.invoke(ep),
                        onConfirmDelete = { onConfirmDelete.invoke(ep) },
                        onChooseVersion = { onChooseVersion.invoke(ep, it) },
                        modifier =
                            Modifier
                                .padding(top = 4.dp)
                                .fillMaxWidth(),
                    )
                }
            }

            AnimatedVisibility(
                visible = peopleInEpisode.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                if (preferences.appPreferences.interfacePreferences.showPeopleImages) {
                    val (guestStars, castAndCrew) =
                        remember(peopleInEpisode) {
                            peopleInEpisode.partition { it.type == PersonKind.GUEST_STAR }
                        }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (castAndCrew.isNotEmpty()) {
                            PersonRow(
                                title = R.string.cast_and_crew,
                                people = castAndCrew,
                                showImages = true,
                                onClick = personOnClick,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged {
                                            if (it.hasFocus) focusTarget = SeriesFocusTarget.External
                                        },
                                focusRequester = castCrewRowFocusRequester,
                            )
                        }
                        if (guestStars.isNotEmpty()) {
                            PersonRow(
                                title = R.string.guest_stars,
                                people = guestStars,
                                showImages = true,
                                onClick = personOnClick,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged {
                                            if (it.hasFocus) focusTarget = SeriesFocusTarget.External
                                        },
                                focusRequester = guestStarRowFocusRequester,
                            )
                        }
                    }
                } else {
                    PersonRow(
                        title = R.string.cast_and_crew,
                        people = peopleInEpisode,
                        showImages = false,
                        onClick = personOnClick,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .onFocusChanged {
                                    if (it.hasFocus) focusTarget = SeriesFocusTarget.External
                                },
                        focusRequester = castCrewRowFocusRequester,
                    )
                }
            }
            AnimatedVisibility(
                visible = seasonExtras.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ExtrasRow(
                    extras = seasonExtras,
                    onClickItem = onClickExtra,
                    onLongClickItem = { _, _ -> },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onFocusChanged {
                                if (it.hasFocus) focusTarget = SeriesFocusTarget.External
                            }
                            .focusRequester(extrasRowFocusRequester),
                )
            }
        }
    }
}
