---
id: wphn-005
title: Extend the shared row engine for library hubs
status: Human Review
assignee:
  - '@codex'
created_date: '2026-08-29'
updated_date: '2026-08-29 19:30'
labels:
  - android
  - rows
  - library-hub
  - tests
dependencies:
  - wphn-001
modified_files:
  - >-
    app/src/main/java/com/github/damontecres/wholphin/data/model/HomeRowConfig.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/services/HomeSettingsService.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/services/LatestNextUpService.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/main/settings/HomeSettingsViewModel.kt
  - app/src/test/java/com/github/damontecres/wholphin/test/NextUpTest.kt
  - app/src/test/java/com/github/damontecres/wholphin/test/TestHomeRowSamples.kt
  - docs/rais-stream/plan/fixtures/verify-jellyfin-12-rc6-collections.ps1
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add only the row vocabulary and request scoping that the approved fallback and plugin-managed library hubs require. Keep all data fetching in `HomeSettingsService` and existing request services.

## Goal

Allow Home and library hubs to share one row engine without custom per-hub queries.

## Files to inspect

- `data/model/HomeRowConfig.kt`
- `services/HomeSettingsService.kt`
- `services/LatestNextUpService.kt`
- `ui/components/RecommendedMovie.kt`
- `ui/components/RecommendedTvShow.kt`
- current request handlers and `HomeItemFields`

## Implementation

1. Add an optional `parentId` to:
   - `ContinueWatching`
   - `NextUp`
   - `ContinueWatchingCombined`

   Existing serialized rows omit/null it and retain global Home behaviour.

2. Thread optional `parentId` through `LatestNextUpService.getResume()` and `getNextUp()` to the underlying Jellyfin requests. Update every caller once at the shared function boundary.
3. Add serializable row types:

   ```kotlin
   HomeRowConfig.TopUnwatched(parentId, viewOptions)
   HomeRowConfig.Collections(parentId, viewOptions)
   ```

4. Implement `TopUnwatched` with the same media type, recursive scope, rating sort, user data, and useful fields currently used by Recommended Movie/TV. Infer the item type from the known library record; do not duplicate a second `kind` field unless the real request cannot be resolved safely.
5. Implement `Collections` by first reusing the same Jellyfin 12 query shape as the existing Movie Collections tab:
   - parent library ID;
   - recursive;
   - include `BOX_SET`;
   - user-visible results;
   - existing card fields.
6. Add a focused integration/manual fixture against Jellyfin 12.0.0-rc6 proving that the scoped query:
   - returns a TV collection containing a series from the current TV library;
   - keeps a mixed collection when it has a qualifying series;
   - excludes a collection unrelated to that library.
7. Only if the RC6 query fails that acceptance test, add the smallest membership-filter fallback. Do not pre-emptively add an N+1 collection scanner.
8. Add resolution titles and View More handling through the existing `HomeRowLoadingState.Success` path.
9. Keep `Studios` model support for old/local settings, but do not add it to any new fallback or managed profile UI.
10. Keep local `HomePageSettings.version` unchanged. The managed envelope has its own version boundary.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Existing global Home rows behave exactly as before when `parentId` is null.
- [x] #2 Hub watching rows are scoped to their library.
- [x] #3 Top Unwatched uses the current library and correct media kind.
- [x] #4 Movie and TV Collections return only qualifying collections on Jellyfin 12.0.0-rc6.
- [x] #5 Mixed TV collections remain when qualifying.
- [x] #6 All row types use the existing fetch/resolve pipeline.
- [x] #7 No second row service or arbitrary query abstraction is added.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Extend `HomeRowConfig` in place: add nullable `parentId` defaults to the three watching rows, add serializable `TopUnwatched` and `Collections`, preserve `HomePageSettings.version`, and update the existing exhaustive row sample/round-trip test.
2. Extend `LatestNextUpService.getResume()` and `getNextUp()` with a trailing nullable `parentId`; pass it into the generated Jellyfin requests. Keep existing callers global by default, pass row scope from `HomeSettingsService`, and preserve that scope through existing Combine/Split settings actions.
3. Add `TopUnwatched` and `Collections` branches to `HomeSettingsService.fetchDataForRow()` and `resolve()`. Reuse the known `Library` record, the existing Movie/TV-only `SuggestionsWorker.getTypeForCollection` resolver, `SlimItemFields`, `GetItemsRequestHandler`, `ApiRequestPager`, and `HomeRowLoadingState.Success`; leave generic video libraries type-unfiltered and add no new service or general query abstraction.
4. Capture scoped resume/Next Up requests in the nearest existing test, keep exhaustive row serialization/legacy-null coverage, and add a small manual RC6 collection-query fixture that asserts qualifying TV, qualifying mixed, and unrelated collection IDs using the exact single scoped BOX_SET query.
5. Run the focused WPHN-005 tests plus `:app:compileDefaultDebugKotlin`, and run the manual fixture against the exact official Jellyfin 12.0.0-rc6 image.
6. Inspect the complete focused diff, obtain independent runtime-code review, record commands/results/deviations, move the task to `Human Review`, and create one WPHN-005 commit.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented the shared-row extension in the existing model, Latest/Next Up service, HomeSettingsService resolve/fetch pipeline, and existing settings preset/action paths. Added nullable parentId defaults to watching rows; request scoping for resume and Next Up; serializable TopUnwatched and Collections rows; safe Movie/TV kind inference with generic video libraries left type-unfiltered; generic Success/View More support; and scope-preserving Combine/Split behavior. Added exhaustive serialization/legacy-null coverage, scoped request capture, and a reusable RC6 PowerShell assertion fixture.

Verification:
- `./gradlew.bat :app:testDefaultDebugUnitTest --tests com.github.damontecres.wholphin.test.TestHomeRowSamples --tests com.github.damontecres.wholphin.test.NextUpTest` — BUILD SUCCESSFUL.
- `./gradlew.bat :app:compileDefaultDebugKotlin` — BUILD SUCCESSFUL.
- Exact official image `jellyfin/jellyfin:12.0-rc6-amd64.20260826-011825`; disposable localhost fixture with Current TV, Other TV, and Movies libraries plus TV-only, mixed, and unrelated collections. `verify-jellyfin-12-rc6-collections.ps1` passed: TV-only and mixed retained; unrelated excluded.
- Focused diff review found two scope/kind issues; both were corrected, reverified, and the reviewer returned ship.

Deviations: the initial plan noted RC6 might remain unavailable, but Docker access was recovered and the exact-version fixture was completed. No membership-filter fallback was added because the single scoped RC6 query passed. Pre-existing Gradle/KSP/deprecation warnings were recorded but not repaired; no broad suite was run per task instruction.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Extends the existing shared Home/library row engine with library-scoped Continue Watching/Next Up rows plus Top Unwatched and Collections. Existing serialized watching rows remain global through nullable defaults, all fetching stays in HomeSettingsService/current request handlers, settings actions preserve scopes, and generic video libraries avoid an unsafe folder-kind filter. The exact Jellyfin 12 RC6 single-query collection contract passed for TV-only, mixed, and unrelated collections, so no client membership scanner was added. Focused row/request tests and app Kotlin compilation pass.
<!-- SECTION:FINAL_SUMMARY:END -->

## Verification

```powershell
.\gradlew.bat :app:testDefaultDebugUnitTest
.\gradlew.bat :app:assembleDefaultDebug
```

Run the RC6 collection query check documented in the task notes.

## Out of scope

- Hub screen.
- Plugin contract wrapper.
- Smart collection generation.
