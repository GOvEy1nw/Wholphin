---
id: wphn-009
title: Make initial and changed season episode targeting deterministic
status: Human Review
assignee:
  - '@codex'
created_date: '2026-08-29'
updated_date: '2026-08-30 14:24'
labels:
  - android
  - series
  - tests
dependencies:
  - wphn-001
modified_files:
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/detail/series/SeriesViewModel.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/detail/series/SeriesOverview.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/detail/series/SeriesOverviewContent.kt
  - >-
    app/src/test/java/com/github/damontecres/wholphin/test/SeriesTargetSelectionTest.kt
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Fix the ViewModel/data logic before changing the season UI. Entering a show should select its meaningful next/unwatched episode, while changing season should select that season’s first unwatched episode. Old requests must never overwrite the newest selection.

## Goal

Produce one resolved `(season, episode)` target and a race-safe season-loading path independent of Compose focus timing.

## Files to inspect

- `ui/detail/series/SeriesViewModel.kt`
- `ui/detail/series/SeriesOverview.kt`
- `data/model/BaseItem.kt` episode/season destinations
- `services/LatestNextUpService.kt`
- Jellyfin SDK TV Shows/Next Up APIs already used in the project
- `ApiRequestPager` and blocking-list helpers

## Implementation

1. Preserve explicit `SeasonEpisodeIds` as the highest-priority target.
2. When no explicit target exists, query Jellyfin’s series-specific Next Up endpoint with `seriesId` and a limit of one. Use the returned episode’s season/episode IDs and numbers.
3. If Next Up is empty:
   - request seasons with user data enabled so unplayed counts are available;
   - choose the first season with `unplayedItemCount > 0`;
   - otherwise choose the first season.
4. When loading the chosen season, find its first episode whose `played` state is not true. If none, index zero.
5. For a manual season change, always apply the first-unwatched/index-zero rule; do not carry an episode index from the previous season.
6. Represent the selected target in one state update so tab index, season ID, episode index, focused header, and chosen-track lookup cannot disagree.
7. Add retained load jobs or a monotonically increasing generation token:
   - cancel previous episode and extras loads;
   - verify season/generation before committing each result;
   - cancellation is not logged as an error.
8. Keep explicit episode navigation able to locate a paged episode by ID/number.
9. Do not add a local watch-history database or calculate “in-progress season” independently when Jellyfin Next Up already supplies the answer.
10. Add small tests around a pure target-selection helper and stale-commit guard:
    - explicit target wins;
    - Next Up wins;
    - first unplayed season/episode fallback;
    - fully watched fallback to first;
    - stale generation cannot commit.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Explicit episode links still open the requested episode.
- [x] #2 Normal series entry prefers series Next Up.
- [x] #3 Fallback chooses first season/episode with unplayed content.
- [x] #4 Fully watched/no-progress show falls back to first season/episode.
- [x] #5 Changed season targets its first unwatched episode or first episode.
- [x] #6 Stale episode and extras responses cannot alter current state.
- [x] #7 Tests cover selection precedence and stale result protection.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add pure initial-target, first-unwatched episode, and stale-generation guards around the existing SeriesViewModel pager path. 2. Make SeriesState own the selected position and commit season/episode/extras results only for the current generation, preserving explicit paged episode lookup and using series-specific Next Up before unplayed fallbacks. 3. Update SeriesOverview to use the ViewModel-owned atomic position, add the task-specified focused tests, run only those tests plus :app:compileDefaultDebugKotlin, independently review the diff, finalize WPHN-009, and create one focused commit.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented deterministic series targeting in SeriesViewModel and atomic selection consumption in SeriesOverview and SeriesOverviewContent. Files: SeriesViewModel.kt, SeriesOverview.kt, SeriesOverviewContent.kt, and SeriesTargetSelectionTest.kt. Initial precedence is explicit SeasonEpisodeIds, series-specific Next Up with limit 1, first season with unplayedItemCount, then first season. Selected and manual seasons use first played-not-true episode or index 0. Existing paged ID and number lookup remains. Episode, extras, refresh, and chosen-stream commits are guarded by current generation, season, and episode; cancellation is excluded from error logging. Verification: focused SeriesTargetSelectionTest BUILD SUCCESSFUL after final corrections and executed compileDefaultDebugKotlin; focused TestFindIndexByNumberOrId BUILD SUCCESSFUL and executed compileDefaultDebugKotlin; git diff --check passed. Independent final review verdict: ship with no findings. Deviations: no separate compile-only rerun because each focused Gradle run executed compileDefaultDebugKotlin; no device or UI run because WPHN-009 is ViewModel and data logic and WPHN-010 owns season interaction timing. The initial combined Gradle invocation misapplied the test filter and hit the known problems-report access issue; separate approved runs recovered conclusive results. Existing unrelated compiler warnings were not repaired.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Made WPHN-009 season and episode targeting deterministic through the existing SeriesViewModel and pager path. Explicit links and series Next Up take precedence, unplayed fallbacks are stable, manual season changes choose first unwatched, and stale episode, extras, refresh, or chosen-stream results cannot commit. Focused target and guard tests plus explicit paging tests pass, default-debug Kotlin compiles, and independent review returned ship.
<!-- SECTION:FINAL_SUMMARY:END -->

## Verification

```powershell
.\gradlew.bat :app:testDefaultDebugUnitTest
.\gradlew.bat :app:assembleDefaultDebug
```

## Out of scope

- Moving season tabs.
- Focus dwell UI.
- Redesigning episode cards.
