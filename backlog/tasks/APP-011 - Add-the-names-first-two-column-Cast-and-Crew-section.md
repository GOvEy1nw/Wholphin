---
id: APP-011
title: Add the names-first two-column Cast and Crew section
status: To Do
assignee: []
created_date: '2026-08-29'
labels:
  - android
  - people
  - focus
  - settings
  - tests
dependencies: 
  - APP-001
priority: high
---

## Description

Replace the default portrait-card person rows with one text-first Cast/Crew section that costs only one D-pad step during normal page traversal. Preserve the current image row behind an opt-in preference.

## Goal

Apply the agreed presentation consistently anywhere Wholphin currently renders people, without duplicating person queries or person-detail navigation.

## Files to inspect

- `data/model/Person.kt`
- `ui/cards/PersonRow.kt`
- `ui/cards/PersonCard.kt`
- every `PersonRow(` call site under Movie, Series, Episode, Discover, and other detail pages
- `preferences/AppPreference.kt`
- `preferences/AppPreferencesSerializer.kt`
- `app/src/main/proto/WholphinDataStore.proto`
- preference upgrade conventions

## Implementation

1. Add a positive preference:

   ```text
   Show people images
   default: false
   ```

   Proto3’s false default matches the desired default. Add serializer/upgrade work only where the project’s current preference rules require it.
2. Keep the existing `PersonRow`/`PersonCard` implementation as the image-enabled branch. Do not rewrite it.
3. Add one text `PeopleSection` that partitions the current list while preserving server order:
   - Cast: `ACTOR`, `GUEST_STAR`, and equivalent performer kinds returned by the current SDK.
   - Crew: directors, writers, producers, composers, creators, and every other non-cast person kind.
4. Render two side-by-side vertical columns:
   - name as primary text;
   - `role` when supplied, otherwise a readable person-kind/job label;
   - Cast left, Crew right;
   - all returned entries, no cap or View All.
5. Outer mode:
   - only the whole section container can focus;
   - children have `canFocus = false`;
   - normal Up/Down crosses the section in one step;
   - show one clear focused container/heading state.
6. OK enters internal mode:
   - enable child focus;
   - focus first Cast entry, otherwise first Crew entry.
7. Internal navigation:
   - Up/Down within current column;
   - Left/Right to `min(currentIndex, otherColumn.lastIndex)`;
   - OK uses existing person navigation callback;
   - Back exits internal mode and returns container focus;
   - leaving/disposal clears internal mode.
8. Avoid nested scroll containers. Let the containing detail page scroll the whole rendered section.
9. Replace current person-row call sites through one common composable choice so the preference applies everywhere. Do not copy partition/focus state into each page.
10. For pages that currently separate Guest Stars, combine them into Cast for the text section. If images are enabled, preserve the current separate image rows unless doing so creates a broken caller contract.
11. Add a pure test for classification only if the mapping has more than trivial enum branches. Focus geometry remains manual.

## Acceptance criteria

- [ ] New installs default to no person images/icons.
- [ ] Text section has Cast left and Crew right with secondary role/job text.
- [ ] All returned people are shown.
- [ ] The section costs one D-pad step in outer mode.
- [ ] OK enters child focus; Back returns to section focus.
- [ ] Cross-column navigation targets the nearest valid index.
- [ ] Person OK still opens the existing person page.
- [ ] Enabling Show people images restores existing image-card behaviour.
- [ ] All current PersonRow call sites use the shared preference-aware path.

## Verification

```powershell
.\gradlew.bat :app:testDefaultDebugUnitTest
.\gradlew.bat :app:assembleDefaultDebug
```

Manually test sparse columns, Crew-only, Cast-only, long names, and all Cast/Crew focus scenarios.

## Out of scope

- New person metadata queries.
- View All page.
- Image redesign.
