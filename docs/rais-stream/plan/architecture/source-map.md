# Source map

This map is a starting point, not permission to edit every listed file. Each task should inspect the current branch and touch only the files required by the real call path.

## 1. Current Android files to reuse

| Current path | Current responsibility | Primary tasks |
|---|---|---|
| `app/src/main/java/com/github/damontecres/wholphin/ui/nav/Destination.kt` | Navigation keys | APP-002, APP-003, APP-004 |
| `.../ui/nav/DestinationContent.kt` | Destination-to-screen dispatch | APP-002, APP-008, APP-017 |
| `.../services/NavigationManager.kt` | Back stack and drawer navigation | APP-003, APP-004 |
| `.../ui/nav/NavDrawer.kt` | Drawer focus, click, icon rendering | APP-004, APP-012, APP-013 |
| `.../services/NavDrawerService.kt` | Libraries/built-ins and drawer ordering | APP-002, APP-013 |
| `.../ui/search/SearchPage.kt` | Search input activation and results | APP-004 |
| `.../ui/main/HomePage.kt` | Reusable spotlight and row content | APP-006, APP-007, APP-017 |
| `.../ui/main/HomeViewModel.kt` | Home row loading and state | APP-017 |
| `.../data/model/HomeRowConfig.kt` | Serializable row definitions/options | APP-005, APP-016 |
| `.../services/HomeSettingsService.kt` | Row resolution and fetching | APP-005, APP-006, APP-016, APP-017 |
| `.../ui/main/settings/HomeSettingsViewModel.kt` | Local Home editor and persistence | APP-017 |
| `.../ui/components/RecommendedContent.kt` | Existing multi-row loading pattern | APP-006 reference only |
| `.../ui/components/CollectionFolderView.kt` | Existing filter/grid provider and focus | APP-007, APP-008 |
| `.../ui/detail/CollectionFolderMovie.kt` | Current Movie tabs and full-library config | APP-008 |
| `.../ui/detail/CollectionFolderTv.kt` | Current TV tabs, genre/studio config | APP-008 |
| `.../ui/components/GenreCardGrid.kt` | Existing genre query/destination logic | APP-007 reference; do not duplicate query logic |
| `.../data/model/BaseItem.kt` | Destinations and genre filter construction | APP-007, APP-009 |
| `.../services/LatestNextUpService.kt` | Resume and Next Up queries | APP-005, APP-009 |
| `.../ui/detail/series/SeriesOverview.kt` | Series page orchestration | APP-009, APP-010 |
| `.../ui/detail/series/SeriesOverviewContent.kt` | Season and episode focus UI | APP-010 |
| `.../ui/detail/series/SeriesViewModel.kt` | Season/episode requests and initial target | APP-009 |
| `.../ui/components/TabRow.kt` | Existing focusable tab row | APP-010 |
| `.../data/model/Person.kt` | Person name, role, kind, image | APP-011 |
| `.../ui/cards/PersonRow.kt` | Current image person row | APP-011 |
| `.../ui/cards/PersonCard.kt` | Current image person card | APP-011 reuse when images enabled |
| `.../preferences/AppPreference.kt` | Settings definitions and screens | APP-011, APP-015, APP-017 |
| `app/src/main/proto/WholphinDataStore.proto` | App preference schema | APP-011 |
| `.../preferences/AppPreferencesSerializer.kt` | Defaults for new installs | APP-011 |
| `.../services/ThemeSongPlayer.kt` | Theme music runtime | APP-015 |
| `.../services/ScreensaverService.kt` | Screensaver runtime | APP-015 |
| `.../services/MediaManagementService.kt` | Delete/management capability | APP-015 |
| `.../MainActivity.kt` and `.../MainContent.kt` | App shell, screensaver and resume hooks | APP-015, APP-016 |
| `app/build.gradle.kts` | Product flavours and BuildConfig | APP-014 |
| `app/src/main/AndroidManifest.xml` | launcher/banner/DreamService | APP-014, APP-015 |
| `.../ui/setup/SwitchServerViewModel.kt` | Existing server validation/add flow | APP-018 |
| `.../ui/setup/SwitchUserViewModel.kt` | Jellyfin credentials and Quick Connect | APP-018 |
| `.../services/UserSwitchListener.kt` | user-specific Home/Seerr restore | APP-016, APP-019 |
| `.../services/SeerrServerRepository.kt` | Current Seerr login/session state | APP-019 |
| `.../services/SeerrApi.kt` | Generated Seerr client wrapper | APP-019 |
| `.../services/hilt/AppModule.kt` | OkHttp/Jellyfin/Seerr providers | APP-019 |
| `app/src/main/seerr/seerr-api.yml` | Generated Seerr API source | APP-019 |

## 2. Expected new Android files

Names may move slightly to match the project’s current package conventions. Do not create all of these pre-emptively.

```text
ui/library/LibraryHub.kt
ui/library/LibraryHubViewModel.kt
ui/library/LibraryBrowse.kt
ui/library/GenreSelectorRow.kt
ui/library/LibraryType.kt                 # pure video-like classification only
ui/nav/DrawerPreviewController.kt         # only if ViewModel-local job becomes duplicated
ui/people/PeopleSection.kt
ui/people/PeopleSectionState.kt           # only if state cannot remain local
services/ManagedLayoutApi.kt
services/ManagedLayoutService.kt
services/ManagedLayoutCache.kt
services/SeerrSessionStore.kt
services/SeerrQuickConnectService.kt       # thin state flow, not a second repository
services/hilt/SeerrNetworkModule.kt        # optional if AppModule becomes crowded
data/model/ManagedLayout.kt
ui/main/settings/ManagedLayoutStatusPage.kt
util/LibraryIconResolver.kt
```

Prefer fewer files when one existing file remains readable. A single-use helper does not automatically deserve a service or model file.

## 3. Existing plugin reference repository

The experimental `damontecres/jellyfin-plugin-wholphin` contains useful examples of:

- registering a plugin page;
- an authenticated controller;
- polymorphic row JSON;
- the current Wholphin row vocabulary.

Do not inherit its old assumptions:

- global Home-only configuration;
- YAML as the main admin interface;
- Jellyfin 10.11 package versions;
- `net9.0` target;
- global layout response without per-user assignment/access filtering;
- `CustomEndpoint` editor as a version-one requirement.

## 4. Expected plugin repository shape

Recommended repository: `jellyfin-plugin-wholphin-layout`

```text
Jellyfin.Plugin.WholphinLayout.sln
Directory.Build.props
README.md
LICENSE
Jellyfin.Plugin.WholphinLayout/
  Jellyfin.Plugin.WholphinLayout.csproj
  Plugin.cs
  Configuration/
    PluginConfiguration.cs
  Api/
    LayoutController.cs
  Models/
    ConfigurationModels.cs
    ManagedLayoutEnvelope.cs
    HomeRowConfig.cs
  Services/
    LayoutValidator.cs
    EffectiveLayoutService.cs
    UserAccessFilter.cs
    LayoutRevision.cs
  Pages/
    Layout/index.html
    Layout/index.js               # separate only if supported cleanly by plugin page embedding
Jellyfin.Plugin.WholphinLayout.Tests/
  ContractFixtureTests.cs
  LayoutValidatorTests.cs
  EffectiveLayoutTests.cs
  AccessFilterTests.cs
fixtures/
  managed-layout-v1.json
.github/workflows/
  build.yml
  release.yml
```

Plugin ID fixed for the plan:

```text
6f601681-320f-59ab-a58f-9aca481fcaf7
```

Generate a different ID only before the first public release and update every manifest/config reference in the same commit.

## 5. Cross-repository contract files

Keep semantically identical copies:

```text
Android: app/src/test/resources/managed-layout/managed-layout-v1.json
Plugin:  fixtures/managed-layout-v1.json
```

The canonical source in this package is:

```text
architecture/examples/managed-layout-v1.json
```

Do not create a shared package, Git submodule, schema-generation build, or repository dependency for one JSON fixture.
