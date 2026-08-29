---
id: APP-004
title: Add 300 ms Home Search and library focus previews
status: To Do
assignee: []
created_date: '2026-08-29'
labels:
  - android
  - navigation
  - focus
  - tests
dependencies: 
  - APP-003
priority: high
---

## Description

Wire drawer focus to the preview operations with a cancellable 300 ms dwell. Home and Search participate; Watch List, Settings, profile, More, and other built-ins remain click-only.

## Goal

Deliver the approved focus-preview interaction without opening Search input, stealing drawer focus, or causing network/navigation churn during fast traversal.

## Files to inspect

- `ui/nav/NavDrawer.kt`
- `ui/nav/ApplicationContent.kt`
- `ui/search/SearchPage.kt`
- `services/NavigationManager.kt`
- any current focus interaction-source helpers

## Implementation

1. Add `DRAWER_PREVIEW_DELAY_MS = 300L` as a code constant, not a preference.
2. Track the currently focused preview key. Use a `LaunchedEffect` or one ViewModel job so changing focus cancels the previous delay automatically.
3. Preview only:
   - Home
   - Search
   - `ServerNavDrawerItem` whose preview destination is `LibraryHub`
4. Focusing any click-only item cancels a pending preview.
5. Call `NavigationManager.previewFromDrawer()` only after the item remains focused for 300 ms and only when it differs from the visible preview.
6. Keep focus in the drawer after preview navigation.
7. Extend Search destination/state with the smallest activation signal needed to distinguish:

   ```text
   preview: page visible, field inactive
   activated: field requests focus and keyboard may open
   ```

   Do not add a second Search screen.
8. On Search OK, replace/activate the current Search destination and request the existing input focus. On Search focus alone, `SearchPage` must skip its current automatic focus request.
9. Home focus restores the existing Home entry without calling `reloadHome()`. Home OK preserves explicit reload behaviour.
10. Library OK calls `openLibraryBrowse()` using that item’s hub and browse destinations.
11. Keep the existing drawer More expansion and non-video click behaviour.
12. Add a coroutine-level cancellation test only if the focus-delay logic is extracted into a testable controller. Do not extract it solely to satisfy a test.

## Acceptance criteria

- [ ] Fast traversal previews only the final item that remains focused for 300 ms.
- [ ] Home, Search, and video libraries preview.
- [ ] Search preview does not focus input or open keyboard.
- [ ] Search OK activates input.
- [ ] Home preview does not reload; Home OK can reload.
- [ ] Watch List, Settings, profile, More, and non-preview items do not focus-switch.
- [ ] Drawer retains focus throughout preview.
- [ ] Library OK builds Hub -> Browse Back behaviour.

## Verification

```powershell
.\gradlew.bat :app:assembleDefaultDebug
```

Manually run the Drawer previews section in `architecture/verification-matrix.md`.

## Out of scope

- Hub design.
- Genre selector.
- User-adjustable timing.
