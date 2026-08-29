# Master implementation plan

## 1. Goal

Create a cleaner, family-oriented Android TV experience called **Rais Stream** while preserving Wholphin’s playback stack and keeping the general application changes maintainable and potentially upstreamable.

The result has four connected parts:

1. **Focus-driven top-level navigation** for Home, Search, and video-library hubs.
2. **Unified video-library hubs** with a media-linked spotlight, fixed text genre selector, curated content rows, embedded genre filtering, TV collections, and click-through full-library browsing.
3. **Simplified detail navigation** through focus-selected TV seasons and a names-first, two-column Cast/Crew section.
4. **A small Jellyfin layout plugin** that publishes reusable, per-user effective layouts without implementing smart collections itself.

A private `rais` product flavour supplies preconfigured endpoints, application identity, family onboarding, and disabled features. It contains no embedded credentials or tokens.

## 2. Baseline and constraints

### Android app baseline

- Repository: `GOvEy1nw/Wholphin`
- Branch: `rais`
- Baseline commit: `fdde98602921b8ecd7f52b3d18f32d3965ba8ab7`
- Architecture: single-activity Kotlin Android TV app using Compose, Navigation 3, Hilt, Room, DataStore, Jellyfin Kotlin SDK, Media3, MPV, Coil, and OkHttp.

### Plugin baseline

Build a new, narrow plugin repository. The existing experimental `damontecres/jellyfin-plugin-wholphin` is useful as a source-map reference, but should not be copied wholesale because it targets an older Jellyfin release and provides a global YAML configuration rather than reusable profiles with per-user access filtering.

First target only:

```text
Jellyfin server: 12.0.0-rc6
Plugin target framework: net10.0
Jellyfin.Controller: 12.0.0-rc6
Jellyfin.Model: 12.0.0-rc6
```

The first plugin task must prove those versions against the running server. Do not build a compatibility abstraction until another concrete server version is actually required.

### Device constraints

The interaction model must work with ordinary D-pad, OK, and Back input across:

- Nvidia Shield
- Fire TV
- Chromecast / Google TV
- television-native Android TV
- mixed 1080p and 4K displays
- unknown Android and Fire OS versions within Wholphin’s existing supported range

Avoid touch assumptions, hover-only controls, tiny focus targets, nested horizontal navigation without a clear exit, and timing behaviour that depends on high-end hardware.

## 3. Product decisions fixed by this plan

### Navigation

- Home, Search, and video-library drawer entries preview after a cancellable **300 ms** focus dwell.
- Search preview does not activate the input or keyboard; OK does.
- Library focus shows its hub; OK opens its full browsable library.
- Repeated previews replace the current preview destination instead of growing the back stack.
- Back from full browse returns to the corresponding hub.
- Entering a hub from the drawer always targets the first media item in the first non-empty media row, never the genre selector.

### Library hubs

Apply the new hub only to video-like libraries. Music, Photos, Live TV, playlists, and other specialised screens keep their existing flows unless a later task explicitly changes them.

Fallback rows:

**Movies, Documentaries, Kids, Home Videos, concerts, mixed video libraries**

```text
Spotlight tied to selected/first media
Genres selector
Continue Watching
Recently Released
Recently Added
Top Unwatched
Suggestions
Collections
```

**TV libraries**

```text
Spotlight tied to selected/first media
Genres selector
Continue Watching
Next Up
Recently Released
Recently Added
Top Unwatched
Suggestions
Collections
```

Studios are removed from the TV hub. The underlying general Studio components do not need to be deleted.

### Genres

- `Home` is always the active genre when a hub is first shown or when switching libraries.
- Genre focus alone does nothing; OK activates it.
- An active genre replaces normal rows with an in-place filtered grid.
- The genre row retains focus during loading.
- The spotlight uses the first filtered result until a grid item receives focus.
- Down enters the grid; Up returns to the active pill.
- The first Back while filtered restores `Home` mode.

### TV seasons

- Season selector sits between the media header/spotlight and episode row.
- A cancellable **200 ms** dwell changes the selected season while retaining focus on the selector.
- On first entry, prefer the series-specific next/unwatched episode in an in-progress season.
- On manual season change, prefer that season’s first unwatched episode; otherwise episode one.
- Rapid season traversal must never allow an old request to overwrite a newer selection.

### People

Default to a text-only Cast/Crew section:

- Cast left; crew right.
- Name primary; character/job secondary.
- No portraits or fallback icons.
- The entire section is one focusable item until OK enters internal navigation.
- Inside: Up/Down within a column; Left/Right to the nearest corresponding entry in the other column; Back exits to the section container.
- Show all returned people.
- A preference can restore the existing image-card presentation.

### Watch List and removed features

- Visible `Favourites` terminology becomes `Watch List`; Jellyfin’s existing favourite flag remains the data source.
- Watch List is fixed immediately above Settings.
- Rais Stream hides and force-disables theme music, screensaver, delete/media-management actions, file-path/technical management actions, and ordinary server-editing controls.
- Mark watched, Watch List, playlists, playback version, audio, and subtitle controls remain.
- Dormant implementation code is retained where possible to reduce merge cost.

### Private onboarding

- One preconfigured HTTPS Jellyfin URL and one preconfigured HTTPS Seerr URL.
- Skip ordinary server-address entry, but retain an Advanced escape hatch.
- Support Jellyfin credentials and Jellyfin Quick Connect.
- Use Seerr’s own Quick Connect transaction as the preferred Seerr onboarding path after Jellyfin login.
- Do not reuse a Jellyfin Quick Connect code, Jellyfin token, or embedded secret for Seerr.
- Persist the Seerr session, not a plaintext password.
- Direct Seerr credential login is a fallback and must discard the password after establishing a session.

## 4. Architectural choices

## 4.1 Split hub preview from full browsing

Add explicit destinations rather than overloading `Destination.MediaItem`:

```kotlin
Destination.LibraryHub(...)
Destination.LibraryBrowse(...)
```

A drawer library entry holds both destinations:

```text
focus -> LibraryHub
OK    -> LibraryBrowse, with LibraryHub directly beneath it on the stack
```

This keeps back behaviour deterministic and lets the full browser reuse current filters, sorting, view options, and remembered library display state.

## 4.2 Reuse the home-row engine

Do not create `LibraryHubRow`, `ManagedRowFetcher`, or another card renderer.

Extend existing `HomeRowConfig` only where required:

- optional library scope on Continue Watching, Next Up, and Combined
- `TopUnwatched`
- `Collections`

Use `HomeSettingsService.fetchDataForRow()` for Home, hubs, plugin-managed layouts, and fallback layouts. Keep `Genres` as the existing row type; hub rendering extracts it into the fixed selector slot.

## 4.3 Keep spotlight as derived UI state

The spotlight is not a configured row. It derives from:

```text
focused media item
    else first media item from the active content source
    else empty/loading header
```

In Home mode, the active source is the configured hub rows. In genre mode, it is the filtered grid. This avoids a separate recommendation query and guarantees the header describes something the user can actually select.

## 4.4 Publish effective layouts from the plugin

The plugin should expose one authenticated endpoint that returns the effective profile for the current Jellyfin user:

```text
GET /WholphinLayout/v1/layout
```

Recommended responses:

- `200` — valid managed layout envelope.
- `204` — plugin exists but no profile is assigned to this user.
- `404` — plugin/endpoint absent; app continues unmanaged.
- `401/403` — session/auth failure; app uses cache or unmanaged fallback and surfaces status.

This is deliberately simpler than copying a profile into every user’s DisplayPreferences. Effective publishing provides:

- one reusable profile for many users;
- filtering at request time based on each user’s actual library access;
- no fan-out writes or synchronisation job;
- immediate profile changes on the next refresh;
- a small plugin configuration model.

Wholphin’s existing local/server Home settings remain intact for unmanaged users.

## 4.5 Stale-while-revalidate managed layouts

For managed users:

1. Read and validate the per-server/per-user cache.
2. Render it immediately.
3. Fetch the current layout in parallel.
4. If valid and revised, atomically replace the cache and active layout.
5. If the fetch fails, retain the cache and show stale/offline status.

Refresh triggers:

- login/user switch;
- app launch;
- managed profile change detected by revision;
- manual Refresh action;
- app resume when last successful refresh is older than **15 minutes**.

No background polling, WebSocket, push service, or WorkManager job is needed in version one.

## 4.6 Isolate Rais Stream in a product flavour

Add a `rais` flavour alongside current app variants. Load private values from an untracked root `family.properties`, with a committed `family.properties.example` containing placeholders only.

The flavour supplies:

- application ID;
- app name `Rais Stream`;
- Jellyfin URL;
- Seerr URL;
- update feed URL;
- feature-policy flags;
- source-set launcher/banner overrides.

Standard Wholphin builds must compile and behave as before when `family.properties` is absent. The Rais build should fail early with a clear message if required non-secret values are missing.

## 4.7 Give Seerr a dedicated session client

Current Seerr login stores username/password and replays authentication. Replace that path for Rais Stream with:

- a dedicated Seerr OkHttp client;
- a small persistent cookie/session store partitioned by Jellyfin user and Seerr base URL;
- generated API methods for Quick Connect initiate/check/authenticate;
- session validation on user switch;
- reconnect state when the session expires.

Do not add a general credentials vault or new authentication framework. Store only what is necessary to resume the Seerr session, consistent with the app’s existing local session model.

## 5. Shared managed-layout contract

The canonical format is defined in `architecture/managed-layout-contract.md` and `architecture/examples/managed-layout-v1.json`.

Important boundaries:

- Envelope `schemaVersion` starts at `1` and is separate from Wholphin’s local `HomePageSettings.version`.
- Rows reuse `HomeRowConfig` JSON.
- Wrapper metadata carries stable row ID, title override, max items, and View All behaviour.
- Hub surfaces are keyed by Jellyfin library UUID.
- No icon override in version one.
- No arbitrary request/endpoint editor in version one.
- Unknown optional fields may be ignored; unsupported envelope versions reject the remote document without deleting a valid cache.

Both repositories keep a byte-equivalent canonical fixture and a compatibility test.

## 6. Work streams and order

## Phase A — foundations

Run in parallel:

```text
Android: APP-001 -> APP-002 / APP-005 / APP-009 / APP-011 / APP-012 / APP-013 / APP-014
Plugin:  LAY-001 -> LAY-002
```

Success: both projects compile; contract fixture is fixed; no user-facing behaviour is required yet.

## Phase B — app navigation and hubs

```text
APP-002 -> APP-003 -> APP-004
APP-005 + APP-002 -> APP-006 -> APP-007
APP-004 + APP-006 + APP-007 -> APP-008
```

Success: Home/Search/library preview works; video hubs replace tabs; genre filtering remains in the hub; full browsing has correct Back behaviour.

## Phase C — detail navigation and simplification

```text
APP-009 -> APP-010
APP-011
APP-012
APP-013
APP-014 -> APP-015
```

Success: deterministic season targeting, stable rapid switching, Cast/Crew entry mode, automatic icons, fixed Watch List, and family feature policy.

## Phase D — managed layouts

```text
Plugin: LAY-002 -> LAY-003 -> LAY-004 -> LAY-005
App:    APP-001 + contract -> APP-016
App:    APP-006 + APP-007 + APP-016 -> APP-017
Plugin: LAY-003 + LAY-004 -> LAY-006 -> LAY-007
```

Success: cached-first managed Home/hub layouts, access filtering, reusable profiles, and read-only managed status page.

## Phase E — private onboarding

```text
APP-014 -> APP-018 -> APP-019
```

Success: clean Rais install lands on Jellyfin user authentication, supports both Jellyfin login paths, then establishes a Seerr session using Seerr Quick Connect or a non-persisted credential fallback.

## Phase F — release verification

```text
LAY-007 -> LAY-008
all APP feature tasks -> APP-020
```

Success: plugin package installs on Jellyfin 12.0.0-rc6; Android app passes targeted checks and manual device matrix; deferred visual-brand work remains untouched.

## 7. Commit and review strategy

- One backlog task per commit or compact PR.
- A task may touch several files when they are part of the same behaviour; do not split a root-cause fix across callers.
- Do not combine Android and plugin changes in one repository or PR.
- Contract changes require updating both canonical fixtures and their compatibility tests before dependent work continues.
- Generated Seerr client output may be committed with the task that updates its OpenAPI source; do not manually patch generated files.
- Do not commit `family.properties`, credentials, cookies, APK signing material, or Jellyfin/Seerr access tokens.

## 8. Testing policy

Keep automated tests small and durable:

- pure icon-name classification;
- layout envelope decoding/validation and fixture compatibility;
- profile assignment/access filtering;
- stale season-load protection and initial target selection;
- Seerr Quick Connect state transitions using a fake API;
- navigation back-stack semantics where testable without Compose.

Use manual Android TV verification for focus geometry, timing, D-pad transitions, keyboard activation, and cross-device rendering. Do not add screenshot tests, pixel-position assertions, exact text-size tests, or broad UI automation suites.

## 9. Key risks and mitigations

### Focus-triggered network churn

Use cancellable 300 ms/200 ms jobs, identity checks, and stale-response guards. Never launch a permanent flow or polling loop for focus previews.

### Search stealing focus

Separate Search preview from Search activation. The Search page must not request input focus when shown as a drawer preview.

### Old season result overwriting the current season

Cancel the prior load and verify the request generation/season ID before committing episode and extras results.

### Managed layout invalid or unavailable

Validate before activation, retain the last valid cache, and never overwrite a valid cache with an unsupported or partial response.

### Shared profile leaks inaccessible content

Filter on the server using Jellyfin’s current-user access APIs and defensively tolerate 403/404 rows in the client.

### Seerr endpoint drift

Treat the generated Seerr OpenAPI source and a live integration check as the source of truth. The semantic flow is initiate -> check -> authenticate; do not hardcode a path copied from documentation into business logic.

### Private flavour contaminates standard builds

Use source-set overrides and build fields. Standard variants must not read or require `family.properties`.

### Scope creep into smart collections

The plugin only selects existing Jellyfin libraries, collections, and playlists. Existing plugins remain responsible for generating and updating smart collections.

## 10. Definition of done for version one

Version one is complete when:

- Home, Search, and video libraries preview correctly by focus.
- Library OK opens the full browser and Back returns to the hub.
- All video-like libraries use the new hub; Music/Photos/Live TV remain stable.
- Genre filtering stays inside the hub and has the specified Home/Back behaviour.
- The spotlight always tracks an active or first available media item.
- TV seasons focus-switch safely and target next/first unwatched content.
- Text-only Cast/Crew works as a single-entry focus group.
- Library names resolve to sensible icons with tested fallbacks.
- Watch List and family feature restrictions behave as specified.
- Rais Stream installs beside standard Wholphin and skips ordinary server setup.
- Jellyfin and Seerr Quick Connect paths work without embedding or persisting plaintext credentials.
- Plugin profiles configure Home and individual hubs, filter inaccessible content, and publish effective layouts.
- Managed layouts render cached-first, refresh safely, and expose a read-only status page.
- The plugin is built and smoke-tested against Jellyfin 12.0.0-rc6.
- The final visual-branding pass remains a separate future piece of work.
