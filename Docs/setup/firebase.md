# Firebase setup

This factory never invents `google-services.json`, Firebase project IDs, or web client
IDs (see `AGENTS.md` §7). Without a real one, the app still compiles and passes tests —
`app/build.gradle.kts` skips applying the Google Services/Crashlytics Gradle plugins,
and `AnalyticsModule`/`AuthModule` fall back to `NoOpAnalyticsTracker`/`NoOpCrashReporter`
/`FakeAuthRepository` at runtime instead of crashing. This document is what turns that
into a real, live integration.

## 1. Create the Firebase project

1. Go to the [Firebase console](https://console.firebase.google.com) and create a
   project (or reuse an existing one for staging/prod separation — recommended: one
   Firebase project per `APP_SPEC.yaml` environment, or at minimum separate Android app
   registrations within one project).

## 2. Register the Android app per environment

For each of `dev`/`staging`/`prod` in `APP_SPEC.yaml`'s `environments`, register an
Android app in the Firebase console using the **exact** applicationId that
environment produces (see `app/build.gradle.kts`'s `applicationIdSuffix` per flavor —
e.g. `com.yourapp.dev`, `com.yourapp.staging`, `com.yourapp` for prod).

## 3. Download and place `google-services.json`

Download each registered app's `google-services.json` and place it at:

```
app/src/dev/google-services.json
app/src/staging/google-services.json
app/src/prod/google-services.json
```

These paths are already `.gitignore`d — never commit a real one. Once at least one is
present, re-run `./gradlew :app:assembleDebug`; the build log's
`[factory-setup-required]` warning about a missing config disappears once the plugin
actually applies.

## 4. Enable Authentication providers (if `auth.enabled: true`)

In the Firebase console → Authentication → Sign-in method, enable exactly the
providers `APP_SPEC.yaml`'s `auth.providers` turns on:

- **Email/Password** — enable directly, no further setup.
- **Anonymous** — enable directly.
- **Google** — enabling it in the console gives you a **Web client ID**. Set it via a
  Gradle property (never commit it): add to `~/.gradle/gradle.properties` (machine-local,
  not this repo) or pass `-PFACTORY_GOOGLE_WEB_CLIENT_ID=...`:
  ```properties
  FACTORY_GOOGLE_WEB_CLIENT_ID=1234567890-abc...apps.googleusercontent.com
  ```

## 5. Google Sign-In SHA fingerprints

Google Sign-In additionally requires your app's signing certificate fingerprints
registered in the Firebase console (Project settings → Your apps → the Android app →
Add fingerprint):

```bash
# Debug fingerprint (uses the auto-generated debug keystore):
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Release fingerprint (once you have a real release keystore — see Docs/release/signing.md):
keytool -list -v -keystore /path/to/release.keystore -alias <your-alias>
```

Add both the SHA-1 and SHA-256 values. Re-download `google-services.json` afterward —
it must be regenerated to include the fingerprints.

## 6. Analytics and Crashlytics

No separate setup beyond `google-services.json` being present and `APP_SPEC.yaml`'s
`analytics.enabled` / `crash_reporting.enabled` being `true` — `AnalyticsModule` picks
the real `FirebaseAnalyticsTracker`/`FirebaseCrashReporter` automatically once
`FirebaseApp.getApps(context)` is non-empty (see `core-analytics`'s `AnalyticsModule`).

## 7. Safe credential handling

- Never commit `google-services.json` (already `.gitignore`d).
- Never paste a real Google web client ID or API key into `APP_SPEC.yaml`,
  `AppSpecFlags.kt`, or any committed file — it only ever belongs in a Gradle property
  supplied externally (CI secret, local `~/.gradle/gradle.properties`).
- If a real credential is ever accidentally committed, treat it as compromised: rotate
  it in the Firebase console, then scrub it from git history.

## 8. Verification

- **Without real config**: `./scripts/verify.sh` reports steps 2/3 as
  `PASS_WITH_EXTERNAL_SETUP` — this is expected, not a failure.
- **With real config**: install a debug build on a device/emulator, sign in with each
  enabled provider, and confirm events show up in Firebase console → Analytics
  (DebugView) and a forced crash (e.g. `throw RuntimeException("test")` behind a debug
  button) shows up in Crashlytics within a few minutes.
- This factory cannot verify live Firebase behavior itself — it only verifies that the
  code compiles against the SDK and behaves safely (falls back to fakes) without one.
