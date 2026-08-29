# Verification matrix

## 1. Principle

Use one small automated check for non-trivial logic and manual TV checks for focus geometry. The goal is to catch regressions without freezing visual layout or making routine UI edits expensive.

## 2. Android automated checks

| Area | Minimum check | Avoid |
|---|---|---|
| Video-like library classification | table-driven JVM test across known collection types/names | Compose test for each library |
| Icon matching | table-driven normalized-name test and type fallback | screenshot/glyph pixel comparison |
| Back-stack preview/browse | small ViewModel/manager test if current navigation test harness supports it | end-to-end UI automation |
| Managed contract | decode canonical fixture, reject version 2, invalid-cache replacement test | raw JSON property-order comparison |
| Row extensions | request construction tests only where handlers are already testable | mocking the entire Jellyfin SDK |
| Season target | pure selection tests for explicit, Next Up, first-unwatched, fallback | exact UI focus-coordinate test |
| Stale season load | coroutine test proving old generation cannot commit | timing-sensitive real network test |
| People split | pure cast/crew classifier test if role mapping is non-trivial | layout snapshots |
| Seerr Quick Connect | fake API state-machine test for success, cancel, timeout, expired session | live Seerr in unit test |

Suggested command after relevant tasks:

```powershell
.\gradlew.bat :app:testDefaultDebugUnitTest
```

Also compile the touched flavours:

```powershell
.\gradlew.bat :app:assembleDefaultDebug
.\gradlew.bat :app:assembleRaisDebug
```

## 3. Plugin automated checks

| Area | Check |
|---|---|
| Contract | serialize model and compare parsed JSON tree with canonical fixture |
| Validation | duplicates, missing parent, invalid limit, invalid assignment, duplicate Genres |
| Assignment | zero/one profile per user and missing-profile behaviour |
| Access filtering | inaccessible hub/parent omitted, global row retained |
| Revision | stable for same effective layout; changes when effective layout changes |
| Endpoint | authenticated 200, unassigned 204, unauthenticated rejection |

Commands:

```powershell
dotnet build -c Release
dotnet test -c Release
```

## 4. Required manual focus scenarios

Run on at least one fast device and one lower-powered/Fire TV-class device before release.

### Drawer previews

- Rapidly move Home -> Search -> Movies -> TV faster than 300 ms; only the final item previews.
- Pause on Home; Home restores without reload.
- Pause on Search; page displays but keyboard remains closed.
- OK Search; input focuses and keyboard opens.
- Pause on a library; hub displays while drawer focus remains.
- D-pad right; first media card focuses, not Genres.
- OK a library; full browser opens; Back returns to hub.
- Repeated previews do not require repeated Back presses.

### Hub rows and spotlight

- Empty first row is skipped for initial focus.
- Spotlight shows first available item before card focus.
- Moving cards changes spotlight.
- Switching libraries clears previous genre and spotlight state.
- Movie/general fallback row order matches specification.
- TV row order includes Next Up and no Studios.
- Collections include qualifying TV/mixed collections and exclude unrelated ones.

### Genres

- Left/right focus does not change content.
- OK activates genre and keeps selector focus.
- First filtered result becomes spotlight fallback.
- Down enters grid; Up returns to active pill.
- Back restores Home mode and is consumed once.
- OK Home restores rows without leaving hub.
- Switching drawer library while filtered starts the new hub on Home.

### Seasons

- Enter a show with progress and land on the intended Next Up/unwatched episode.
- Enter a fully watched/no-progress show and land on episode one.
- Traverse seasons faster than 200 ms; no unnecessary loads visibly commit.
- Pause on season; episode row updates while tab keeps focus.
- Target first unwatched episode in changed season.
- OK season enters target episode.
- Rapidly reverse direction during loading; stale season never appears.

### Cast/Crew

- One D-pad down crosses the section while it is in outer mode.
- OK enters names; first available Cast/Crew name focuses.
- Up/down and left/right follow column rules.
- OK opens Person details.
- Back returns to one-section focus.
- Image preference restores existing person cards.

### Watch List and restrictions

- Watch List appears immediately above Settings.
- Existing favourites appear with renamed labels.
- No delete/file-path/server-edit/theme/screensaver/media-management UI is exposed in Rais.
- Watched, Watch List, playlist, stream, audio and subtitle controls remain.

### Managed layouts

- Assigned user sees cached layout immediately after relaunch.
- Updated server layout replaces cache without a blank page.
- Offline launch uses last valid cache.
- Invalid remote response leaves cache intact.
- Unassigned user sees existing local customiser and settings.
- Assigned user sees read-only status and manual Refresh.
- User without a library does not see its hub or rows.

### Onboarding

- Clean Rais install skips server URL entry.
- Preconfigured URL failure offers Retry and Advanced escape hatch.
- Jellyfin username/password login works.
- Jellyfin Quick Connect works.
- Existing authenticated user restores without setup.
- Seerr Quick Connect creates a separate code and completes.
- Seerr cancellation leaves playback usable with Discover inactive.
- Seerr session restores after process death without stored plaintext password.
- Expired Seerr session exposes Reconnect without breaking Jellyfin login.

## 5. Display/device matrix

Minimum release sample:

| Class | Resolution | Required |
|---|---:|---:|
| Nvidia Shield or equivalent fast Android TV | 4K | Yes |
| Fire TV-class lower-powered device | 1080p or 4K | Yes |
| Chromecast/Google TV | mixed | Preferable |
| television-native Android TV | mixed | Preferable |

Check:

- focus animation remains responsive;
- dwell cancellation feels consistent;
- no text is clipped at 1080p;
- spotlight/cards are not excessively small at 4K density;
- Cast/Crew columns remain readable;
- long library/genre/person names ellipsize rather than distort navigation.

## 6. Release gate

Do not block release on deferred branding. Block release on:

- wrong Back stack;
- focus traps;
- keyboard opening during Search preview;
- stale season result;
- layout cache corruption/loss;
- user-access leak;
- embedded secret;
- stored Seerr plaintext password;
- Rais and standard package ID collision;
- failure to install/load plugin on Jellyfin 12.0.0-rc6.
