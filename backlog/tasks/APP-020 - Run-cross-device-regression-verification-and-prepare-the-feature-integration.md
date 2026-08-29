---
id: APP-020
title: Run cross-device regression verification and prepare the feature integration
status: To Do
assignee: []
created_date: '2026-08-29'
labels:
  - android
  - tests
  - focus
dependencies: 
  - APP-008
  - APP-010
  - APP-011
  - APP-012
  - APP-013
  - APP-015
  - APP-017
  - APP-019
priority: high
---

## Description

Validate the complete feature set on representative Android TV hardware, remove only defects/orphans introduced by the planned changes, and prepare a clean integration history. This task is not the deferred visual-branding pass.

## Goal

Prove navigation, managed layouts, onboarding, and restrictions work together on fast and lower-powered devices without adding brittle tests or unrelated polish.

## Inputs

- `architecture/interaction-spec.md`
- `architecture/verification-matrix.md`
- all completed APP task notes
- plugin build from `LAY-008`

## Implementation

1. Rebase/merge the completed task branches in dependency order. Resolve only real conflicts; do not squash unrelated fixes into this task.
2. Run Android compilation/tests for Default and Rais.
3. Run the complete manual verification matrix on:
   - one Nvidia Shield/equivalent fast device;
   - one Fire TV-class/lower-powered device;
   - additional Chromecast/TV-native devices where available.
4. Verify at both 1080p and 4K where the device matrix allows.
5. Specifically stress:
   - fast drawer traversal around 300 ms;
   - rapid season reversal around 200 ms;
   - Search preview keyboard suppression;
   - genre Back and library-switch reset;
   - Cast/Crew entry/exit;
   - cached-first layout while offline/slow;
   - user access filtering;
   - Quick Connect cancellation/restart;
   - standard versus Rais capability differences.
6. Fix only regressions attributable to this package. Log unrelated upstream defects separately.
7. Remove imports/variables/functions made unused by these changes. Do not perform a general cleanup/refactor.
8. Confirm no private property file, URL intended to remain private, session cookie, token, password, signing key, or generated local build artefact is staged.
9. Review all new constants and task notes. If timing is still acceptable, retain 300/200 ms; do not build a settings UI merely because tuning may happen later.
10. Write a concise integration summary:
    - commits/tasks included;
    - automated checks;
    - devices checked;
    - known limitations;
    - deferred branding work.
11. Follow Wholphin’s contribution/AI disclosure rules for any upstreamable PR, but keep the private Rais flavour/restrictions in the private fork.

## Acceptance criteria

- [ ] Default and Rais variants compile.
- [ ] Targeted unit tests pass.
- [ ] Required fast and lower-powered device checks pass.
- [ ] No focus trap, stale season overwrite, Search keyboard leak, or back-stack growth remains.
- [ ] Managed cache/access scenarios pass with the release plugin.
- [ ] Both Jellyfin auth methods and Seerr Quick Connect work.
- [ ] Standard variant retains disabled-in-Rais features.
- [ ] No secrets/private local files are staged.
- [ ] Integration summary records evidence and known limitations.
- [ ] No branding/design-system scope has slipped into this task.

## Verification

```powershell
.\gradlew.bat :app:testDefaultDebugUnitTest
.\gradlew.bat :app:testRaisDebugUnitTest
.\gradlew.bat :app:assembleDefaultDebug
.\gradlew.bat :app:assembleRaisDebug
pre-commit run --all-files
```

## Out of scope

- Final logo/banner/colour/spacing/typography pass.
- Supporting more Jellyfin server versions.
- New features discovered during testing.
