# Current Session State

- Active plan: `Docs/plans/2026-08-31-native-android-factory-v1.md`
- Status: **V1 factory build complete.** All 18 phases (0–17) are DONE/VERIFIED.
- Canonical verification: `./scripts/verify.sh` → `PASS_WITH_EXTERNAL_SETUP`, confirmed
  twice on 2026-08-31 (initial run, then re-confirmed in a completion audit after a
  session interruption — identical result both times, 23/23 unit tests, no incomplete
  files found). Re-run it yourself before trusting this if much time has passed.

## What is true right now

The entire factory — Gradle foundation, all core/ads/purchases/feature modules, the
`app` shell, Python automation, canonical verification, tests, docs, and CI — is
implemented and verified for real (see `CHANGELOG.md` for the full list, and the plan's
§10 for the exact verification transcript). A debug APK and an unsigned, R8-minified
release APK both build successfully:
- `app/build/outputs/apk/prod/debug/app-prod-debug.apk`
- `app/build/outputs/apk/prod/release/app-prod-release-unsigned.apk`

No git commit has been made this session (not requested by the user) — the working
tree has the full implementation, untracked.

## What remains (external or explicitly deferred — not incomplete factory work)

See the plan's §13 for the full list. In short: real Firebase/RevenueCat/AdMob
credentials (external, `Docs/setup/`), running the one Compose UI test on a device,
mocking RevenueCat's static singleton to test its "enabled" path, wiring a few
"reserved" analytics events to real UI once one exists, and a full four-way
`configure_app.py` combination matrix.

## Resume instructions for the next session

1. Read `AGENTS.md`, then `CLAUDE.md`, then this file, then the plan.
2. If asked to add a new capability or fix something: it's almost certainly a change
   *within* one of the existing modules (see `Docs/modules/README.md`) — the module
   boundaries and Hilt wiring pattern are established; follow the existing pattern
   (interface + real impl + `Fake*` impl + a Hilt `@Provides` selecting between them)
   rather than inventing a new one.
3. If asked to configure this factory into a real app: follow `README.md`'s Quick
   start / `ARCHITECTURE.md`'s "Cloning this factory" section.
4. Before claiming anything passes, actually run it — this session found several real
   bugs (see `CHANGELOG.md`'s "Fixed" section and `Docs/troubleshooting/README.md`)
   that looked correct on inspection but weren't; that pattern held for essentially
   every non-trivial claim, not just an unlucky few.

## Next exact action

None required for V1. If continuing: pick one item from "What remains" above, or ask
the user what to build next now that the factory itself is done.
