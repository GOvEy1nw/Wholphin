# projectmem - Wholphin

_Last updated: 2026-08-29_

## Project purpose
Wholphin is an open-source, television-first Android client for Jellyfin. This fork also carries the checked-in Rais Stream implementation plan for a focused product flavour, managed layouts, simplified TV navigation, and private onboarding while preserving standard Wholphin builds.

## Recent issues
- [DONE] #legacy_786b Legacy issue: Fix white flash on app start (#1909) -> Fix white flash on app start (#1909) (fixed)

## Decisions
- Wholphin is a television-first Android Jellyfin client built as a Kotlin/Jetpack Compose app with Hilt-backed services and repositories. [README.md]
- The Gradle project keeps the Android application in :app and uses :wholphin-mpv-stub only when a real MPV AAR or extension dependency is unavailable. [settings.gradle.kts]
- Rais Stream work follows the checked-in master plan and one Backlog APP task at a time, reusing existing navigation, service, and UI paths instead of parallel implementations. [docs/rais-stream/plan/MASTER-IMPLEMENTATION-PLAN.md]
- Rais Stream is isolated through incremental task-scoped changes; standard Wholphin behavior and builds remain protected and deferred features stay outside each task boundary. [docs/rais-stream/plan/README.md]

## Notes
- Translated using Weblate (Danish)
- Translated using Weblate (Danish)
- Translated using Weblate (Danish)
- Translated using Weblate (Chinese (Traditional Han script))
- Translated using Weblate (Danish)
- Translation 2026-08-21 (#1887)
- Release v1.0.7
- Add more searchable types & choose what to search (#1857)
- Update favorites page with more types (#1864)
- Local Android builds target JDK 11 bytecode and SDK 36; unit tests include Android resources, and native MPV extensions are optional for compilation because the stub module is supported. [app/build.gradle.kts]

## Key files
- `app/src/main/res/values-it/strings.xml`
- `app/src/main/res/values-zh-rCN/strings.xml`
- `app/src/main/res/values-sv/strings.xml`
- `app/src/main/res/values-pl/strings.xml`
- `app/src/main/res/values-cs/strings.xml`
- `app/src/main/res/values-tr/strings.xml`
- `app/src/main/res/values-hr/strings.xml`
- `app/src/main/res/values-da/strings.xml`
- `app/src/main/res/values-zh-rTW/strings.xml`
- `v1.0.7`
- `app/src/main/java/com/github/damontecres/wholphin/data/model/HomeRowConfig.kt`
- `app/src/main/java/com/github/damontecres/wholphin/services/LiveTvService.kt`
- `app/src/main/java/com/github/damontecres/wholphin/ui/Formatting.kt`
- `app/src/main/java/com/github/damontecres/wholphin/ui/UiConstants.kt`
- `app/src/main/java/com/github/damontecres/wholphin/ui/cards/SeasonCard.kt`
- `app/src/main/java/com/github/damontecres/wholphin/ui/components/CollectionFolderGrid.kt`
- `app/src/main/java/com/github/damontecres/wholphin/ui/detail/livetv/DvrSchedule.kt`
- `app/src/main/java/com/github/damontecres/wholphin/ui/detail/livetv/LiveTvViewModel.kt`
- `app/src/main/java/com/github/damontecres/wholphin/ui/detail/livetv/TvGuideGrid.kt`
- `app/src/main/java/com/github/damontecres/wholphin/ui/discover/DiscoverSearchPage.kt`

## Open questions
- None logged yet.
