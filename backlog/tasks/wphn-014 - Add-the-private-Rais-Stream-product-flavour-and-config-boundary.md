---
id: wphn-014
title: Add the private Rais Stream product flavour and config boundary
status: Done
assignee:
  - '@codex'
created_date: '2026-08-29'
updated_date: '2026-09-01 13:28'
labels:
  - android
  - private-flavour
  - foundation
dependencies:
  - wphn-001
modified_files:
  - .gitignore
  - app/build.gradle.kts
  - >-
    app/src/main/java/com/github/damontecres/wholphin/preferences/AppPreference.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/preferences/PreferencesViewModel.kt
  - app/src/rais/res/values/strings.xml
  - app/src/raisDebug/res/values/strings.xml
  - app/src/rais/res/mipmap-anydpi/ic_launcher.xml
  - app/src/rais/res/mipmap-anydpi/ic_banner.xml
  - app/src/rais/res/mipmap-anydpi-v26/ic_launcher.xml
  - app/src/rais/res/mipmap-anydpi-v26/ic_banner.xml
  - family.properties.example
  - docs/rais-stream/README.md
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
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
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Standard default/appstore/firetv debug builds do not need `family.properties`.
- [x] #2 Rais build reads the untracked values and has a distinct package ID.
- [x] #3 Visible app name is Rais Stream.
- [x] #4 Launcher/banner can be overridden from the Rais source set.
- [x] #5 Rais uses its configured update feed.
- [x] #6 No credential, token, API key, password, or signing material is committed.
- [x] #7 Missing Rais values fail with a concise actionable build error.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Extend the existing version flavour dimension with a Rais-only, validated family configuration and shared BuildConfig defaults; verify standard variants still configure without family.properties.
2. Reuse the existing app resources and updater preference path for Rais name, neutral launcher/banner overrides, and configured update feed.
3. Add the untracked configuration boundary and exact build/setup documentation without secrets.
4. Verify missing Rais configuration fails actionably, then assemble DefaultDebug and RaisDebug with placeholder HTTPS values; inspect the complete diff and acceptance criteria.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented the Rais Stream private build boundary.

Files/components:
- app/build.gradle.kts: added the Rais version flavour, Rais-only family.properties loading, validation task, distinct application ID, endpoint/update BuildConfig strings, family marker, and WPHN-015 capability booleans with unchanged standard defaults.
- app/src/main/java/com/github/damontecres/wholphin/preferences/AppPreference.kt and ui/preferences/PreferencesViewModel.kt: reused the existing update preference/checker path for the Rais feed, including release notes.
- app/src/rais and app/src/raisDebug resources: Rais Stream name plus neutral launcher/banner overrides for base and API 26+ resource selection.
- .gitignore and family.properties.example: untracked deployment configuration boundary with placeholder-only committed example.
- docs/rais-stream/README.md: exact setup and :app:assembleRaisDebug command.

Verification:
- WPHN-001 dependency: Done; git merge-base --is-ancestor 0ed62c2b HEAD exited 0.
- .\gradlew.bat :app:assembleDefaultDebug --console=plain: passed without family.properties (final run, 73 tasks).
- .\gradlew.bat :app:assembleRaisDebug --console=plain without family.properties: failed as required at validateRaisConfiguration, naming all four keys and the copy/setup action.
- .\gradlew.bat :app:assembleRaisDebug --console=plain with ignored placeholder HTTPS config: passed (final run, 74 tasks).
- Generated BuildConfig: distinct com.example.raisstream.debug package, supplied Jellyfin/Seerr/update URLs, family marker true, four capability flags false; standard BuildConfig retains empty endpoints, public feed, family false, capabilities true.
- aapt2 dump badging: application label Rais Stream with Rais launcher and banner; merge provenance confirms base and API 26+ icon/banner assets come from app/src/rais.
- adb install -r both armeabi-v7a APKs: both succeeded; pm list packages shows com.github.damontecres.wholphin.debug and com.example.raisstream.debug together.
- git diff --check: passed; secret-pattern scan: none; family.properties is absent, ignored, and untracked.
- Independent orchestrate reviewer: ship after two corrections (API 26+ resource overrides and configured-feed release notes).

Deviations: Added app/src/raisDebug/res/values/strings.xml because the existing debug source set otherwise overrides Rais Stream with Wholphin (Debug). No automated tests were added; the task specifies builds/config failure and APK coexistence, which were directly exercised.

Human acceptance: accepted by the user on 2026-09-01 with instruction to push.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added a private Rais version flavour backed by ignored, validated non-secret configuration; reused the existing update preference/checker path; added Rais naming and neutral launcher/banner overrides; documented setup. DefaultDebug and RaisDebug builds passed, missing Rais configuration failed actionably, both APKs installed side-by-side, and independent review returned ship.
<!-- SECTION:FINAL_SUMMARY:END -->

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
