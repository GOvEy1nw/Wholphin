---
id: APP-009
title: Make initial and changed season episode targeting deterministic
status: To Do
assignee: []
created_date: '2026-08-29'
labels:
  - android
  - series
  - tests
dependencies: 
  - APP-001
priority: high
---

## Description

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

## Acceptance criteria

- [ ] Explicit episode links still open the requested episode.
- [ ] Normal series entry prefers series Next Up.
- [ ] Fallback chooses first season/episode with unplayed content.
- [ ] Fully watched/no-progress show falls back to first season/episode.
- [ ] Changed season targets its first unwatched episode or first episode.
- [ ] Stale episode and extras responses cannot alter current state.
- [ ] Tests cover selection precedence and stale result protection.

## Verification

```powershell
.\gradlew.bat :app:testDefaultDebugUnitTest
.\gradlew.bat :app:assembleDefaultDebug
```

## Out of scope

- Moving season tabs.
- Focus dwell UI.
- Redesigning episode cards.
