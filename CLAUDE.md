# CLAUDE.md — Claude-specific workflow for this repository

Read `AGENTS.md` first — it has the binding rules (reading order, no invented
credentials, no disabled tests, canonical verification, module boundaries). This file
adds workflow notes specific to how Claude should operate in *this* repository; it does
not repeat those rules.

## How to start a session here

1. Run `git status` and `git log --oneline -20` to see real repository state — do not
   trust anything in your own context that isn't confirmed by the filesystem/git.
2. Read `Docs/sessions/CURRENT.md`. It names the active plan and the exact next action.
3. Open the active plan under `Docs/plans/`. Resume at the first row in its phase table
   that is not `DONE`/`VERIFIED`.
4. If `Docs/sessions/CURRENT.md` and the plan disagree, trust the plan's phase table and
   fix `CURRENT.md` to match before proceeding.

## How to work through a phase (matches the plan's phase table 1:1)

For each phase:
1. Inspect the relevant existing code/docs (do not assume from earlier phases — read them).
2. Set the phase to `IN_PROGRESS` in the plan.
3. Implement it for real — a phase is not complete when a directory/file exists but is
   empty or stub-only.
4. Add/update tests for the behavior the phase introduces.
5. Run the narrowest verification that actually exercises the change (e.g.
   `./gradlew :core:core-network:test`) before running the full `./scripts/verify.sh`.
6. Fix failures at the root cause. Do not comment out a failing test or suppress a lint
   rule to get past a red run.
7. Update `CHANGELOG.md`, the relevant `Docs/modules/*.md`, and the plan's status,
   "Files affected", and "Verification results" sections.
8. Update `Docs/sessions/CURRENT.md`'s "next exact action" before moving on.

## Large-task pacing

This factory build is large by design (15+ phases). Do not try to silently compress
multiple phases into one uncommitted blob of changes with no plan updates in between —
that is exactly what makes a session unresumable. Prefer: implement one phase → verify →
record → move on. It is fine, and expected, for a single session to only complete a
subset of phases; what matters is that the plan and `CURRENT.md` always reflect reality
so the *next* session (Claude or otherwise) can resume from the repository alone.

## When something can't be verified live

Firebase, RevenueCat, and AdMob production behavior cannot be verified without real
credentials, and this factory must never invent them (see `AGENTS.md` §7). When you hit
this:
- Say exactly what *was* verified (compiles, unit tests against fakes pass, lint/Detekt
  clean) and exactly what remains external (e.g. "requires a real `google-services.json`
  and a physical/emulator run to observe a live Crashlytics event").
- Write or update the matching `Docs/setup/*.md` file with the precise external steps.
- Never phrase this as "Firebase was tested" — phrase it as what it is:
  "compiles against the Firebase SDK; live behavior requires external setup."

## Tool usage notes specific to this repo

- Gradle/Android CLI commands need `JAVA_HOME` and `ANDROID_HOME` set for the shell
  invoking them (this machine has them in `~/.zshrc`, but a fresh non-interactive shell
  may not source it — export them inline in commands if a build fails with "no JDK"/"SDK
  not found" errors instead of assuming the profile was sourced).
- Prefer `./gradlew <module>:<task>` scoped to the module you touched while iterating;
  reserve full-project builds/`./scripts/verify.sh` for phase-completion checkpoints.
- Python scripts under `scripts/` target Python 3.12+ and must remain runnable with the
  standard library plus whatever is pinned in `scripts/requirements.txt` — do not add a
  heavy dependency for a small task.
