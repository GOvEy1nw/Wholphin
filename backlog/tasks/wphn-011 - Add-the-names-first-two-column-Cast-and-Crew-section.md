---
id: wphn-011
title: Add the names-first two-column Cast and Crew section
status: Done
assignee:
  - '@codex'
created_date: '2026-08-29'
updated_date: '2026-08-30 19:08'
labels:
  - android
  - people
  - focus
  - settings
  - tests
dependencies:
  - wphn-001
modified_files:
  - app/src/main/java/com/github/damontecres/wholphin/data/model/Person.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/preferences/AppPreference.kt
  - app/src/main/java/com/github/damontecres/wholphin/ui/cards/PersonRow.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/detail/movie/MovieDetails.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/detail/series/SeriesDetails.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/detail/series/SeriesOverviewContent.kt
  - app/src/main/proto/WholphinDataStore.proto
  - app/src/main/res/values/strings.xml
  - app/src/test/java/com/github/damontecres/wholphin/data/model/PersonTest.kt
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Replace default portrait-card person rows with a text-first Cast/Crew table while preserving the current image row behind an opt-in preference.

## Goal

Apply the agreed presentation consistently anywhere Wholphin currently renders people, without duplicating person queries or person-detail navigation.

## Implementation

1. Add `Show people images`, default false, through the existing interface-preference proto/settings path.
2. Keep the existing PersonRow/PersonCard image implementation as the enabled branch.
3. Partition the current people list while preserving server order: actor, guest-star, artist, and album-artist kinds are Cast; all other kinds are Crew.
4. Render Cast and Crew as two labeled groups with two equal-width subcolumns each. Show all entries; person name and role/job are each one line with ellipsis.
5. Use direct person focus. Bind the caller requester to first Cast or first Crew, route Up/Down within a subcolumn, and route Left/Right to the nearest valid row while skipping empty subcolumns.
6. Keep the table within the existing detail-content inset and use the containing detail page for scrolling.
7. Route all PersonRow call sites through the shared preference-aware composable. Combine episode Guest Stars into Cast only in text mode; preserve separate rows in image mode.
8. Add the focused classification test; validate focus geometry manually.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 New installs default to no person images/icons.
- [x] #2 Text section has Cast and Crew groups with two equal-width subcolumns each and secondary role/job text.
- [x] #3 All returned people are shown with one-line ellipsis when text exceeds a column.
- [x] #4 Person entries are directly focusable without an outer section focus stop.
- [x] #5 The caller focus requester targets first Cast, otherwise first Crew.
- [x] #6 Cross-column navigation skips empty subcolumns and targets the nearest valid index.
- [x] #7 Person OK still opens the existing person page.
- [x] #8 Enabling Show people images restores existing image-card behaviour.
- [x] #9 All current PersonRow call sites use the shared preference-aware path.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add the false-default Show people images preference through existing proto/settings paths. 2. Extend shared PersonRow with a text mode that partitions Cast/Crew, renders each group in two equal-width subcolumns, and keeps the existing image-card branch. 3. Bind caller focus directly to first Cast or first Crew and provide deterministic direct D-pad navigation across all four person columns. 4. Route all PersonRow callers through the preference-aware API, preserve separate episode image rows, add the focused classification test, compile, and validate on the TV emulator.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented the shared preference-aware PersonRow path. Text mode partitions performer kinds into Cast and every other kind into Crew while preserving server order, then renders four equal-width person columns (two per group) with one-line ellipsized person and role/job text. Per the final review direction, person cells are directly focusable; the caller requester targets first Cast or first Crew and explicit D-pad routing skips empty subcolumns and clamps to the nearest valid row. The table is inset 16dp to the episode/content bounds. Existing portrait-card rows remain unchanged behind Show people images (proto field 16, false default); episode Guest Stars remain separate only in image mode.

Files: Person.kt, AppPreference.kt, WholphinDataStore.proto, strings.xml, PersonRow.kt, MovieDetails.kt, SeriesDetails.kt, SeriesOverviewContent.kt, and PersonTest.kt.

Verification: `gradlew :app:testDefaultDebugUnitTest --tests com.github.damontecres.wholphin.data.model.PersonTest :app:assembleDefaultDebug` passed after the final direct-focus revision. Earlier exact `gradlew :app:testDefaultDebugUnitTest` ran 360 tests with only the four known pre-existing ServerRepositoryTest FileStorage IOException failures; no unrelated repair was attempted. `git diff --check` passed. Android TV emulator evidence confirmed four equal columns clear of the nav rail, direct focus on Fernando Lindez, Right to Juan Gabriel Roig, and OK opened the existing Juan Gabriel Roig person page. A second Futurama journey confirmed index-clamped Cast-to-Crew navigation. Independent review returned ship with no findings.

Deviation: the original outer-container/OK/internal-mode acceptance was explicitly superseded during review by direct person focus, matching the prior person-row interaction model.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
author: @user
created: 2026-08-30 19:08
---
Accepted by the user; implementation approved for publication.
---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added the default names-first Cast/Crew table through shared PersonRow, with four equal-width direct-focus columns, one-line ellipsized names/roles, stable classification, and an opt-in Show people images fallback. Verified by focused unit test, final APK assembly, diff check, Android TV focus/person-navigation journeys, and independent review; the full unit suite retains only four known pre-existing ServerRepositoryTest I/O failures.
<!-- SECTION:FINAL_SUMMARY:END -->

## Verification

```powershell
.\gradlew.bat :app:testDefaultDebugUnitTest
.\gradlew.bat :app:assembleDefaultDebug
```

Manually test sparse columns, Crew-only, Cast-only, long names, and all Cast/Crew focus scenarios.

## Out of scope

- New person metadata queries.
- View All page.
- Image redesign.
