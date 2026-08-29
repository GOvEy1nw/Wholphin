# Rais Stream / Wholphin Fork — Codex implementation package

This package turns the agreed product decisions into a sequenced implementation plan for two repositories:

- **Android TV app:** `GOvEy1nw/Wholphin`, based on branch `rais` at commit `fdde98602921b8ecd7f52b3d18f32d3965ba8ab7`.
- **Jellyfin plugin:** a new repository, recommended name `jellyfin-plugin-wholphin-layout`, targeting Jellyfin `12.0.0-rc6` only for the first release.

The plan intentionally reuses Wholphin’s existing navigation, `HomeRowConfig`, home-row renderer, filtering, Room/DataStore, Hilt, generated Seerr client, and Jellyfin SDK integration. It does **not** introduce a second row engine, smart-list rule engine, general design-system rewrite, real-time layout protocol, or multi-version Jellyfin compatibility layer.

## Package contents

- `MASTER-IMPLEMENTATION-PLAN.md` — architecture, sequencing, boundaries, risks, and rollout.
- `architecture/interaction-spec.md` — exact D-pad, OK, Back, dwell, spotlight, season, genre, Search, and Cast/Crew behaviour.
- `architecture/managed-layout-contract.md` — versioned app/plugin JSON contract, endpoint semantics, caching, and access filtering.
- `architecture/source-map.md` — current files to reuse or modify and expected new files.
- `architecture/verification-matrix.md` — lean automated checks plus the required Android TV manual checks.
- `architecture/examples/managed-layout-v1.json` — canonical contract fixture.
- `android-app/backlog/` — sequenced Backlog.md tasks for the Wholphin fork.
- `jellyfin-layout-plugin/backlog/` — sequenced Backlog.md tasks for the plugin.

## How Codex should use this package

1. Read this README, the master plan, the relevant architecture document, and **one** backlog task.
2. Work on one task per branch and one focused commit unless the task explicitly says otherwise.
3. Inspect every current caller before editing a shared function.
4. Reuse existing code paths; do not add parallel services or composables for behaviour already present.
5. Run only the smallest verification listed by the task, plus compilation for touched modules.
6. Do not refactor adjacent code, rename unrelated internals, reformat unrelated files, or perform the deferred branding pass.
7. Update the Backlog.md task status and implementation notes as work proceeds.
8. Stop at the task boundary. A later task may depend on the exact shape created by the current one.

## Recommended branch strategy

### Android app

Start from `rais` and create a feature integration branch, for example:

```text
integration/rais-stream-v1
```

Each `APP-*` task should use a short-lived child branch and merge back in task order.

### Jellyfin plugin

Create a new repository and integration branch, for example:

```text
integration/layout-plugin-v1
```

The plugin can proceed in parallel after `LAY-002` fixes the shared contract. `APP-016` must use exactly the canonical fixture produced by `LAY-002`.

## Build commands

### Android app — Windows

```powershell
.\gradlew.bat :app:assembleDefaultDebug
.\gradlew.bat :app:testDefaultDebugUnitTest
.\gradlew.bat :app:assembleRaisDebug
pre-commit run --all-files
```

Run only the commands relevant to the touched task. Native playback extensions are optional for compilation; the existing stub behaviour remains unchanged.

### Jellyfin plugin

```powershell
dotnet restore
dotnet build -c Release
dotnet test -c Release
```

The first plugin task must verify the exact package versions and runtime against the installed Jellyfin `12.0.0-rc6` server before later tasks build on them.

## Deliberately deferred

- Final logo, launcher artwork, Android TV banner, colours, spacing polish, typography, and visual-branding pass.
- Plugin-controlled library-icon overrides.
- Smart-list creation or rule evaluation.
- External-list integrations.
- Live push, WebSockets, polling, or server notifications for layouts.
- Support for multiple Jellyfin major/minor versions in one plugin build.
- Arbitrary Jellyfin `GetItemsRequest` or endpoint editors in the plugin UI.
