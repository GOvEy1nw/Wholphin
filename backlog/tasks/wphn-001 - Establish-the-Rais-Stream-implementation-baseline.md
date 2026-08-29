---
id: wphn-001
title: Establish the Rais Stream implementation baseline
status: Ready
assignee: []
created_date: "2026-08-29"
labels:
  - android
  - foundation
  - tests
dependencies: []
priority: high
---

## Description

Create the clean implementation starting point from the current `rais` branch and leave behind only the smallest durable contract fixture and baseline notes needed by later tasks. This task does not implement user-facing functionality.

## Goal

Prove that the exact baseline builds before feature work, fix the canonical branch/commit in the repository notes, and add the version-one managed-layout fixture that the later client and plugin tests will share.

## Files to inspect

- `DEVELOPMENT.md`
- `CONTRIBUTING.md`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- current unit-test conventions under `app/src/test`

## Implementation

1. Create the feature branch from commit `fdde98602921b8ecd7f52b3d18f32d3965ba8ab7` on `rais`.
2. Confirm there are no carried-over Sequence/experimental layout changes or unrelated worktree changes.
3. Build the unmodified default debug variant using the repository’s supported JDK/Android SDK.
4. Run the existing default debug unit tests once. Record any pre-existing failures rather than “fixing” them inside this task.
5. Add the canonical fixture from this package at:

   ```text
   app/src/test/resources/managed-layout/managed-layout-v1.json
   ```

6. Add a short `docs/rais-stream/README.md` containing:
   - baseline branch and commit;
   - links/names of the two plan tracks;
   - the one-task/one-commit rule;
   - deferred branding statement;
   - no secret/private values rule.
7. Do not add a managed-layout model or speculative test yet; `wphn-016` owns that runtime code.

## Acceptance criteria

- [ ] Branch is based on the specified `rais` commit.
- [ ] `:app:assembleDefaultDebug` succeeds, or a clearly documented pre-existing blocker is recorded.
- [ ] Existing test result is recorded without unrelated cleanup.
- [ ] Canonical fixture is present and byte-equivalent to the plan package fixture.
- [ ] Repository note states the baseline and boundaries.
- [ ] No production Kotlin, dependency, generated API, or UI behaviour changes are included.

## Verification

```powershell
.\gradlew.bat :app:assembleDefaultDebug
.\gradlew.bat :app:testDefaultDebugUnitTest
```

## Out of scope

- Runtime contract models.
- New build flavour.
- Navigation changes.
- Plugin work.
- Formatting or refactoring unrelated files.
