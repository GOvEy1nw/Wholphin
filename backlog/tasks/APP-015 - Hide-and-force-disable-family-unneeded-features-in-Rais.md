---
id: APP-015
title: Hide and force-disable family-unneeded features in Rais
status: To Do
assignee: []
created_date: '2026-08-29'
labels:
  - android
  - private-flavour
  - settings
dependencies: 
  - APP-014
priority: high
---

## Description

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
   - server add/remove/edit controls except the Advanced escape hatch added in APP-018.
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

## Acceptance criteria

- [ ] Rais exposes none of the specified settings/actions.
- [ ] Existing persisted theme/screensaver/media-management preferences cannot reactivate them.
- [ ] No delete or file-path/server-edit action appears in Rais.
- [ ] Watched, Watch List, playlist, version, audio, and subtitle controls remain.
- [ ] Standard variants retain existing functionality.
- [ ] Underlying code is retained unless this task alone makes a small piece unreachable.

## Verification

```powershell
.\gradlew.bat :app:assembleDefaultDebug
.\gradlew.bat :app:assembleRaisDebug
```

Manually compare Settings and context menus in Default versus Rais.

## Out of scope

- Deleting dormant features.
- General Settings redesign.
