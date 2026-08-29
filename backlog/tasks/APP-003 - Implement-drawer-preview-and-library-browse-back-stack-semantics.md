---
id: APP-003
title: Implement drawer preview and library browse back-stack semantics
status: To Do
assignee: []
created_date: '2026-08-29'
labels:
  - android
  - navigation
  - tests
dependencies: 
  - APP-002
priority: high
---

## Description

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

## Acceptance criteria

- [ ] Previewing five drawer items does not require five Back presses.
- [ ] Home state key is retained during preview navigation.
- [ ] Browse stack always includes the matching hub beneath it.
- [ ] Existing non-preview navigation remains unchanged.
- [ ] Back-stack tests pass without Compose UI tests.

## Verification

```powershell
.\gradlew.bat :app:testDefaultDebugUnitTest
.\gradlew.bat :app:assembleDefaultDebug
```

## Out of scope

- 300 ms focus jobs.
- Search keyboard behaviour.
- Hub focus entry.
