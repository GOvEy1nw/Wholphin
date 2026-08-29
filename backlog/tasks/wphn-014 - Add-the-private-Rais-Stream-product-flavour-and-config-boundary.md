---
id: wphn-014
title: Add the private Rais Stream product flavour and config boundary
status: Ready
assignee: []
created_date: "2026-08-29"
labels:
  - android
  - private-flavour
  - foundation
dependencies:
  - wphn-001
priority: high
---

## Description

Create a private `rais` build variant that can coexist with standard Wholphin and reads only non-secret deployment values from an untracked properties file.

## Goal

Isolate family identity, endpoints, update feed, and feature policy from general/upstreamable app changes.

## Files to inspect

- `app/build.gradle.kts`
- root `.gitignore`
- `app/src/main/AndroidManifest.xml`
- existing `default`, `appstore`, and `firetv` flavour setup
- wphn-name, launcher, banner, update URL resources

## Implementation

1. Add `rais` to the existing `version` flavour dimension; do not create a combinatorial second dimension.
2. Add committed `family.properties.example` with placeholders only:

   ```properties
   rais.applicationId=
   rais.jellyfinUrl=https://jellyfin.example.com
   rais.seerrUrl=https://seerr.example.com
   rais.updateUrl=https://updates.example.com/rais-stream.json
   ```

3. Add `family.properties` to `.gitignore`.
4. Load the file lazily only when configuring/building the `rais` variant. Standard variants must not require it.
5. Fail a Rais build early with one clear message if application ID or required HTTPS URLs are blank/invalid.
6. Supply BuildConfig fields using safe quoted values:
   - preconfigured Jellyfin URL;
   - preconfigured Seerr URL;
   - update feed URL;
   - family build marker;
   - capability flags used by wphn-015.
7. Set the Rais application ID from the private file so it installs beside standard Wholphin.
8. Add `app/src/rais/res/values/strings.xml` with `app_name = Rais Stream`.
9. Establish `src/rais` launcher/banner resource override paths. Until the later branding task supplies final artwork, copy/use neutral valid placeholders so builds install without touching global Wholphin assets.
10. Route the existing updater to the Rais feed through the normal preference/default path; do not build a second updater.
11. Keep signing keys and credentials out of the file and repository.
12. Document the exact Rais build command and config setup in `docs/rais-stream/README.md`.

## Acceptance criteria

- [ ] Standard default/appstore/firetv debug builds do not need `family.properties`.
- [ ] Rais build reads the untracked values and has a distinct package ID.
- [ ] Visible app name is Rais Stream.
- [ ] Launcher/banner can be overridden from the Rais source set.
- [ ] Rais uses its configured update feed.
- [ ] No credential, token, API key, password, or signing material is committed.
- [ ] Missing Rais values fail with a concise actionable build error.

## Verification

```powershell
.\gradlew.bat :app:assembleDefaultDebug
.\gradlew.bat :app:assembleRaisDebug
```

Install both APKs on one device and confirm they coexist.

## Out of scope

- Final visual branding/artwork.
- Onboarding behaviour.
- Feature hiding implementation.
