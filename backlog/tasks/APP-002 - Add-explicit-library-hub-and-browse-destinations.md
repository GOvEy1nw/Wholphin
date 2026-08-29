---
id: APP-002
title: Add explicit library hub and browse destinations
status: To Do
assignee: []
created_date: '2026-08-29'
labels:
  - android
  - navigation
  - library-hub
  - tests
dependencies: 
  - APP-001
priority: high
---

## Description

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
6. Add temporary destination dispatch that delegates both new destinations to the existing collection-folder screen so the branch compiles. `APP-006` and `APP-008` replace those temporary delegates.
7. Add a table-driven JVM test for the video-like classifier and its fallbacks.

## Acceptance criteria

- [ ] Every drawer item still has a valid click destination.
- [ ] Supported video libraries carry distinct hub/browse destinations.
- [ ] Non-video libraries retain current destinations and behaviour.
- [ ] New destination keys serialize through the existing back-stack persistence path.
- [ ] Classifier tests cover Movies, TV, Home Videos, Music, Photos, Live TV, Box Sets, Playlists, unknown video folder, and recording folder.
- [ ] No visual redesign or focus dwell is implemented here.

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
