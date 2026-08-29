---
id: APP-019
title: Add Seerr Quick Connect and persistent per-user sessions
status: To Do
assignee: []
created_date: '2026-08-29'
labels:
  - android
  - onboarding
  - seerr
  - tests
  - private-flavour
dependencies: 
  - APP-018
priority: high
---

## Description

Replace Rais Stream’s need to persist/replay Seerr passwords with Seerr’s own Quick Connect flow and a persistent session store. Keep a non-persisted credential fallback when Quick Connect is unavailable.

## Goal

Establish the same family member in Seerr after Jellyfin login without embedding secrets, reusing Jellyfin tokens, or retaining plaintext passwords.

## Files to inspect

- `app/src/main/seerr/seerr-api.yml`
- generated Seerr client conventions
- `services/SeerrApi.kt`
- `services/SeerrServerRepository.kt`
- `services/UserSwitchListener.kt`
- `data/model/SeerrServer.kt`
- `services/hilt/AppModule.kt`
- existing Seerr setup screens/ViewModel

## Implementation

1. Treat the configured/running Seerr OpenAPI definition as the endpoint source of truth. Update `seerr-api.yml` and regenerate; do not hand-edit generated files.
2. Ensure generated operations cover the semantic sequence:

   ```text
   initiate Quick Connect
   poll/check code
   final authenticate
   ```

   Seerr documentation and frontend versions have used slightly different final route shapes; business logic must call the generated operation and a live integration test must verify the configured server.
3. Add a dedicated Seerr OkHttp client/qualifier rather than adding user-specific cookies to the general Jellyfin client.
4. Add a small persistent cookie/session store under app-private `noBackupFilesDir`, keyed by:

   ```text
   Jellyfin server ID
   Jellyfin user row/ID
   Seerr base URL
   ```

   Store only the session data necessary for Seerr requests. Do not store a plaintext password.
5. On Jellyfin user switch/login:
   - load that user’s Seerr session;
   - validate with Seerr `auth/me` or equivalent;
   - activate Discover/request functionality when valid;
   - expose reconnect state when expired.
6. Quick Connect onboarding:
   - initiate a **new Seerr** transaction;
   - show the Seerr code and clear instructions;
   - poll check with cancellation, bounded interval, and finite timeout;
   - final-authenticate with code and known username; request optional email only if the server/API requires it;
   - persist returned session/cookies;
   - never reuse the Jellyfin Quick Connect code or access token.
7. Credential fallback:
   - available only when Quick Connect is unavailable/fails or user chooses it;
   - submit Jellyfin-mode username/password to Seerr;
   - discard password immediately after session establishment;
   - save username/non-secret identity plus session only.
8. Migrate existing Rais Seerr records on successful session creation by clearing any stored password. Do not undertake a broad Room redesign when a nullable-field update is sufficient.
9. Update `UserSwitchListener` to restore session first instead of replaying `seerrLogin` with stored password.
10. Cancelling/failing Seerr setup must not block Jellyfin content. Discover/request UI remains inactive and offers Reconnect in Settings.
11. Standard Wholphin’s existing manual Seerr setup may remain, but shared repository changes must not break it.
12. Add fake-API tests for:
   - initiate/check/authenticate success;
   - cancellation;
   - timeout;
   - invalid/expired stored session;
   - user partitioning;
   - credential fallback discards password.

## Acceptance criteria

- [ ] Rais can complete Seerr’s separate Quick Connect flow.
- [ ] Jellyfin Quick Connect code/token is never reused for Seerr.
- [ ] Session survives process restart for the correct user.
- [ ] Switching Jellyfin users cannot reuse another user’s Seerr session.
- [ ] No plaintext Seerr password is stored after successful setup.
- [ ] Expired session produces Reconnect, not a broken Jellyfin login.
- [ ] Cancelling setup leaves playback usable.
- [ ] Generated client and live configured Seerr endpoint agree.
- [ ] Tests cover state transitions and session partitioning.

## Verification

```powershell
.\gradlew.bat :app:testRaisDebugUnitTest
.\gradlew.bat :app:assembleRaisDebug
```

Perform one live Quick Connect and one fallback login against the configured Seerr instance.

## Out of scope

- Reusing Jellyfin tokens as Seerr credentials.
- Administrative API-key embedding.
- General multi-account password vault.
