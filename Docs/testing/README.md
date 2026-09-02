# Testing

See `Docs/testing/module-matrix.md` for the optional-capability combination testing
strategy. This file covers how to run each kind of test.

## Unit tests (JVM, no device needed)

```bash
./gradlew testDebugUnitTest          # every module
./gradlew :feature:feature-home:testDebugUnitTest   # one module
```

`core-database`'s `NoteDaoTest` and `feature-auth`'s `AuthModuleTest` run Room/Firebase
lookups against Robolectric (a JVM Android runtime) — no emulator needed for these
either, despite touching Android APIs.

## Python factory-script tests

```bash
pip install -r scripts/requirements.txt
python -m unittest discover -s scripts/tests -v
```

## Compose UI tests (instrumented — need a device or emulator)

`feature-auth`'s `LoginScreenTest` (in `src/androidTest`) is the factory's representative
Compose UI test. It compiles as part of `./gradlew compileDebugAndroidTestKotlin` (which
canonical verification does check), but actually **running** it requires a connected
device or running emulator:

```bash
./gradlew :feature:feature-auth:connectedDebugAndroidTest
```

If you don't have an emulator set up yet: Android Studio → Device Manager → Create
Device → pick any phone profile with a recent system image → launch it, then re-run the
command above (or press the test's gutter icon in Android Studio).

## Canonical verification

`./scripts/verify.sh` runs unit tests (Kotlin + Python) but does **not** run instrumented
tests — no step in canonical verification requires a device, by design (see
`AGENTS.md`/the plan's assumption A4.3). Run `connectedDebugAndroidTest` separately when
a device is available.
