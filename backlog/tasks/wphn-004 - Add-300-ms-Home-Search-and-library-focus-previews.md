---
id: wphn-004
title: Add 300 ms Home Search and library focus previews
status: Done
assignee:
  - '@codex'
created_date: '2026-08-29'
updated_date: '2026-08-29 18:41'
labels:
  - android
  - navigation
  - focus
  - tests
dependencies:
  - wphn-003
modified_files:
  - app/src/main/java/com/github/damontecres/wholphin/ui/nav/NavDrawer.kt
  - app/src/main/java/com/github/damontecres/wholphin/ui/nav/Destination.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/nav/DestinationContent.kt
  - app/src/main/java/com/github/damontecres/wholphin/ui/search/SearchPage.kt
  - app/src/main/java/com/github/damontecres/wholphin/ui/UiConstants.kt
  - app/src/main/java/com/github/damontecres/wholphin/ui/main/HomePage.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/components/CollectionFolderView.kt
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
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
<!-- SECTION:DESCRIPTION:END -->

## Acceptance criteria

- [ ] Fast traversal previews only the final item that remains focused for 300 ms.
- [ ] Home, Search, and video libraries preview.
- [ ] Search preview does not focus input or open keyboard.
- [ ] Search OK activates input.
- [ ] Home preview does not reload; Home OK can reload.
- [ ] Watch List, Settings, profile, More, and non-preview items do not focus-switch.
- [ ] Drawer retains focus throughout preview.
- [ ] Library OK builds Hub -> Browse Back behaviour.

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add a local focused-preview destination in `NavDrawer` and a single keyed `LaunchedEffect` with `DRAWER_PREVIEW_DELAY_MS = 300L`; item focus changes update/cancel that key, and the effect calls the existing `NavigationManager.previewFromDrawer` only when the visible destination differs.
2. Preserve existing click paths while activating Search through an activated `Destination.Search`, keeping Home click reload semantics, routing video-library OK through the existing `openLibraryBrowse`, and leaving click-only/More/non-video items unchanged.
3. Add the smallest Search activation flag to the existing destination and pass it through `DestinationContent` so `SearchPage` requests its existing focus only when activated; default existing/deep-link Search construction to activated for compatibility.
4. Inspect the complete scoped diff, run the task-relevant focused navigation test only if changed behavior is covered there, compile the touched `:app` debug flavours, run `git diff --check`, and record manual focus scenarios as unrun unless a device session is available.

5. Carry an explicit preview-versus-activation focus signal through Home and video-library content so preview composition cannot steal drawer focus; preserve normal focus transfer on OK.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implementation summary:
- Added a keyed 300 ms drawer dwell preview for Home, inactive Search, and video-library Hub destinations; focus changes cancel pending previews and click-only items do not preview.
- Preserved Home reload on OK, activated the existing Search input on OK, and reused openLibraryBrowse for Hub -> Browse back-stack behavior.
- Added a default-true content focus-ownership gate so preview composition cannot steal drawer focus; More preserves suppression and actual drawer exit/activation restores normal focus.

Files:
- app/src/main/java/com/github/damontecres/wholphin/ui/nav/NavDrawer.kt
- app/src/main/java/com/github/damontecres/wholphin/ui/nav/Destination.kt
- app/src/main/java/com/github/damontecres/wholphin/ui/nav/DestinationContent.kt
- app/src/main/java/com/github/damontecres/wholphin/ui/search/SearchPage.kt
- app/src/main/java/com/github/damontecres/wholphin/ui/UiConstants.kt
- app/src/main/java/com/github/damontecres/wholphin/ui/main/HomePage.kt
- app/src/main/java/com/github/damontecres/wholphin/ui/components/CollectionFolderView.kt

Acceptance review:
- #1-#8 are represented by the scoped implementation and were independently reviewed with an approve verdict.
- The required physical Drawer previews verification matrix was not run because no device/emulator session is attached; runtime D-pad timing, keyboard, retained-focus, and Hub -> Browse -> Back behavior remain for human confirmation.

Verification:
- .\gradlew.bat :app:compileDefaultDebugKotlin --console=plain — PASS (BUILD SUCCESSFUL).
- .\gradlew.bat :app:assembleDefaultDebug --console=plain — PASS (BUILD SUCCESSFUL, 73 actionable tasks).
- git diff --check — PASS; only line-ending conversion warnings.
- No coroutine test added because the delay stayed local to the composable, as the task directs.

Pre-existing warnings left unchanged:
- Optional Media3 decoders/libMPV are absent and the stub is used.
- Existing SeerrServer foreign-key index, manifest replacement, deprecated API, and Gradle deprecation warnings remain.

Deviations:
- None from task scope. The repository currently exposes Default/Appstore/Firetv variants rather than a Rais variant, so the task-specified Default debug variant was compiled and assembled.

Human acceptance: approved by the user on 2026-08-29 for publication; task moved from Human Review to Done.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented WPHN-004 drawer dwell previews, Search activation, library Hub/Browse activation, and preview-safe focus ownership. Default debug compile/assembly and diff check pass; physical D-pad verification remains for human review.
<!-- SECTION:FINAL_SUMMARY:END -->

## Verification

```powershell
.\gradlew.bat :app:assembleDefaultDebug
```

Manually run the Drawer previews section in `architecture/verification-matrix.md`.

## Out of scope

- Hub design.
- Genre selector.
- User-adjustable timing.
