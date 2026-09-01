---
id: wphn-015
title: Hide and force-disable family-unneeded features in Rais
status: Done
assignee:
  - '@codex'
created_date: '2026-08-29'
updated_date: '2026-09-01 14:33'
labels:
  - android
  - private-flavour
  - settings
dependencies:
  - wphn-014
modified_files:
  - app/src/main/java/com/github/damontecres/wholphin/BuildCapabilities.kt
  - app/src/main/java/com/github/damontecres/wholphin/MainActivity.kt
  - app/src/main/java/com/github/damontecres/wholphin/MainContent.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/preferences/AppPreference.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/services/MediaManagementService.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/services/ScreensaverService.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/services/ThemeSongPlayer.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/data/ItemDetailsDialogInfo.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/setup/SwitchServerContent.kt
  - app/src/rais/AndroidManifest.xml
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Use the private flavour’s capability policy to hide and disable theme music, screensaver, media management, file-path/technical management, and ordinary server-edit controls without deleting the underlying reusable code.

## Goal

Simplify Rais Stream and guarantee hidden persisted preferences cannot reactivate unwanted behaviour, while standard Wholphin variants remain unchanged.

## Files to inspect

- `preferences/AppPreference.kt`
- `MainActivity.kt`
- `MainContent.kt`
- `services/ThemeSongPlayer.kt`
- `services/ScreensaverService.kt`
- `services/MediaManagementService.kt`
- context-menu builders and item-details dialogs
- `AndroidManifest.xml` DreamService declaration
- server/user setup and Settings entry points

## Implementation

1. Define/read a small immutable build capability policy, backed by BuildConfig booleans. Standard variants default enabled; Rais sets disabled:
   - theme music;
   - screensaver;
   - media management;
   - ordinary server management.
2. Gate settings lists so Rais hides:
   - Theme Music;
   - Screensaver destination/settings;
   - Show media management;
   - server add/remove/edit controls except the Advanced escape hatch added in wphn-018.
3. Force runtime disable:
   - `ThemeSongPlayer` returns/stops when capability disabled regardless stored volume.
   - app shell does not start/render/pulse screensaver state when disabled.
   - Rais manifest overlay disables/removes DreamService exposure if practical without affecting standard variants.
   - media-management capability reports false and delete actions never render.
4. Hide file-path and technical/server-edit details from dialogs in Rais even for administrators.
5. Preserve:
   - watched/unwatched;
   - Watch List;
   - add to playlist;
   - playback version;
   - audio/subtitle selection;
   - media report only if it is not an editing/management action and remains useful.
6. Prefer one shared capability check at each service/action boundary rather than caller-specific `if (BuildConfig...)` duplication.
7. Do not remove proto fields, migrations, services, or reusable screens. Standard builds must still expose them.
8. Search every caller of delete/canDelete and theme/screen saver entry points before changing the shared boundary.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Rais exposes none of the specified settings/actions.
- [x] #2 Existing persisted theme/screensaver/media-management preferences cannot reactivate them.
- [x] #3 No delete or file-path/server-edit action appears in Rais.
- [x] #4 Watched, Watch List, playlist, version, audio, and subtitle controls remain.
- [x] #5 Standard variants retain existing functionality.
- [x] #6 Underlying code is retained unless this task alone makes a small piece unreachable.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Extend the existing WPHN-014 BuildConfig capability fields through one immutable BuildCapabilities policy; keep standard defaults enabled and Rais overrides disabled.
2. Filter the existing settings lists and ordinary server add/remove/edit controls through that policy, while retaining server switching and all preserved playback/watch/playlist controls.
3. Enforce capabilities at shared runtime/action boundaries: ThemeSongPlayer, ScreensaverService plus app shell and Rais DreamService manifest overlay, MediaManagementService canDelete/deleteItem, and ItemDetailsDialogInfo file-path rendering.
4. Add only the smallest focused regression check warranted by the shared capability logic, then run WPHN-015's specified Default/Rais app compilation and inspect the assembled manifests/diff.
5. Re-check every acceptance criterion, record exact implementation/verification evidence, obtain independent review, commit only WPHN-015 paths, push to origin/rais, and set WPHN-015 Done as explicitly requested.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented one immutable BuildCapabilities policy over WPHN-014's four BuildConfig fields. Existing preference lists, shared theme/screensaver/media-management boundaries, technical-details rendering, and server-switcher controls now consume that policy; reusable code and standard behavior remain.

Verification: `rtk proxy .\gradlew.bat :app:assembleDefaultDebug :app:assembleRaisDebug` completed BUILD SUCCESSFUL after the final Settings guard (124 actionable tasks; 17 executed, 107 up-to-date). Kotlin compilation for both touched app variants ran inside those assemblies. Existing warnings only: Room missing-index advisory, deprecated diagnostic stack trace/package-install APIs, and Gradle deprecation/configuration-time resolution warnings.

Generated-output inspection: Default BuildConfig has all four capabilities true; Rais has all four false. Merged DefaultDebug manifests retain WholphinDreamService; merged RaisDebug manifests contain none.

Installed AOSP TV emulator comparison: Rais Settings omitted Play theme music, Screensaver settings, and Seerr/Add Server; Rais Advanced omitted Show media management options; server switching retained the existing Rais Stream server with no add/remove/edit controls. Rais context menu retained Add to playlist, Mark as watched, Add to Watch List, and Play with while showing no Delete. Episode details retained 1080p/HEVC, English AAC 5.1, and English SRT while showing no path or item ID. Default Settings still showed Play theme music, Screensaver settings, Seerr integration/Add Server, and Advanced Show media management options.

Dependency evidence: `git merge-base --is-ancestor 051f356e HEAD` exited 0 for WPHN-014. `git diff --check` passed. Independent reviewer verdict: ship, no findings.

Deviation/blocked-check record: the first verification-only Rais rebuild used obsolete temporary family.properties keys and failed validation; it was corrected from family.properties.example and rerun successfully. An emulator run-as transfer attempt through /data/local/tmp was denied by SELinux; a quoted base64 stream copied only the disposable emulator session and enabled the installed comparison. No repository workaround or unrelated repair was made. No automated tests were added because the task specifies flavour assemblies and the externally observable flavor comparison was exercised directly.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
WPHN-015 is complete. Rais now hides and force-disables theme music, screensaver, media management/delete, file-path/technical management details, Seerr configuration, and ordinary server add/remove controls through shared immutable build capabilities. Server switching and watched, Watch List, playlist, playback-version, audio, subtitle, and non-editing media information remain available. Default retains the original surfaces. Both flavor assemblies and installed Default/Rais comparisons passed; independent review returned ship.
<!-- SECTION:FINAL_SUMMARY:END -->

## Verification

```powershell
.\gradlew.bat :app:assembleDefaultDebug
.\gradlew.bat :app:assembleRaisDebug
```

Manually compare Settings and context menus in Default versus Rais.

## Out of scope

- Deleting dormant features.
- General Settings redesign.
