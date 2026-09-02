# AGENTS.md — Rules for any AI agent working in this repository

This repository **is** the Native Android App Factory. It is both a template and a
factory: cloning it and running `python scripts/configure_app.py APP_SPEC.yaml` should
turn it into a specific, shippable Android app.

Any agent (Claude, another LLM, or a human following the same discipline) working here
must follow these rules. `CLAUDE.md` adds Claude-specific workflow notes on top of this
file — read both.

## 1. Required reading order

Before making any change of substance, read, in this order, whichever of these exist:

1. `AGENTS.md` (this file)
2. `CLAUDE.md`
3. `Docs/references/factory-requirements.md` (if present — original requirements source)
4. `Docs/sessions/CURRENT.md` (what is true right now, what to do next)
5. The active plan under `Docs/plans/` referenced by `CURRENT.md`
6. `ARCHITECTURE.md` and the relevant `Docs/modules/*.md` for the area you are touching

Do not start writing code from memory of a previous session. State lives in the
repository, not in conversation history.

## 2. Inspect-before-editing rule

Before editing or creating a file:
- Read the file if it exists. Never overwrite a file you have not read.
- Search for existing implementations of the same capability before adding a new one
  (`grep`/`find` across `core/`, `feature/`, `ads/`, `purchases/`, `scripts/`).
- Prefer extending an existing module boundary over creating a parallel one.

## 3. Persistent-plan rule

All non-trivial work is tracked in `Docs/plans/YYYY-MM-DD-<topic>.md` using the status
vocabulary: `NOT_STARTED`, `IN_PROGRESS`, `BLOCKED`, `IMPLEMENTED`, `VERIFIED`, `DONE`.

- Update the plan *as you work*, not only at the end.
- `Docs/sessions/CURRENT.md` always points at the active plan and states the exact next
  action. Update it before ending a session, and whenever the next action changes.
- Never treat the chat transcript as durable state. If it is not written to a file in
  this repository, it does not exist for the next session.

## 4. Session-ending handoff rule

A session may end mid-work if the scope does not fit in one sitting. Before stopping:

- Update the active plan's status table and "Verification results" section honestly.
- Update `Docs/sessions/CURRENT.md` with: what is done, what remains, the exact next
  command/action, and any blockers.
- Update `CHANGELOG.md` with what actually landed (not what was attempted).
- Never leave the repository in a state where `main`/the working tree fails to build
  for a reason that isn't documented as a known/blocked issue.

## 5. Definition of done

A phase or task is done only when **all** of the following are true:
- The implementation is real (no placeholder-only stubs presented as complete).
- Tests exist and pass for the behavior added or changed.
- `./scripts/verify.sh` (or the specific focused checks relevant to the change) has
  actually been executed, and its real output is what is reported — not an assumption.
- Documentation and `CHANGELOG.md` reflect the change.
- The persistent plan reflects the change with an accurate status.

## 6. No hidden failures

- Do not swallow build, test, lint, or script errors. Surface them, fix the root cause,
  and only then continue.
- If something cannot be fixed in-session, mark it `BLOCKED` in the plan with a precise
  reason — do not mark it done, and do not silently skip it.
- Never claim a check "passed" if it was not executed. If a check could not run
  (missing tool, missing credential, no device), say so explicitly and use
  `PASS_WITH_EXTERNAL_SETUP`, `FAIL`, or `BLOCKED` as appropriate — never `PASS`.

## 7. No invented credentials, no committed secrets

- Never generate, guess, or fabricate `google-services.json`, RevenueCat API keys,
  AdMob production ad unit IDs, signing keystores/passwords, or any other production
  credential.
- Debug/test builds and CI use official *test* identifiers only (Google test ad unit
  IDs, RevenueCat/Firebase local or fake implementations).
- Never commit real secrets, keystores, or `.env` files with real values. `.gitignore`
  in this repository already excludes the known secret-bearing paths — do not remove
  those entries. If a real secret is ever pasted into a file in this repo, stop and
  flag it instead of committing it.

## 8. No disabling tests or lint to obtain a pass

- Do not delete, comment out, `@Ignore`, or weaken a test to make a build green.
- Do not add blanket lint/Detekt suppressions to silence a real finding. A narrow,
  justified suppression with a comment explaining *why* is acceptable; a global
  disable of a rule to avoid fixing violations is not.
- If a test is genuinely wrong (testing removed behavior), fix or remove it with an
  explanation in the commit/plan — not silently.

## 9. No unrelated refactoring

- Keep changes scoped to the phase/task at hand. Do not rename, reformat, or restructure
  unrelated code while implementing something else.
- If you notice unrelated debt, note it in `Docs/plans/` or a `Docs/decisions/` entry
  instead of fixing it inline.

## 10. Canonical verification requirement

`./scripts/verify.sh` is the single canonical entry point for "does this factory work."
Before declaring any milestone complete, run it (or explicitly record why a specific
step could not run, e.g. no device attached) and report its real final health status:
`PASS`, `PASS_WITH_EXTERNAL_SETUP`, `FAIL`, or `BLOCKED`.

## 11. Module boundaries are not optional

- Feature code (`feature/*`) must depend on `ads-api`, `purchases-api`, `core-analytics`,
  and `AuthRepository` — never directly on the Google Mobile Ads SDK, RevenueCat SDK, or
  Firebase SDK. Those live only in `ads-admob`, `purchases-revenuecat`, and the
  `*FirebaseRepository` implementations.
- Do not create `utils`, `helpers`, or `misc` packages/modules. If something doesn't fit
  an existing feature-oriented or capability-oriented module, that is a signal to name
  the real concept, not to reach for a dumping ground.

## 12. Product configuration vs. engineering standards

`APP_SPEC.yaml` is the *product* configuration (name, package, branding, which optional
capabilities are on). `MODULES.yaml` is the *capability catalog* the factory understands.
Neither file is where engineering standards (lint rules, CI steps, versions) live — those
belong in `build-logic/`, `gradle/libs.versions.toml`, and `.github/workflows/`. Do not
blur this line; a new app should only ever need to edit `APP_SPEC.yaml`.
