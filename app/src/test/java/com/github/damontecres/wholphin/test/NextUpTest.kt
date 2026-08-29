package com.github.damontecres.wholphin.test

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.github.damontecres.wholphin.preferences.AppPreference
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.preferences.updateHomePagePreferences
import com.github.damontecres.wholphin.services.DatePlayedService
import com.github.damontecres.wholphin.services.DisplayPreferencesService
import com.github.damontecres.wholphin.services.FavoriteWatchManager
import com.github.damontecres.wholphin.services.LatestNextUpService
import com.github.damontecres.wholphin.services.mockQueryResult
import com.github.damontecres.wholphin.services.testDisplayPreferencesDto
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.operations.ItemsApi
import org.jellyfin.sdk.api.operations.TvShowsApi
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.request.GetNextUpRequest
import org.jellyfin.sdk.model.api.request.GetResumeItemsRequest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class NextUpTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private val mockTvShowsApi = mockk<TvShowsApi>()
    private val mockItemsApi = mockk<ItemsApi>()
    private val mockApi = mockk<ApiClient>(relaxed = true)
    private val mockDatePlayedService = mockk<DatePlayedService>()
    private val mockDisplayPreferencesService = mockk<DisplayPreferencesService>()
    private val mockFavoriteWatchManager = mockk<FavoriteWatchManager>(relaxed = true)

    private val latestNextUpService =
        LatestNextUpService(
            mockApi,
            mockDatePlayedService,
            mockDisplayPreferencesService,
            mockFavoriteWatchManager,
        )

    @Before
    fun setUp() {
        every { mockApi.tvShowsApi } returns mockTvShowsApi
        every { mockApi.itemsApi } returns mockItemsApi
        coEvery {
            mockDisplayPreferencesService.getDisplayPreferences(
                any(),
                any(),
                any(),
            )
        } returns testDisplayPreferencesDto
    }

    @Test
    fun `Test max 30 days in next up`() =
        runTest {
            val maxDays = 30
            val nextUpSlot = CapturingSlot<GetNextUpRequest>()
            coEvery { mockTvShowsApi.getNextUp(capture(nextUpSlot)) } returns mockQueryResult()
            latestNextUpService.getNextUp(
                userId = UUID.randomUUID(),
                limit = 10,
                enableRewatching = true,
                enableResumable = true,
                maxDays = maxDays,
            )
            Assert.assertEquals(10, nextUpSlot.captured.limit)
            val expected = LocalDate.now().minusDays(maxDays.toLong())
            Assert.assertEquals(expected, nextUpSlot.captured.nextUpDateCutoff?.toLocalDate())
        }

    @Test
    fun `Test no limit in next up`() =
        runTest {
            val nextUpSlot = CapturingSlot<GetNextUpRequest>()
            coEvery { mockTvShowsApi.getNextUp(capture(nextUpSlot)) } returns mockQueryResult()
            latestNextUpService.getNextUp(
                userId = UUID.randomUUID(),
                limit = 10,
                enableRewatching = true,
                enableResumable = true,
                maxDays = -1,
            )
            Assert.assertEquals(10, nextUpSlot.captured.limit)
            Assert.assertNull(nextUpSlot.captured.nextUpDateCutoff)
        }

    @Test
    fun `Watching requests use optional library scope`() =
        runTest {
            val parentId = UUID.randomUUID()
            val resumeSlot = CapturingSlot<GetResumeItemsRequest>()
            val nextUpSlot = CapturingSlot<GetNextUpRequest>()
            coEvery { mockItemsApi.getResumeItems(capture(resumeSlot)) } returns mockQueryResult()
            coEvery { mockTvShowsApi.getNextUp(capture(nextUpSlot)) } returns mockQueryResult()

            latestNextUpService.getResume(UUID.randomUUID(), 10, true, parentId = parentId)
            latestNextUpService.getNextUp(UUID.randomUUID(), 10, true, true, -1, parentId = parentId)

            Assert.assertEquals(parentId, resumeSlot.captured.parentId)
            Assert.assertEquals(parentId, nextUpSlot.captured.parentId)
        }

    @Test
    fun `Test storing preference`() {
        AppPreference.MaxDaysNextUp.setter.invoke(AppPreferences.getDefaultInstance(), 0).let {
            Assert.assertEquals(7, it.homePagePreferences.maxDaysNextUp)
        }

        AppPreference.MaxDaysNextUp.setter
            .invoke(
                AppPreferences.getDefaultInstance(),
                AppPreference.MaxDaysNextUpOptions.lastIndex.toLong(),
            ).let {
                Assert.assertEquals(365, it.homePagePreferences.maxDaysNextUp)
            }

        AppPreference.MaxDaysNextUp.setter
            .invoke(AppPreferences.getDefaultInstance(), 3)
            .let {
                Assert.assertEquals(60, it.homePagePreferences.maxDaysNextUp)
            }

        AppPreference.MaxDaysNextUp.setter
            .invoke(
                AppPreferences.getDefaultInstance(),
                AppPreference.MaxDaysNextUpOptions.lastIndex + 1L,
            ).let {
                Assert.assertEquals(-1, it.homePagePreferences.maxDaysNextUp)
            }
    }

    @Test
    fun `Test getting preference`() {
        AppPreferences
            .getDefaultInstance()
            .updateHomePagePreferences { maxDaysNextUp = 7 }
            .let {
                val result = AppPreference.MaxDaysNextUp.getter.invoke(it)
                Assert.assertEquals(0, result)
            }

        AppPreferences
            .getDefaultInstance()
            .updateHomePagePreferences { maxDaysNextUp = 60 }
            .let {
                val result = AppPreference.MaxDaysNextUp.getter.invoke(it)
                Assert.assertEquals(3, result)
            }

        AppPreferences
            .getDefaultInstance()
            .updateHomePagePreferences { maxDaysNextUp = -1 }
            .let {
                val result = AppPreference.MaxDaysNextUp.getter.invoke(it)
                Assert.assertEquals(AppPreference.MaxDaysNextUpOptions.lastIndex + 1L, result)
            }
    }
}
