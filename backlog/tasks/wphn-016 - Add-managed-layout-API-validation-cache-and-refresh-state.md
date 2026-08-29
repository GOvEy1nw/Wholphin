---
id: wphn-016
title: Add managed layout API validation cache and refresh state
status: Ready
assignee: []
created_date: "2026-08-29"
labels:
  - android
  - managed-layout
  - foundation
  - tests
dependencies:
  - wphn-001
priority: high
---

## Description

Implement the client half of the version-one managed-layout contract as a small service that can emit a valid cache immediately and then refresh from the plugin.

## External dependency

`LAY-002` must have fixed the same canonical JSON fixture and field names. Do not proceed if the two fixtures differ semantically.

## Goal

Provide a reliable, isolated managed-layout source without changing Home/hub rendering yet.

## Files to inspect

- `services/hilt/AppModule.kt`
- `services/UserSwitchListener.kt`
- current authenticated OkHttp client
- `services/HomeSettingsService.kt`
- existing local JSON file read/write patterns
- `data/model/HomeRowConfig.kt`

## Implementation

1. Add Kotlin models exactly matching `architecture/managed-layout-contract.md` and the canonical fixture.
2. Reuse `HomeRowConfig` polymorphic decoding. Do not define plugin-specific row subclasses.
3. Add an authenticated API wrapper for:

   ```text
   GET /WholphinLayout/v1/layout
   ```

4. Interpret responses exactly:
   - 200 managed envelope;
   - 204 unassigned;
   - 404 absent/unmanaged;
   - auth/server errors retain cache and expose status.
5. Add validation before activation:
   - schema version 1;
   - required metadata;
   - unique row IDs per surface;
   - hub key/library ID equality;
   - valid limits/UUIDs/row types.
6. Add a per-server/per-user cache under app files. Write via temporary file and same-directory atomic rename. Never overwrite a valid cache with an invalid/unsupported remote response.
7. Expose a single StateFlow/status model that can represent:
   - Unmanaged;
   - ManagedCached;
   - ManagedRemote;
   - ManagedStale with error;
   - ManagedNoCache with error/loading.
8. Implement cached-first load:
   - validate and emit cache synchronously/first;
   - fetch remote;
   - update only when valid;
   - compare `revision` to avoid resetting active UI unnecessarily.
9. Track last successful remote refresh separately from envelope `generatedAt`.
10. Add a 15-minute resume staleness constant and methods for:

- login/app launch forced refresh;
- resume refresh if stale;
- manual forced refresh.

11. Do not add WorkManager, polling, WebSockets, push, ETag support, or a database migration.
12. Add tests:

- canonical fixture decode;
- unsupported version rejection;
- duplicate row rejection;
- invalid remote does not replace cache;
- cache emits before delayed remote;
- unchanged revision does not replace active object/state unnecessarily.

## Acceptance criteria

- [ ] Canonical fixture decodes.
- [ ] Cached layout can emit before network completion.
- [ ] Valid remote atomically replaces cache and state.
- [ ] Invalid/unsupported remote never destroys a valid cache.
- [ ] 204/404 select unmanaged mode.
- [ ] Resume refresh uses 15-minute staleness, not a polling loop.
- [ ] Service contains no rendering or profile-admin logic.

## Verification

```powershell
.\gradlew.bat :app:testDefaultDebugUnitTest
.\gradlew.bat :app:assembleDefaultDebug
```

## Out of scope

- Applying layouts to Home/hubs.
- Status page.
- Plugin implementation.
