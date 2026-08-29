---
id: APP-012
title: Add flexible automatic library icon classification
status: To Do
assignee: []
created_date: '2026-08-29'
labels:
  - android
  - navigation
  - tests
dependencies: 
  - APP-001
priority: medium
---

## Description

Resolve a sensible navigation icon from a library’s normalized name before falling back to Jellyfin collection type. Version one remains fully automatic.

## Goal

Support the agreed family library vocabulary without manual configuration or brittle exact-string checks.

## Files to inspect

- `ui/nav/NavDrawer.kt`
- `ui/nav/ServerNavDrawerItem` definition
- `app/src/main/res/values/fa_strings.xml`
- current bundled Font Awesome font/version and available vector assets

## Implementation

1. Add a pure resolver accepting:

   ```kotlin
   libraryName: String
   collectionType: CollectionType
   ```

2. Normalize once:
   - lowercase with `Locale.ROOT`;
   - replace punctuation/separators with spaces;
   - collapse whitespace;
   - tokenize;
   - support simple singular/plural aliases through explicit token sets, not stemming libraries.
3. Apply rules in priority order so qualifiers win before generic matches:

   ```text
   4K TV / 4K Shows / 4K Series
   4K Movies / 4K Films
   3D Movies / 3D Films
   Reality / Reality TV / Reality Shows
   Documentaries / Docs
   Kids / Children / Childrens / Family
   Concert / Concerts
   Home Video / Home Videos
   TV / TV Shows / Shows / Series / Television
   Movie / Movies / Film / Films / Cinema
   ```

4. Initial icon families may reuse existing bundled glyphs to avoid a new icon dependency:
   - Movies, 3D Movies, 4K Movies -> film
   - TV, 4K TV, Reality -> TV
   - Docs -> file-video/document-style glyph already supported by the font
   - Kids/Family -> existing image/family-suitable bundled glyph
   - Concerts -> music
   - Home Videos -> video
5. Name matching may refine a broad Jellyfin type, for example a Movies collection called `Documentaries` gets the Docs icon.
6. When no name rule matches, use the current `CollectionType` mapping. Unknown remains the current safe generic fallback.
7. Keep rendering inside the existing NavItem path. Do not introduce an icon registry, plugin lookup, database field, or new dependency.
8. Add table-driven tests covering all requested names, punctuation/case/plural variants, ambiguous names, and type fallback.
9. Document future plugin override as out of scope; do not reserve wire fields in the version-one managed contract.

## Acceptance criteria

- [ ] Every requested example classifies correctly.
- [ ] Matching is case/punctuation/plural tolerant.
- [ ] `4K TV` cannot be misclassified as generic Movies and `3D Movies` cannot fall through to unknown.
- [ ] Unmatched libraries retain current type-based behaviour.
- [ ] Resolver is pure and tested.
- [ ] No icon pack/dependency or manual override setting is added.

## Verification

```powershell
.\gradlew.bat :app:testDefaultDebugUnitTest
.\gradlew.bat :app:assembleDefaultDebug
```

## Out of scope

- Distinct badge overlays for 3D/4K.
- Plugin icon overrides.
- Final custom icon artwork.
