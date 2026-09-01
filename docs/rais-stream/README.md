# Rais Stream implementation

The implementation baseline is the `rais` branch at commit
`0f7fbd46e003781d85aae9457428af68c5965c0b`.

The two implementation tracks are:

- [Android app tasks](../../backlog/tasks/) in this Wholphin repository.
- Jellyfin layout plugin tasks described by the
  [master implementation plan](plan/MASTER-IMPLEMENTATION-PLAN.md).

Implement one task per feature branch and one focused commit per task. Stop at
the selected task boundary.

Visual branding is deliberately deferred until the functional implementation
is complete. Do not commit secrets, credentials, server addresses, user IDs, or
other private deployment values.

## Private Rais build

Copy `family.properties.example` to the repository root as `family.properties`
and set a distinct Android application ID plus the three required HTTPS URLs.
The file is ignored by Git and must contain deployment endpoints only, never
credentials, tokens, signing keys, or passwords.

Build the private debug variant on Windows with:

```powershell
.\gradlew.bat :app:assembleRaisDebug
```

Standard `default`, `appstore`, and `firetv` variants do not require this file.
