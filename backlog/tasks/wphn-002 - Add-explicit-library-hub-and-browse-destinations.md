---
id: wphn-002
title: Add explicit library hub and browse destinations
status: Human Review
assignee:
  - '@codex'
created_date: '2026-08-29'
updated_date: '2026-08-29 17:32'
labels:
  - android
  - navigation
  - library-hub
  - tests
dependencies:
  - wphn-001
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Split a video library’s drawer destination into a focus-preview hub and an OK-activated full browser. Keep all non-video library routes on their existing specialised screens.

## Goal

Create the minimum navigation vocabulary needed by later drawer and hub tasks without changing the visible library screen yet.

## Files to inspect

- `ui/nav/Destination.kt`
- `ui/nav/DestinationContent.kt`
- `ui/nav/NavDrawer.kt`
- `services/NavDrawerService.kt`
- `util/Constants.kt`
- current `ServerNavDrawerItem` callers

## Implementation

1. Add serializable destinations with only the fields required to reconstruct the current library context:

   ```kotlin
   Destination.LibraryHub(
       itemId: UUID,
       type: BaseItemKind,
       collectionType: CollectionType?,
   )

   Destination.LibraryBrowse(
       itemId: UUID,
       type: BaseItemKind,
       collectionType: CollectionType?,
   )
   ```

   Do not add names, icons, filter JSON, or row configuration to navigation keys unless the existing code proves they are required.

2. Add one pure helper that decides whether a Jellyfin user view should use the new video hub. Initial supported collection types:
   - Movies
   - TV Shows
   - Home Videos
   - Music Videos when used as video media
   - mixed/unknown video folders where the item type is a collection folder or user view and the existing page treats it as video
3. Explicitly exclude Music, Photos, Live TV, Box Sets, Playlists, and recording folders from hub routing.
4. Change `ServerNavDrawerItem` to expose:

   ```text
   previewDestination
   clickDestination
   ```

   For ordinary non-video items both values may remain the same existing destination.

5. Update `NavDrawerService` to build the two destinations from the same library object. Do not fetch additional metadata.
6. Add temporary destination dispatch that delegates both new destinations to the existing collection-folder screen so the branch compiles. `wphn-006` and `wphn-008` replace those temporary delegates.
7. Add a table-driven JVM test for the video-like classifier and its fallbacks.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Every drawer item still has a valid click destination.
- [x] #2 Supported video libraries carry distinct hub/browse destinations.
- [x] #3 Non-video libraries retain current destinations and behaviour.
- [x] #4 New destination keys serialize through the existing back-stack persistence path.
- [x] #5 Classifier tests cover Movies, TV, Home Videos, Music, Photos, Live TV, Box Sets, Playlists, unknown video folder, and recording folder.
- [x] #6 No visual redesign or focus dwell is implemented here.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add LibraryHub and LibraryBrowse as serializable Destination keys and add a pure library classifier in ui/library using existing Jellyfin enums. 2. Extend ServerNavDrawerItem with previewDestination and clickDestination, wire NavDrawerService from the same Library object, and keep current clicks/highlighting on clickDestination. 3. Temporarily dispatch both new keys through the existing CollectionFolder screen and add focused JVM coverage for classification and destination serialization. 4. Run :app:testDefaultDebugUnitTest and :app:assembleDefaultDebug, inspect the complete diff, record evidence, and move the task to Human Review.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Blocked before implementation: declared dependency WPHN-001 is not merged into the current rais branch. Current HEAD is 0f7fbd46; git merge-base --is-ancestor 0ed62c2b HEAD returned exit 1. Only codex/wphn-001-baseline and origin/codex/wphn-001-baseline contain the focused WPHN-001 commit. Current HEAD also lacks the WPHN-001 fixture, repository note, tracked plan package, and .gitignore change. No implementation files were modified. Required next step: merge or fast-forward WPHN-001 into the intended WPHN-002 base branch, then return WPHN-002 to Ready.

Dependency resolved: rais was fast-forwarded to WPHN-001 commit 0ed62c2b. The ancestry gate now passes and implementation resumed.

Implemented files: ui/nav/Destination.kt, ui/nav/DestinationContent.kt, ui/nav/NavDrawer.kt, services/NavDrawerService.kt, ui/library/LibraryType.kt, and ui/library/LibraryTypeTest.kt. Added exact serializable hub/browse keys, a pure internal classifier, paired drawer destinations, existing-click routing, temporary CollectionFolder dispatch, and table-driven plus serialization coverage. Verification: git diff --check passed; .\gradlew.bat :app:testDefaultDebugUnitTest ran 346 tests with 342 passing, including both new tests, and only the four pre-existing ServerRepositoryTest FileStorage IOException failures; .\gradlew.bat :app:assembleDefaultDebug completed BUILD SUCCESSFUL. Independent reviewer verdict: ship. Deviations: no product-scope deviations; a stale generated Gradle problems report was moved aside to recover packaging, and no unrelated failures were repaired.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added explicit serializable library hub and browse destinations, reusable video-library classification, and paired drawer routing while preserving all specialized library flows. Verified both new tests through the full unit task (342 passed; four documented pre-existing ServerRepositoryTest failures), successful default-debug assembly, clean diff checks, and an independent ship review.
<!-- SECTION:FINAL_SUMMARY:END -->

## Verification

```powershell
.\gradlew.bat :app:testDefaultDebugUnitTest
.\gradlew.bat :app:assembleDefaultDebug
```

## Out of scope

- Hub UI.
- Drawer focus switching.
- Full browser extraction.
- Library-name icon matching.
