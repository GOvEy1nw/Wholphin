# Interaction specification

This document is the behavioural source of truth for focus and navigation. Visual values may be tuned later, but changing these state transitions requires an explicit product decision.

## 1. Terminology

- **Drawer preview:** changing the page shown beside the navigation drawer without moving focus into that page.
- **Activate:** OK/Enter on a focused drawer item or selector item.
- **Hub Home mode:** normal configured/fallback library rows with `Home` selected in the genre selector.
- **Hub Genre mode:** normal rows replaced by one filtered library grid.
- **Spotlight:** the header that describes the focused media item, or the first available item when no media card currently has focus.
- **Section entry focus:** the element requested when the user crosses from a parent navigation surface into its content.

## 2. Global timing constants

Keep these as named constants close to the behaviour that consumes them. Do not expose them as user settings in version one.

```kotlin
const val DRAWER_PREVIEW_DELAY_MS = 300L
const val SEASON_PREVIEW_DELAY_MS = 200L
```

A delayed action must be cancelled as soon as focus leaves the source item. A result must also verify that it still belongs to the current request before changing visible state.

## 3. Navigation drawer

## 3.1 Home

### Focus

1. Home receives focus.
2. Start a cancellable 300 ms job.
3. At completion, if Home still has focus:
   - remove any drawer-preview destination above the existing Home destination;
   - show the existing Home state;
   - keep focus in the drawer;
   - do not reload rows.

### OK

- If Home is already displayed, preserve Wholphin’s current explicit reload behaviour.
- If another page is displayed, navigate to Home and then apply the same explicit reload behaviour only if the existing implementation already does so on click.
- Move focus into Home according to the current drawer close behaviour.

## 3.2 Search

### Focus

1. Search receives focus.
2. Start a cancellable 300 ms job.
3. At completion, display `Destination.Search` in **preview** mode.
4. Keep focus in the drawer.
5. The Search field must not request focus and the keyboard must not open.

### OK

- Switch the existing Search destination to **activated** mode rather than stacking another Search page.
- Close/exit the drawer according to current behaviour.
- Request focus on the Search field.
- Open the keyboard through the existing input mechanism.

### Back

- Standard Search Back behaviour applies after activation.
- Merely previewing Search must not create a trail of Search destinations.

## 3.3 Video library

### Focus

1. Library item receives focus.
2. Start a cancellable 300 ms job.
3. At completion, replace the current preview with that library’s `LibraryHub` destination.
4. Reset the new hub to Genre `Home` mode.
5. Keep focus in the drawer.
6. Do not request Genre focus.

### D-pad right from drawer

- Close/leave the drawer.
- Request the first media card in the first non-empty media row.
- Skip loading, error, empty, and selector-only rows.
- Do not request the genre selector, even though it is visually above the media rows.
- The selected media becomes the spotlight item.

### OK on drawer library

Build this stack shape:

```text
Home
LibraryHub(current library)
LibraryBrowse(current library)
```

Then move focus into the full browser. Back returns to the same hub, including its existing in-memory focus state when Navigation 3 retains that entry.

### Non-video library

Keep the current click/navigation behaviour. Do not force Music, Photos, Live TV, playlists, or other specialised pages through `LibraryHub`.

## 3.4 Items that do not preview

The following remain click-only in version one:

- profile/server item;
- Watch List;
- Settings;
- More expansion control;
- Now Playing;
- any future administrative item.

Focusing one of these cancels a pending preview job.

## 4. Library hub

## 4.1 Initial state

On each new hub destination:

```text
activeGenre = Home
focusedMedia = null
spotlightFallback = first available item across active content rows
contentMode = rows
```

The genre selection is not shared between libraries. Previewing another library always starts that new hub in Home mode.

## 4.2 Spotlight resolution

Resolve the spotlight item in this order:

```text
1. currently focused media item in the active rows/grid
2. first non-null media item in the active rows/grid
3. null while loading or when the active source is empty
```

Do not preserve a spotlight item from a previous library or previous genre after the active content source changes.

## 4.3 Hub Home mode

Layout order:

```text
Spotlight (not independently focusable)
Genre selector
Configured/fallback media rows
```

The genre selector is visually above the media rows but is not the section-entry target from the drawer.

### Moving up from first media row

- Move focus to the currently active genre pill (`Home` in normal mode).

### Moving down from genre selector

- Enter the first media card in the first non-empty row.
- If no media is available, retain focus on the selector and show the appropriate empty/loading state.

## 4.4 Genre selector

### Visual behaviour

- Horizontal, text-only row.
- `Home` first.
- Subtle pill/background and normal TV focus treatment.
- Selected and focused are distinct states.
- No artwork, icons, or poster cards.

### Focus

- Left/right changes focus only.
- Focus does not run a query or change the active genre.

### OK on a genre

1. Set the selected genre.
2. Keep focus on that pill.
3. Start the existing filtered-library query using the hub library, genre ID, and correct media type.
4. Replace normal rows with the filtered grid in place.
5. When the first result is available, use it as spotlight fallback.
6. Do not automatically move focus into the grid.

### OK on Home

- Cancel an in-flight genre request.
- Restore configured/fallback rows.
- Clear any focused item from the old genre result.
- Use the first available row item as spotlight fallback.
- Keep focus on Home.

### Down in genre mode

- Focus the first filtered result.

### Up from grid first row

- Return to the active genre pill.

### Back in genre mode

The first Back is consumed by the hub:

```text
activeGenre = Home
contentMode = rows
focus = Home pill
```

A subsequent Back follows normal page/drawer navigation.

## 4.5 Empty rows and fallback

- Empty content rows are omitted from the focus traversal.
- A hub with no available media keeps the selector accessible and shows a single clear empty state.
- A plugin-managed row that returns 403/404 is treated as unavailable, not as a fatal hub error.

## 5. Full library browser

The browser reuses the former Library-tab configuration:

- correct include item types;
- recursive behaviour;
- filters and sorting;
- remembered view options;
- play-enabled state where currently supported;
- context actions permitted by the Rais feature policy.

The browser is a separate destination. Genre filtering from the hub does not alter the browser’s saved filter state.

## 6. TV series seasons

## 6.1 Layout

```text
Series/episode spotlight and details
Season selector
Episode row
Episode controls/footer
Cast/Crew and later sections
```

## 6.2 Initial target

Precedence:

1. Explicit episode/season carried by `Destination.SeriesOverview`.
2. Jellyfin’s series-specific Next Up/next unwatched episode.
3. First season with an unplayed count, then that season’s first unplayed episode.
4. First season and first episode.

The selected season tab and episode-row focus index must come from one resolved target so they cannot disagree.

## 6.3 Focus-changing season

1. A season tab receives focus.
2. Start a cancellable 200 ms job.
3. If focus remains, select that season and request episodes.
4. Keep focus on the season selector.
5. Select the first unplayed episode index when results arrive; otherwise index zero.
6. Update the spotlight to that target episode.
7. Do not jump into the episode row.

## 6.4 OK on season

- Select/load it if necessary.
- When a target episode is ready, move focus into that episode card.

## 6.5 Rapid traversal

Every season load has both protections:

- cancel the previous episode/extras jobs;
- compare the response’s season ID or generation token before updating state.

A stale response may log and return, but must never change episodes, extras, selected index, or spotlight.

## 7. Cast and Crew

## 7.1 Default presentation

```text
Cast                         Crew
Actor Name                   Director Name
Character                    Director

Actor Name                   Writer Name
Character                    Writer
```

- Cast: actors and guest stars.
- Crew: directors, writers, producers, composers, creators and all other non-cast roles returned by Jellyfin.
- Preserve server ordering within each column.
- Render all returned people.

## 7.2 Outer focus mode

- The full section is one focusable container.
- Individual names are not focusable.
- Normal up/down page traversal crosses the section in one press.
- The container has a visible focused state around the section or its heading, not around every name.

## 7.3 Internal focus mode

OK on the container:

- enable child focus;
- focus first Cast entry, otherwise first Crew entry;
- retain the page’s current scroll position as far as possible.

Within:

- Up/down stays in the current column.
- Left/right targets `min(currentIndex, otherColumn.lastIndex)`.
- OK opens the existing Person page.
- Back disables child focus and returns focus to the section container.
- Leaving the section through normal page traversal also clears internal mode.

## 7.4 Image-card preference

When `Show people images` is enabled, retain/reuse Wholphin’s current image-card row presentation. Do not build a second two-column image mode.

## 8. Watch List

- Fixed directly above Settings.
- Click-only; no focus preview.
- Uses existing Jellyfin favourite state and page.
- Visible actions use `Add to Watch List` / `Remove from Watch List`.
- No new database or watch-later semantics.

## 9. Managed layout refresh UI

Managed status page shows:

- profile name;
- profile revision or update time;
- last successful remote refresh;
- source currently rendered: remote, cached, or fallback;
- stale/offline/error message when applicable;
- `Refresh managed layout` button.

The page contains no row editing controls. An unassigned/unmanaged user continues to see the existing Home customiser.

## 10. Seerr onboarding

After Jellyfin authentication:

1. Check for a valid stored Seerr session for the Jellyfin user and configured Seerr URL.
2. If valid, continue without a prompt.
3. Otherwise, prefer Seerr Quick Connect:
   - initiate a new Seerr transaction;
   - display its code;
   - poll the Seerr check endpoint with cancellation and a finite timeout;
   - call Seerr’s final authenticate step;
   - persist the resulting session cookie/state.
4. Never reuse the Jellyfin Quick Connect code; it is a separate transaction.
5. If Seerr Quick Connect is unavailable, offer credential fallback:
   - accept Jellyfin username/password;
   - establish Seerr’s Jellyfin-mode session;
   - discard the password immediately after the response;
   - persist only session state.
6. Cancelling or failing Seerr setup must not block Jellyfin playback. Discover/request features remain disabled and Settings shows a reconnect action.
