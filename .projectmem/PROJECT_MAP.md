# Project Map - Wholphin

## Project purpose
Wholphin is an open-source, television-first Android client for Jellyfin. This fork also carries the checked-in Rais Stream implementation plan for a focused product flavour, managed layouts, simplified TV navigation, and private onboarding while preserving standard Wholphin builds.

## Stack
- Kotlin 2.3 targeting JVM 11, Android SDK 36 (minimum SDK 23)
- Gradle Kotlin DSL with Android application and library modules
- Jetpack Compose for TV UI, Navigation 3, Hilt, Room, WorkManager, DataStore, Kotlin serialization, protobuf, and generated Seerr API sources
- Jellyfin Kotlin SDK with ExoPlayer and optional MPV playback

## Entry points
- `app/src/main/AndroidManifest.xml` — declares the TV application, launcher activity, services, permissions, and deep-link intents.
- `app/src/main/java/com/github/damontecres/wholphin/WholphinApplication.kt` — Hilt application and process-level initialization.
- `app/src/main/java/com/github/damontecres/wholphin/MainActivity.kt` — Android activity, injected services, lifecycle, and Compose host.
- `app/src/main/java/com/github/damontecres/wholphin/ui/nav/ApplicationContent.kt` — root Compose navigation and destination content.

## Structure
- `README.md` — product purpose, supported platforms, features, compatibility, and contributor entry point.
- `DEVELOPMENT.md` — local Android development and build guidance.
- `CONTRIBUTING.md` — contribution and repository-check conventions.
- `settings.gradle.kts` — repositories and the `:app` / `:wholphin-mpv-stub` module graph.
- `build.gradle.kts` — root plugin declarations.
- `gradle/libs.versions.toml` — central plugin and dependency versions.
- `app/` — Android TV application module.
  - `app/build.gradle.kts` — Android configuration, product flavours, generated sources, dependencies, test options, and packaging.
  - `app/src/main/java/com/github/damontecres/wholphin/data/` — Jellyfin repositories, persistence, and data models.
  - `app/src/main/java/com/github/damontecres/wholphin/preferences/` — application, interface, playback, and user preference state.
  - `app/src/main/java/com/github/damontecres/wholphin/services/` — navigation, network, media, scheduling, lifecycle, and integration services.
  - `app/src/main/java/com/github/damontecres/wholphin/ui/` — Compose screens, navigation, reusable components, and detail flows.
  - `app/src/test/` — JVM unit tests and test resources.
  - `app/src/androidTest/` — Android instrumentation tests.
  - `app/src/main/seerr/` — Seerr OpenAPI definition and generator templates.
- `wholphin-mpv-stub/` — fallback Android library used when a real MPV AAR or extension package is unavailable.
- `docs/rais-stream/plan/` — authoritative Rais Stream architecture, sequencing, source map, verification matrix, and canonical fixtures.
  - `docs/rais-stream/plan/README.md` — implementation-package workflow and build commands.
  - `docs/rais-stream/plan/MASTER-IMPLEMENTATION-PLAN.md` — fixed product decisions, phases, risks, and version-one definition of done.
  - `docs/rais-stream/plan/architecture/source-map.md` — existing paths to reuse and bounded expected additions.
  - `docs/rais-stream/plan/architecture/verification-matrix.md` — lean automated checks and required Android TV scenarios.
  - `docs/rais-stream/plan/architecture/examples/managed-layout-v1.json` — canonical managed-layout contract fixture.
- `backlog/tasks/APP-*.md` — sequenced Android Rais Stream implementation tasks managed through the Backlog CLI.
- `.github/workflows/` — continuous integration, build, and release automation.

## Relationships
- `settings.gradle.kts` includes `app/build.gradle.kts` and `wholphin-mpv-stub/build.gradle.kts` as the two Gradle modules.
- `app/src/main/AndroidManifest.xml` launches `MainActivity.kt` under `WholphinApplication.kt`.
- `MainActivity.kt` hosts `ui/nav/ApplicationContent.kt` and coordinates injected repositories and services.
- `ui/nav/ApplicationContent.kt` dispatches destinations into screens under `ui/`, backed by state from `services/`, `preferences/`, and `data/`.
- `app/build.gradle.kts` generates Seerr client sources from `app/src/main/seerr/seerr-api.yml` and selects real or stub MPV dependencies according to the local environment.
- `docs/rais-stream/plan/MASTER-IMPLEMENTATION-PLAN.md` defines task order; each `backlog/tasks/APP-*.md` task narrows that plan to one reviewable change.
- `docs/rais-stream/plan/architecture/examples/managed-layout-v1.json` is the canonical source for the Android and plugin test fixtures; APP-001 owns the Android copy only.
