---
id: wphn-001
title: Establish the Rais Stream implementation baseline
status: Done
assignee:
  - '@codex'
created_date: '2026-08-29'
updated_date: '2026-08-29 17:04'
labels:
  - android
  - foundation
  - tests
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
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
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Branch is based on the specified `rais` commit.
- [x] #2 `:app:assembleDefaultDebug` succeeds, or a clearly documented pre-existing blocker is recorded.
- [x] #3 Existing test result is recorded without unrelated cleanup.
- [x] #4 Canonical fixture is present and byte-equivalent to the plan package fixture.
- [x] #5 Repository note states the baseline and boundaries.
- [x] #6 No production Kotlin, dependency, generated API, or UI behaviour changes are included.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Use rais commit 0f7fbd46e003781d85aae9457428af68c5965c0b as the user-approved feature-branch baseline and confirm the isolated worktree is clean. 2. Build :app:assembleDefaultDebug and run :app:testDefaultDebugUnitTest once on the unmodified baseline, recording any pre-existing blocker without unrelated fixes. 3. Copy the canonical managed-layout fixture byte-for-byte and add the bounded Rais Stream repository note required by the task. 4. Verify fixture equivalence and the complete diff, record exact evidence, and finalize WPHN-001 for Human Review.

5. Per the user-approved scope change, remove the repository-wide `docs/` ignore rule and track the complete supplied `docs/rais-stream/plan/` package alongside the new task README.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Baseline verification on user-approved rais commit 0f7fbd46e003781d85aae9457428af68c5965c0b: `:app:assembleDefaultDebug` passed (`BUILD SUCCESSFUL`, 1m 56s). `:app:testDefaultDebugUnitTest` completed 344 tests with 4 pre-existing failures, all in `ServerRepositoryTest`: `Test changeUser via auth for existing`, `Test changeUser via auth`, `Test restoreUser via changeUser`, and `Test changeUser`; each failed from `FileStorage.kt:114` with `IOException`. Per task scope, no baseline test cleanup was attempted.

User approved making `docs/` tracked after confirming the Rais plan package was hidden by the repository-wide `docs/` ignore rule.

Final scope verification: all seven tracked `docs/rais-stream/plan/` files are byte-identical to the supplied local plan package; the staged app fixture is byte-identical with SHA-256 `dfc351f8b0617c5d06f2411b27456f4feba1993b9e1e42f35866dc87e58e2e44`; `git diff --cached --check` passed; the 11-path staged diff contains no production Kotlin, dependency, generated API, or UI changes. Targeted `pre-commit` could not run because the executable is not installed on PATH.

Human acceptance recorded on 2026-08-29.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Established the Rais Stream baseline from user-approved rais commit 0f7fbd46e003781d85aae9457428af68c5965c0b. The default debug build passes; the existing unit suite records 340/344 passing with four scoped pre-existing ServerRepositoryTest FileStorage I/O failures. Made docs trackable, added the complete byte-identical Rais plan package, added the bounded baseline README, and added the canonical managed-layout fixture. Verified staged scope, whitespace, and byte equivalence; pre-commit remains unavailable on PATH.
<!-- SECTION:FINAL_SUMMARY:END -->

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
