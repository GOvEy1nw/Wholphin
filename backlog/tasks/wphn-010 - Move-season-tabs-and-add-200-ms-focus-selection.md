---
id: wphn-010
title: Move season tabs and add 200 ms focus selection
status: Ready
assignee: []
created_date: "2026-08-29"
labels:
  - android
  - series
  - focus
dependencies:
  - wphn-009
priority: high
---

## Description

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

## Acceptance criteria

- [ ] Selector is below the header and above episodes.
- [ ] 200 ms focus dwell changes season.
- [ ] Focus remains on tabs during focus-driven loads.
- [ ] Down/OK enters the first-unwatched/first episode target.
- [ ] Rapid traversal neither commits stale UI nor throws focus into episodes.
- [ ] Other TabRow users retain current behaviour.

## Verification

```powershell
.\gradlew.bat :app:assembleDefaultDebug
```

Run all Season scenarios in `architecture/verification-matrix.md`.

## Out of scope

- Season artwork.
- User-configurable dwell.
- Episode card visual redesign.
