# Managed layout contract — version 1

## 1. Purpose

This contract is the only shared runtime boundary between Rais Stream and the Wholphin Layout Jellyfin plugin.

It publishes the **effective** layout for the currently authenticated Jellyfin user after profile assignment and access filtering. The client does not need to understand plugin profiles, assignments, or admin configuration.

## 2. Endpoint

```http
GET /WholphinLayout/v1/layout
Authorization: existing Jellyfin client authorization header
Accept: application/json
```

Response meanings:

| Status | Meaning | Client action |
|---|---|---|
| `200` | Managed layout returned | Validate, activate, cache atomically |
| `204` | Plugin present; no profile assigned | Use unmanaged local/server settings |
| `401`/`403` | Authentication/access problem | Keep valid cache; surface status |
| `404` | Plugin or endpoint absent | Use unmanaged behaviour |
| `5xx` | Temporary plugin/server failure | Keep valid cache; surface stale status |

Version one does not require ETags, long polling, WebSockets, push notifications, or a separate discovery endpoint.

## 3. Envelope

```kotlin
data class ManagedLayoutEnvelope(
    val schemaVersion: Int,
    val revision: String,
    val generatedAt: Instant,
    val profile: ManagedProfileInfo,
    val home: ManagedSurface,
    val hubs: Map<String, ManagedHubSurface>,
)

data class ManagedProfileInfo(
    val id: String,
    val name: String,
    val updatedAt: Instant,
)

data class ManagedSurface(
    val rows: List<ManagedRow>,
)

data class ManagedHubSurface(
    val libraryId: String,
    val rows: List<ManagedRow>,
)

data class ManagedRow(
    val id: String,
    val titleOverride: String? = null,
    val maxItems: Int? = null,
    val showViewMore: Boolean = true,
    val config: HomeRowConfig,
)
```

The C# model must serialize the same camelCase field names and `HomeRowConfig` discriminator values as the canonical fixture.

## 4. Row contract

Reuse the Android app’s existing polymorphic `HomeRowConfig` JSON. Version-one managed layouts may publish:

- `ContinueWatching`
- `NextUp`
- `ContinueWatchingCombined`
- `RecentlyAdded`
- `RecentlyReleased`
- `TopUnwatched`
- `Suggestions`
- `Genres`
- `ByParent` for an existing library, collection, or playlist
- `Collections`

Required app extensions:

```kotlin
ContinueWatching(parentId: UUID? = null, ...)
NextUp(parentId: UUID? = null, ...)
ContinueWatchingCombined(parentId: UUID? = null, ...)
TopUnwatched(parentId: UUID, ...)
Collections(parentId: UUID, ...)
```

A null watching-row `parentId` retains Home’s global semantics. A hub profile should publish the current library ID.

`Genres` remains an ordinary contract row. The hub renderer extracts at most one `Genres` row and renders it in the fixed selector slot between spotlight and media rows. Version-one plugin profiles allow `Genres` only on Hub surfaces; Wholphin's existing unmanaged Home genre-card behaviour is unaffected.

## 5. Managed row metadata

### `id`

- Stable within a profile surface.
- Non-empty.
- Unique within that surface.
- Used to preserve compatible row state when a refreshed document changes only content.
- Generated once by the plugin UI; do not derive it from list position.

### `titleOverride`

- Null uses the app’s existing resolved title.
- Empty string is invalid; use null.
- Plain text only in version one.

### `maxItems`

- Null uses the app’s Home preference/default limit.
- Valid range: `1..50`.
- Applied when fetching that row.

### `showViewMore`

- Hides the View All card when false.
- Does not change server query semantics beyond the selected limit.

### `config.viewOptions`

The plugin offers a small set of named presets, but publishes the existing concrete `HomeRowViewOptions` object. The client does not need a plugin-specific preset enum.

## 6. Surface rules

### Home

- May contain global or library-scoped rows.
- A row tied to an inaccessible library/parent is omitted by the plugin.
- Normal Wholphin Home rendering applies.

### Hub

- Map key and `libraryId` must be the same valid UUID.
- The plugin omits the entire hub if the current user cannot access the library.
- Only video-like library hubs are consumed by version-one Rais Stream.
- `Genres` is fixed under the spotlight regardless of its configured list position.
- Extra `Genres` rows are rejected by plugin validation.
- Spotlight is never serialized.

### Missing hub

When a managed envelope has no surface for an accessible video library, the app uses its built-in fallback hub for that library. This prevents a shared profile from making newly added libraries unusable.

## 7. Server-side profile model

Recommended configuration objects:

```csharp
PluginConfiguration
  Version
  Profiles: List<LayoutProfile>
  Assignments: List<UserProfileAssignment>

LayoutProfile
  Id
  Name
  UpdatedAtUtc
  Home: LayoutSurfaceConfig
  Hubs: List<HubLayoutConfig>

UserProfileAssignment
  UserId
  ProfileId

LayoutSurfaceConfig
  Rows: List<LayoutRowConfig>

HubLayoutConfig
  LibraryId
  Rows: List<LayoutRowConfig>

LayoutRowConfig
  Id
  Enabled
  SourceType
  ParentId
  TitleOverride
  MaxItems
  ShowViewMore
  ViewPreset
```

Use lists rather than dictionaries in persisted plugin configuration to remain friendly to Jellyfin’s XML configuration serializer. Convert to maps only in runtime response models.

The plugin configuration stores source intent. The endpoint maps it to effective `ManagedRow.config` JSON.

## 8. Profile assignment

- A user has zero or one assigned profile.
- The same profile may be assigned to any number of users.
- Assigning a different profile is the per-user override.
- A missing/deleted profile assignment behaves as unassigned.
- Duplicate assignments for one user are invalid and blocked by validation.

## 9. Access filtering

The endpoint must use Jellyfin’s current authenticated user and Jellyfin’s own visibility/access APIs. Do not recreate permissions from raw policy fields.

Filter rules:

1. Build the set of libraries currently visible to the user.
2. Omit inaccessible hub surfaces.
3. For library-scoped row types, omit the row when `parentId` is inaccessible.
4. For collection/playlist `ByParent`, resolve the parent and omit it when Jellyfin says it is not visible to the user.
5. Keep global Continue Watching/Next Up rows.
6. Do not expose a parent name, title, ID, or row in the response after it is rejected.
7. The client still treats 403/404 row fetches as unavailable in case access changes after publication.

A shared family profile therefore needs no per-user copy.

## 10. Revision

`revision` changes whenever the effective document for that user changes. A deterministic hash is preferred:

```text
sha256(schemaVersion + profile configuration + effective accessible surfaces/rows)
```

Do not include `generatedAt` in the hash. Otherwise every fetch would appear revised.

The plugin may prefix the value, for example:

```text
sha256:12ab34...
```

## 11. Validation

### Plugin before save

Reject:

- duplicate profile IDs or names where names are intended unique;
- duplicate user assignments;
- assignment to missing profile;
- duplicate row IDs within one surface;
- invalid UUIDs;
- invalid/missing required parent for source type;
- `maxItems` outside `1..50`;
- empty title override;
- more than one Genres row per hub;
- unsupported row source or view preset.

### Plugin before response

- Re-run structural validation.
- Resolve effective rows.
- Filter access.
- Return no partial malformed document.

### Client before activation/cache

Reject the remote envelope when:

- `schemaVersion != 1`;
- required metadata is missing;
- row IDs are duplicated;
- hub key/library ID mismatch;
- UUID fields cannot decode;
- a row cannot decode into a supported `HomeRowConfig`;
- limits are invalid.

An invalid remote response must not delete or overwrite the last valid cache.

## 12. Client cache

Recommended path:

```text
filesDir/managed_layout/<serverId>/<userId>/layout-v1.json
```

Write process:

1. Serialize to a temporary file in the same directory.
2. Flush/close.
3. Rename over the old cache atomically where supported.
4. Update in-memory status only after the rename succeeds.

Cache identity includes server and user, never only username.

On logout/server removal, deleting the matching cache is optional. It contains no password, but should never be loaded for another server/user identity.

## 13. Cached-first refresh

State should distinguish:

```text
Unmanaged
ManagedCached
ManagedRemote
ManagedStale(error)
ManagedNoCache(error)
```

Load order:

```text
valid cache -> emit immediately
remote fetch -> validate
  changed -> cache + emit
  unchanged -> update refresh metadata only
  error -> keep cache and mark stale
no cache + no assignment -> unmanaged
no cache + fetch error -> existing unmanaged/default fallback + status
```

Use a 15-minute resume staleness threshold for version one. This is a constant, not a user setting.

## 14. Compatibility fixtures

Both repositories must carry the canonical JSON file in `architecture/examples/managed-layout-v1.json` or an equivalent test-resource path.

Required tests:

- Kotlin decodes the fixture and re-encodes semantically equivalent JSON.
- C# serializes a model that semantically matches the fixture.
- Unknown optional fields do not break version one.
- `schemaVersion = 2` is rejected by the version-one client.
- Invalid remote fixture does not replace valid cached fixture.

Compare parsed JSON trees rather than raw property ordering.

## 15. Explicit non-goals

- No smart-rule engine.
- No external-list fetching.
- No collection lifecycle management.
- No user-facing arbitrary JSON/GetItems editor.
- No custom endpoint row editor.
- No icon override.
- No schedule/seasonal activation.
- No real-time push.
- No plugin-managed playback settings.
