---
id: wphn-010
title: Move season tabs and add 200 ms focus selection
status: Done
assignee:
  - '@codex'
created_date: '2026-08-29'
updated_date: '2026-08-30 17:40'
labels:
  - android
  - series
  - focus
dependencies:
  - wphn-009
modified_files:
  - app/src/main/java/com/github/damontecres/wholphin/ui/Extensions.kt
  - app/src/main/java/com/github/damontecres/wholphin/ui/components/TabRow.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/detail/series/SeriesOverview.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/detail/series/SeriesOverviewContent.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/detail/series/SeriesViewModel.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/nav/DestinationContent.kt
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Place the season selector between the spotlight/header and episode row, then select seasons after a cancellable 200 ms focus dwell without moving focus into episodes.

## Goal

Make left/right season browsing behave like a TV-native preview selector while using wphn-009’s deterministic targets and race protection.

## Files to inspect

- `ui/detail/series/SeriesOverviewContent.kt`
- `ui/detail/series/SeriesOverview.kt`
- `ui/components/TabRow.kt`
- current focus requesters and `requestFocusAfterSeason` logic

## Implementation

1. Reorder composition to:

   ```text
   Title/logo and focused episode header
   Season selector
   Episode row
   Focused episode controls
   Later sections
   ```

2. Add `SEASON_PREVIEW_DELAY_MS = 200L` as a code constant.
3. Extend the existing tab primitives with the smallest optional focus callback needed by the season row. Do not fork a full second Tab component.
4. When a season tab remains focused for 200 ms:
   - update selected tab;
   - call the ViewModel season change;
   - keep tab focus;
   - show loading/current episode state without requesting episode focus.
5. Split focus selection from click activation. Existing `requestFocusAfterSeason` behaviour should occur only after OK/explicit entry, not after focus preview.
6. When the new episode target arrives, update which episode card will receive focus on Down/OK using wphn-009’s index.
7. OK on a season:
   - trigger selection immediately if not already selected;
   - enter the target episode when ready.
8. Ensure fast left/right movement cancels each previous 200 ms action before it reaches the ViewModel.
9. Keep tab bring-into-view, selected indicator, and clock padding behaviour.
10. Remove obsolete index/focus code created redundant by the new single target; do not refactor unrelated tab users.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Selector is below the header and above episodes.
- [x] #2 200 ms focus dwell changes season.
- [x] #3 Focus remains on tabs during focus-driven loads.
- [x] #4 Down/OK enters the first-unwatched/first episode target.
- [x] #5 Rapid traversal neither commits stale UI nor throws focus into episodes.
- [x] #6 Other TabRow users retain current behaviour.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Replace the series page's row/index restoration flags with one local Season-or-Episode focus target. 2. Route initial entry, Up/Down, season dwell, explicit season activation, and episode focus through that target; allow only one effect to issue focus requests after the requested target exists. 3. Remove async-load and TabRow focus-reclaim patches that compete with the coordinator while preserving other TabRow callers. 4. Run the focused target test, compile/install on emulator-5554, exercise repeated and rapid season changes plus row transitions/playback, obtain independent review, update WPHN-010, and amend the single unpushed commit.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented WPHN-010 in app/src/main/java/com/github/damontecres/wholphin/ui/components/TabRow.kt and app/src/main/java/com/github/damontecres/wholphin/ui/detail/series/SeriesOverviewContent.kt. TabRow now exposes one default-no-op focused-index callback; SeriesOverviewContent places TitleOrLogo and FocusedEpisodeHeader before the selector, runs a keyed cancellable 200 ms dwell, preserves explicit OK as the only requestFocusAfterSeason path, and cancels pending dwell before OK selection. Verification: git diff --check passed; :app:testDefaultDebugUnitTest filtered to SeriesTargetSelectionTest passed and executed compileDefaultDebugKotlin; final :app:compileDefaultDebugKotlin passed after review correction; :app:installDefaultDebug built and installed Wholphin-default-debug-1.0.7-21-ga69b4a72-51.apk on emulator-5554; independent final review verdict ship. Existing manifest, KSP foreign-key-index, deprecation, and Gradle warnings were not repaired. Deviation: manual Seasons checks could not run because Futurama SeriesOverview remained loading for over 20 seconds across two season entries; filtered logcat had no decisive error. Acceptance criteria remain unchecked because interactive evidence was unavailable.

Human review rejected the split TV-show flow. Required correction: one series view containing series/episode spotlight, text season row, and episode thumbnails; season focus shows series overview, episode focus shows episode overview; untouched shows initially focus seasons, progressed shows initially focus the next-unwatched episode; season focus switches episode rows without OK; episode OK plays. Current regressions: Go To Series still exposes poster seasons, and series/episode entry can remain loading indefinitely. Investigation resumed before further edits.

Corrective implementation after human review: unified BaseItemKind.SERIES and episode/Next Up entry paths on SeriesOverview; overview season loading now uses the lean known-working query while Next Up resolves concurrently; season-row focus shows series metadata, episode-row focus shows episode metadata; initial focus uses the resolved episode target; and explicit vertical D-pad links retain TV row navigation. Files: app/src/main/java/com/github/damontecres/wholphin/ui/nav/DestinationContent.kt, ui/detail/series/SeriesViewModel.kt, SeriesOverview.kt, SeriesOverviewContent.kt, ui/components/TabRow.kt, and ui/Extensions.kt. Checks: git diff --check passed; .\\gradlew.bat :app:testDefaultDebugUnitTest --tests com.github.damontecres.wholphin.test.SeriesTargetSelectionTest passed; .\\gradlew.bat :app:installDefaultDebug passed compilation and installed Wholphin-default-debug-1.0.7-22-g342a009b-51.apk on emulator-5554. Runtime evidence: episode entry reached SeriesOverview without a loading hang; Up moved E1 to Season 11; rapid Left x3 retained tab focus on Season 8 and loaded S8 E1; Down focused E1; OK navigated to Playback and selected EXO_PLAYER. Existing manifest, KSP foreign-key-index, deprecation, and Gradle warnings were not repaired. No deviations from corrected WPHN-010 scope.

Independent final review initially found two edge cases; corrected by restoring season user data for the next-unwatched fallback and guarding tab refocus when the user has left the season row. Re-review verdict: ship. Final post-review verification: focused SeriesTargetSelectionTest passed; installDefaultDebug compiled and installed on emulator-5554; explicit episode entry again rendered both season and episode rows without hanging. One attempted combined Gradle invocation failed before execution because --tests is not valid for installDefaultDebug; commands were rerun separately and passed.

Human acceptance: approved by the user on 2026-08-30; authorized to push.

Human acceptance withdrawn before push: season changes again move focus to the navigation/profile button. Push was stopped; regression investigation resumed.

Series-local focus refactor: replaced competing page/row/load focus requests with one SeriesFocusTarget coordinator and one LaunchedEffect that performs explicit focus. The inline episode LoadingPage is now non-focusable; this was the reproduced focus thief during season loads. TabRow exposes an opt-in onDown callback so the series coordinator owns the row transition; other callers remain unchanged. Removed page-level RequestOrRestoreFocus and direct post-load/footer focus requests. Verification: focused SeriesTargetSelectionTest passed; installDefaultDebug compiled and installed the final APK on emulator-5554; six alternating season changes retained season focus, rapid Left x3 retained the final season and correct episode row, Down/Up moved E1/selected season, Season OK entered E1, Mark watched returned focus to E1, and the watched state was restored to unwatched. Independent final re-review verdict: ship. Existing build warnings were not repaired.

Final human acceptance: series-local focus refactor approved; push authorized.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Unified the TV-show series view and replaced its competing focus mechanisms with one series-local Season/Episode coordinator. Inline loading no longer takes focus, repeated and rapid season changes stay on the selected tab, row transitions target the resolved episode, and episode playback/footer actions retain deterministic focus. Verified with focused tests, compilation/install, repeated emulator D-pad flows, and independent review.
<!-- SECTION:FINAL_SUMMARY:END -->

## Verification

```powershell
.\gradlew.bat :app:assembleDefaultDebug
```

Run all Season scenarios in `architecture/verification-matrix.md`.

## Out of scope

- Season artwork.
- User-configurable dwell.
- Episode card visual redesign.
