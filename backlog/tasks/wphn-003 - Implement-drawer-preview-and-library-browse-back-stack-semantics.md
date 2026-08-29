---
id: wphn-003
title: Implement drawer preview and library browse back-stack semantics
status: Human Review
assignee:
  - '@codex'
created_date: '2026-08-29'
updated_date: '2026-08-29 17:50'
labels:
  - android
  - navigation
  - tests
dependencies:
  - wphn-002
modified_files:
  - >-
    app/src/main/java/com/github/damontecres/wholphin/services/NavigationManager.kt
  - >-
    app/src/test/java/com/github/damontecres/wholphin/services/NavigationManagerTest.kt
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add explicit navigation-manager operations for drawer previews and library browsing. Repeated focus previews must replace one another, while full browsing must place its matching hub directly beneath it.

## Goal

Make back-stack behaviour deterministic before any focus event starts triggering navigation.

## Files to inspect

- `services/NavigationManager.kt`
- current tests for `NavigationManager`
- `ui/nav/Destination.kt`
- every caller of `navigateToFromDrawer`, `goToHome`, and `reloadHome`

## Implementation

1. Preserve the existing Home entry instead of replacing it during previews so its remembered state survives.
2. Add one operation equivalent to:

   ```kotlin
   fun previewFromDrawer(destination: Destination)
   ```

   Required semantics:
   - Home: remove preview/page entries above the existing Home destination without reloading it.
   - Search/LibraryHub: keep the existing Home entry and replace any previous drawer preview with the new destination.
   - same destination already visible: no-op.
   - never append a preview on every focus change.

3. Add one operation equivalent to:

   ```kotlin
   fun openLibraryBrowse(
       hub: Destination.LibraryHub,
       browse: Destination.LibraryBrowse,
   )
   ```

   It must establish:

   ```text
   Home -> matching hub -> matching browse
   ```

4. Keep the current explicit Home click/reload operation separate from Home focus preview.
5. Preserve generic `navigateToFromDrawer` for non-preview items; do not silently change every existing caller.
6. Add the smallest runnable back-stack tests supported by the current test harness:
   - repeated previews leave one preview entry;
   - Home preview returns to the existing Home key;
   - browse Back returns to matching hub;
   - explicit Home reload changes only when requested.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Previewing five drawer items does not require five Back presses.
- [x] #2 Home state key is retained during preview navigation.
- [x] #3 Browse stack always includes the matching hub beneath it.
- [x] #4 Existing non-preview navigation remains unchanged.
- [x] #5 Back-stack tests pass without Compose UI tests.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Extend `NavigationManager` with drawer-preview and library-browse operations that retain the existing `Destination.Home` entry, replace preview entries instead of appending them, and leave `navigateToFromDrawer`/explicit Home reload behavior unchanged.
2. Add one focused JVM test class covering repeated previews, retained Home key, matching hub beneath browse, explicit reload, and unchanged generic drawer navigation.
3. Inspect the complete diff, run the focused `NavigationManagerTest`, then compile the touched `:app` module with the narrowest relevant Gradle compile task. Record exact results and any pre-existing failures.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented `NavigationManager.previewFromDrawer` and `openLibraryBrowse` without changing existing callers. Preview navigation retains the existing Home object/key, keeps at most one preview, no-ops for an equal visible destination, and Home preview trims back to the retained Home. Library browse preserves an already-visible matching hub and establishes Home -> hub -> browse. Added a focused Robolectric JVM test class covering all task contracts and unchanged generic drawer navigation.

Verification:
- `rtk proxy .\gradlew.bat :app:testDefaultDebugUnitTest --tests "com.github.damontecres.wholphin.services.NavigationManagerTest"` — PASS, 4 tests; Gradle also completed `:app` production and unit-test Kotlin/Java compilation.
- `rtk git diff --check` — PASS.
- Fresh orchestrate reviewer verdict — ship, no findings.

Environment/deviations:
- The initial sandboxed Gradle run was inconclusive/failing because Windows denied Kotlin daemon, generated `R.jar`, and Gradle problems-report access. The authorized host rerun exposed an Android Log/ACRA JVM issue, resolved by reusing the repository's Robolectric runner; the final authorized rerun passed.
- No Compose UI tests or full unrelated suite were run; the task calls for focused back-stack JVM checks plus touched-module compilation.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary
- Add explicit drawer-preview back-stack semantics that retain Home state and replace prior previews.
- Add library browse navigation that keeps the matching hub directly beneath browse.
- Preserve existing generic drawer navigation and explicit Home reload behavior.
- Add focused JVM regression coverage for repeated/equal previews, Home-key retention, browse Back, and reload separation.

## Verification
- Focused `NavigationManagerTest`: 4 passed.
- `:app` production/test compilation completed during the focused Gradle run.
- `git diff --check`: passed.
- Independent review: ship.
<!-- SECTION:FINAL_SUMMARY:END -->

## Verification

```powershell
.\gradlew.bat :app:testDefaultDebugUnitTest
.\gradlew.bat :app:assembleDefaultDebug
```

## Out of scope

- 300 ms focus jobs.
- Search keyboard behaviour.
- Hub focus entry.
