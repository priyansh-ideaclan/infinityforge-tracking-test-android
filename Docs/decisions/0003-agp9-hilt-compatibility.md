# ADR-0003: AGP 9 built-in Kotlin support, and the Hilt/KSP versions it requires

- Status: Accepted
- Date: 2026-08-31

## Decision

- Do not apply `org.jetbrains.kotlin.android` in any module — AGP 9's built-in Kotlin
  support rejects the build outright if it's present ("no longer required... remove the
  plugin"). JVM target/toolchain is configured via the Kotlin extension directly (see
  `build-logic`'s `AndroidCommonConfig.kt`).
- Hilt must be **2.59.2**, not any 2.5x version. Hilt 2.58 and earlier fail to apply
  under AGP 9 with `Android BaseExtension not found` — Hilt's Gradle plugin only added
  AGP 9 support in 2.59; 2.59.0 itself had a `ComponentTreeDeps` runtime regression fixed
  in 2.59.2.
- KSP is **2.3.11** — KSP switched to Kotlin-version-independent numbering starting at
  2.3.0 (no longer `<kotlin-version>-<ksp-version>`); Maven Central's own metadata was
  the only reliable source for this, not web search.
- AGP 9's `com.android.build.api.dsl.*` interfaces (`CommonExtension`, `DefaultConfig`,
  `CompileOptions`, `Lint`, ...) dropped their `defaultConfig { }` / `compileOptions { }`
  / `lint { }` block-configuration methods — everything is now plain property access
  (`extension.defaultConfig.minSdk = 26`, not `extension.defaultConfig { minSdk = 26 }`).

## Context

All four of the above were discovered only by actually running Gradle against this
exact combination — not from prior knowledge, which predates AGP 9/Hilt 2.59. Anyone
touching `build-logic/convention` should expect the same class of surprises if bumping
AGP/Kotlin/Hilt/KSP further; re-verify with a real build rather than assuming API
continuity across major versions of any of these four.

## Consequences

Documented here (and in the plan's §5/§11) specifically so a future session doesn't
waste time rediscovering the same four issues from scratch.
