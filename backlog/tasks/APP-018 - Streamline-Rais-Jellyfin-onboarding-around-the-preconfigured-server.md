---
id: APP-018
title: Streamline Rais Jellyfin onboarding around the preconfigured server
status: To Do
assignee: []
created_date: '2026-08-29'
labels:
  - android
  - onboarding
  - private-flavour
dependencies: 
  - APP-014
priority: high
---

## Description

Use the private flavour’s preconfigured Jellyfin URL to skip ordinary server entry while preserving Wholphin’s existing server validation, credential login, Quick Connect, and an Advanced recovery path.

## Goal

A clean Rais Stream install should ask the family only who they are/how they want to authenticate, not where the server is.

## Files to inspect

- `MainActivityViewModel.appStart()`
- setup navigation destinations
- `ui/setup/SwitchServerViewModel.kt`
- `ui/setup/SwitchServerContent.kt` / `ServerList.kt`
- `ui/setup/SwitchUserViewModel.kt`
- `ui/setup/SwitchUserContent.kt`
- `data/ServerRepository.kt`
- current Quick Connect UI/state

## Implementation

1. Standard variants retain the existing server-list start flow.
2. In Rais, when no saved server/session exists:
   - read preconfigured HTTPS Jellyfin URL;
   - call the existing discovery/public-system-info validation path;
   - construct/store the real server ID/name/version returned by Jellyfin;
   - navigate directly to that server’s User list/login screen.
3. Do not hardcode or invent a Jellyfin server UUID.
4. Reuse existing username/password login and Jellyfin Quick Connect; do not create parallel authentication clients.
5. Saved users/sessions restore through current `ServerRepository` behaviour.
6. If the preconfigured server cannot be reached, show a focused TV-safe recovery state:
   - Retry;
   - Open Advanced server setup.
7. Add an Advanced Settings escape hatch that opens the current server management UI. Keep add/edit/remove controls out of ordinary onboarding and ordinary Rais Settings.
8. Provide a `Reset to preconfigured server` action in the Advanced path so a temporary override is reversible.
9. A manually chosen override may remain stored for that installation; do not add LAN detection or automatic URL switching.
10. After successful Jellyfin authentication, signal APP-019’s Seerr onboarding/session check. Jellyfin app content must remain usable if that secondary step is skipped or fails.
11. No username, password, access token, Quick Connect secret, or server identity beyond the configured public URL is placed in BuildConfig/private properties.

## Acceptance criteria

- [ ] Clean Rais install skips server-address input.
- [ ] It validates and stores the server using existing code.
- [ ] Username/password and Jellyfin Quick Connect both work.
- [ ] Existing session restore works.
- [ ] Failure offers Retry and Advanced setup.
- [ ] Advanced override and reset are available but not in ordinary flow.
- [ ] Standard variants retain current setup.
- [ ] No credentials/tokens are embedded.

## Verification

```powershell
.\gradlew.bat :app:assembleRaisDebug
.\gradlew.bat :app:assembleDefaultDebug
```

Test clean data, saved user, wrong URL, server offline, credentials, and Quick Connect.

## Out of scope

- Local/remote URL auto-detection.
- Multiple preconfigured family servers.
- Seerr session implementation.
