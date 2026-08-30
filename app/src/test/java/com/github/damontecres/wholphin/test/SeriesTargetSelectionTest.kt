package com.github.damontecres.wholphin.test

import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.ui.detail.series.InitialSeasonTarget
import com.github.damontecres.wholphin.ui.detail.series.SeasonTargetCandidate
import com.github.damontecres.wholphin.ui.detail.series.canCommitChosenStreams
import com.github.damontecres.wholphin.ui.detail.series.canCommitSeasonLoad
import com.github.damontecres.wholphin.ui.detail.series.firstUnwatchedEpisodeIndex
import com.github.damontecres.wholphin.ui.detail.series.selectInitialSeasonTarget
import com.github.damontecres.wholphin.util.BlockingList
import kotlinx.coroutines.test.runTest
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.UserItemDataDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesTargetSelectionTest {
    private fun target() = InitialSeasonTarget(UUID.randomUUID(), 1, UUID.randomUUID(), 1)

    @Test
    fun `explicit target wins over Next Up and unplayed seasons`() {
        val explicit = target()

        assertEquals(
            explicit,
            selectInitialSeasonTarget(
                explicit,
                target(),
                listOf(SeasonTargetCandidate(target(), 1)),
            ),
        )
    }

    @Test
    fun `Next Up wins over unplayed season fallback`() {
        val nextUp = target()

        assertEquals(
            nextUp,
            selectInitialSeasonTarget(null, nextUp, listOf(SeasonTargetCandidate(target(), 1))),
        )
    }

    @Test
    fun `fallback selects first unplayed season and episode`() =
        runTest {
            val firstUnplayed = target()
            assertEquals(
                firstUnplayed,
                selectInitialSeasonTarget(
                    null,
                    null,
                    listOf(SeasonTargetCandidate(target(), 0), SeasonTargetCandidate(firstUnplayed, 2)),
                ),
            )
            assertEquals(1, firstUnwatchedEpisodeIndex(BlockingList.of(listOf(episode(true), episode(false)))))
        }

    @Test
    fun `fully watched fallback selects first season and episode`() =
        runTest {
            val firstSeason = target()
            assertEquals(
                firstSeason,
                selectInitialSeasonTarget(
                    null,
                    null,
                    listOf(SeasonTargetCandidate(firstSeason, 0), SeasonTargetCandidate(target(), 0)),
                ),
            )
            assertEquals(0, firstUnwatchedEpisodeIndex(BlockingList.of(listOf(episode(true), episode(true)))))
        }

    @Test
    fun `stale generation cannot commit`() {
        val seasonId = UUID.randomUUID()
        val episodeId = UUID.randomUUID()

        assertTrue(canCommitSeasonLoad(2, 2, seasonId, seasonId))
        assertFalse(canCommitSeasonLoad(2, 1, seasonId, seasonId))
        assertFalse(canCommitSeasonLoad(2, 2, UUID.randomUUID(), seasonId))
        assertTrue(canCommitChosenStreams(seasonId, seasonId, episodeId, episodeId))
        assertFalse(canCommitChosenStreams(seasonId, seasonId, UUID.randomUUID(), episodeId))
        assertFalse(canCommitChosenStreams(UUID.randomUUID(), seasonId, episodeId, episodeId))
    }

    private fun episode(played: Boolean) =
        BaseItem(
            BaseItemDto(
                id = UUID.randomUUID(),
                type = BaseItemKind.EPISODE,
                userData =
                    UserItemDataDto(
                        playbackPositionTicks = 0L,
                        playCount = 0,
                        isFavorite = false,
                        lastPlayedDate = null,
                        played = played,
                        key = "",
                        itemId = UUID.randomUUID(),
                    ),
            ),
        )
}
