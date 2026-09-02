# Release readiness

## Debug builds

```bash
./gradlew :app:assembleDevDebug      # or Staging/Prod
```

Output: `app/build/outputs/apk/<flavor>/debug/app-<flavor>-debug.apk`. Debug builds are
always signed with the auto-generated debug keystore (`~/.android/debug.keystore`) and
always use Google's test AdMob IDs — see `Docs/setup/admob.md`.

## Release builds — APK and AAB

```bash
./gradlew :app:assembleProdRelease   # APK, R8-minified, resource-shrunk
./gradlew :app:bundleProdRelease     # AAB — this is what you upload to Play Console
```

Without a signing config, `assembleProdRelease` still succeeds and produces an
**unsigned** APK/AAB — useful for verifying the release build compiles and R8/shrinking
succeed (this factory's canonical verification does exactly this), but it cannot be
installed on a device or uploaded to Play Console until signed.

## Local signing

This factory **never generates or replaces a production signing key** (`AGENTS.md` §mandate
repeated deliberately). To sign locally:

1. Generate a keystore yourself, once, and store it somewhere safe **outside this repo**:
   ```bash
   keytool -genkeypair -v -keystore /secure/path/release.keystore \
     -alias your-app-alias -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Create `keystore.properties` at the repo root (already `.gitignore`d — never commit
   it):
   ```properties
   storeFile=/secure/path/release.keystore
   storePassword=...
   keyAlias=your-app-alias
   keyPassword=...
   ```
3. Add a `signingConfigs { release { ... } }` block to `app/build.gradle.kts` that reads
   `keystore.properties` (only if it exists — keep local dev/CI without it working):
   ```kotlin
   val keystorePropsFile = rootProject.file("keystore.properties")
   if (keystorePropsFile.exists()) {
       val props = java.util.Properties().apply { load(keystorePropsFile.inputStream()) }
       android.signingConfigs.create("release") {
           storeFile = file(props["storeFile"] as String)
           storePassword = props["storePassword"] as String
           keyAlias = props["keyAlias"] as String
           keyPassword = props["keyPassword"] as String
       }
       android.buildTypes.getByName("release").signingConfig = android.signingConfigs.getByName("release")
   }
   ```
   `scripts/release_check.py` will then flag this signingConfig for you to confirm it
   points at a real keystore, not a placeholder.

## CI signing

Store the keystore file base64-encoded and its passwords as CI secrets (GitHub Actions
"Repository secrets"), decode it in a release workflow step, and set the same Gradle
properties `keystore.properties` would have provided — do this only in a *release*
workflow, never the PR workflow (`.github/workflows/pr.yml` must never require these —
see its own file for why).

## Play App Signing

Recommended over pure local signing: enroll in
[Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756)
so Google holds your final signing key and you only need to protect an *upload* key.
Generate/upload the upload key the same way as above; Google re-signs with the app
signing key at distribution time.

## Play Console internal testing

1. Play Console → your app → Testing → Internal testing → Create a release, upload the
   signed AAB.
2. Add testers by email or Google Group.
3. Complete the required questionnaires below before the release can go out even to
   internal testers on some tracks (Play Console blocks submission without them):
   - **Data safety**: declare what data the app collects/shares — for this factory's
     default config that's at minimum Firebase Analytics usage data and, if `auth` is
     on, an account identifier. Update this honestly per what a specific app actually
     enables in `APP_SPEC.yaml`.
   - **Privacy policy**: required once any personal data is collected (Analytics/Auth
     both count) — host one and link it in Play Console's App content section.
   - **Permissions**: this factory declares `INTERNET`/`ACCESS_NETWORK_STATE` only;
     declare any additional permission a specific app adds and justify it in Play
     Console's Permissions declaration form if it's a sensitive one.
   - **Ads declaration**: if `ads.enabled: true`, declare "Yes, my app contains ads" in
     Play Console's App content section.
   - **IAP/subscriptions**: if `purchases.enabled: true`, declare in-app products in
     Play Console → Monetize → Products, matching what's configured in RevenueCat (see
     `Docs/setup/revenuecat.md`) — RevenueCat reads Play Billing directly, but the
     products must still exist in Play Console.

## What this factory cannot verify for you

Everything above that requires a real Play Console listing, a real signing key, or a
live device install is, by definition, outside what `./scripts/verify.sh` can check —
it verifies the build compiles, is R8-shrinkable, and is unsigned-installable-in-theory.
