---
id: APP-017
title: Apply managed layouts to Home hubs and the read-only status page
status: To Do
assignee: []
created_date: '2026-08-29'
labels:
  - android
  - managed-layout
  - library-hub
  - settings
dependencies: 
  - APP-006
  - APP-007
  - APP-016
priority: high
---

## Description

Connect the managed layout state to the existing Home and LibraryHub row engine, preserve unmanaged behaviour, and replace the local editor with a small read-only status page only when a managed profile is active.

## Goal

Render cached-first plugin layouts without blanking content or duplicating the existing Home/row implementation.

## Files to inspect

- `services/UserSwitchListener.kt`
- `services/HomeSettingsService.kt`
- `ui/main/HomeViewModel.kt`
- `ui/main/settings/HomeSettingsViewModel.kt`
- `ui/main/settings/HomeSettingsPage.kt`
- `ui/library/LibraryHub*`
- app resume hooks in `MainActivity.kt`

## Implementation

1. Add one resolved layout-source layer:

   ```text
   Managed envelope when assigned/valid
   existing unmanaged local/server/default settings otherwise
   ```

   Do not replace or delete unmanaged settings.
2. On user switch/app launch:
   - ask APP-016 service to emit cache immediately;
   - render managed cache if available;
   - refresh remote in parallel;
   - replace rows only when the valid revision changes.
3. Home mapping:
   - map each `ManagedRow.config` to existing resolver/fetcher;
   - carry stable ID, title override, max items, and showViewMore in the resolved display wrapper;
   - use row maxItems when fetching;
   - keep existing Home actions/header behaviour.
4. Hub mapping:
   - look up surface by library UUID;
   - extract at most one Genres config to the fixed selector;
   - use managed media rows in Home mode;
   - keep embedded genre filtering from APP-007;
   - if no managed hub exists, use built-in fallback hub.
5. Defensively treat row 403/404/unresolvable parent as unavailable and continue rendering other rows.
6. When a revision refresh changes rows, preserve compatible focus only by stable row ID/item ID where current Compose state makes that straightforward. Otherwise safely return to first media; do not build a general state migration layer.
7. Assigned managed user:
   - route Home customisation setting to `ManagedLayoutStatusPage`;
   - show profile name, revision/update time, last remote refresh, active source, stale/error status;
   - provide `Refresh managed layout`.
8. Unassigned user:
   - keep current Home customiser, local save, server profile save/load, and defaults.
9. Wire app resume to `refreshIfStale()` and do nothing when unmanaged.
10. Avoid a blank Home while remote loads. Existing cache/current unmanaged fallback remains visible until a valid managed state is available.
11. Add a small integration/ViewModel test for managed versus unmanaged source selection if the current test harness supports it without broad mocks.

## Acceptance criteria

- [ ] Assigned users render managed Home and configured hubs.
- [ ] Cache renders first and valid remote revision replaces it.
- [ ] No managed hub entry falls back to built-in hub.
- [ ] Genres remain in the fixed selector slot.
- [ ] Title, limit, View All, and view options apply through existing row rendering.
- [ ] Inaccessible/broken row does not fail the page.
- [ ] Managed user sees read-only status and Refresh.
- [ ] Unmanaged user retains existing editor and persistence.
- [ ] Resume refresh occurs only when stale.

## Verification

```powershell
.\gradlew.bat :app:testDefaultDebugUnitTest
.\gradlew.bat :app:assembleDefaultDebug
```

Run managed cache/offline/assignment scenarios from the verification matrix using a fixture or plugin build.

## Out of scope

- Editing plugin profiles in the app.
- Push refresh.
- Icon override.
