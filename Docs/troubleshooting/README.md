# Troubleshooting

Real issues hit while building this factory, and their actual fixes — not
hypotheticals. See `Docs/decisions/` for the reasoning behind the version choices these
reference.

## "no JDK"/"SDK not found" running `./gradlew` in a fresh shell

`JAVA_HOME`/`ANDROID_HOME` are exported in the interactive shell's profile
(`~/.zshrc`), but a fresh non-interactive shell (a CI runner, a script) won't have them.
Export inline:
```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
```

## `Plugin [id: 'com.google.devtools.ksp', ...]` not found

KSP's versioning changed at 2.3.0 — it's no longer `<kotlin-version>-<ksp-version>`
(e.g. `2.3.20-2.1.0`, which doesn't exist), just a plain KSP version (e.g. `2.3.11`).
Check `https://repo.maven.apache.org/maven2/com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin/maven-metadata.xml`
directly rather than trusting older conventions.

## `The 'org.jetbrains.kotlin.android' plugin is not compatible`

AGP 9+ has built-in Kotlin support; applying the separate Kotlin Android Gradle plugin
alongside it is now rejected. Remove `id("org.jetbrains.kotlin.android")` from every
module — see ADR-0003.

## `Android BaseExtension not found` applying `com.google.dagger.hilt.android`

Hilt's Gradle plugin didn't support AGP 9 until version 2.59. Use Hilt 2.59.2 (2.59.0
had its own `ComponentTreeDeps` regression). See ADR-0003.

## `Unresolved reference: defaultConfig`/`compileOptions`/`lint` in a convention plugin

AGP 9's `com.android.build.api.dsl.*` interfaces removed the `defaultConfig { }` /
`compileOptions { }` / `lint { }` block-configuration methods some older code relies on.
Use direct property access instead: `extension.defaultConfig.minSdk = 26`.

## Compose BOM requires `compileSdk 37`, but only `android-35`/`36` is installed

```bash
sdkmanager "platforms;android-37.1" "build-tools;37.0.0"
```
Then set `compileSdk = 37` (keep `targetSdk` at whatever the actual Play Store
requirement is — they're independent). See ADR-0001.

## A resource-only rename (e.g. `mipmap-anydpi-v26` → `mipmap-anydpi`) isn't picked up

If a Gradle daemon has a stale configuration-cache entry from before the rename, a
resource-merge task can report the old resource as "not found" even though the new file
exists on disk. Fix: `./gradlew --stop`, delete `app/build/intermediates/merged_res` and
`.gradle/configuration-cache`, then rebuild.

## Kotlin "Unclosed comment" from a KDoc containing `` `feature/*` ``

Kotlin block comments (`/* */`, including KDoc's `/** */`) nest. A literal `/*`
substring anywhere inside — even inside backticks, e.g. documenting a glob pattern like
`feature/*` — opens a nested comment that needs its own closing `*/`, and everything
after silently becomes part of the comment until end of file. Rephrase to avoid a
literal `/*` inside any comment (e.g. "feature modules under `feature/`" instead of
`` `feature/*` ``).

## `google-services.json`/RevenueCat/AdMob credentials

Not a bug — this factory deliberately never has them. See `Docs/setup/firebase.md`,
`Docs/setup/revenuecat.md`, `Docs/setup/admob.md` for how to add real ones, and
`AGENTS.md` §7 for why they're never invented.
