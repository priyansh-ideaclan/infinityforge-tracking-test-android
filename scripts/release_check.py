#!/usr/bin/env python3
"""Release-readiness gate: fails if a release build would ship with an unresolved
template placeholder or a capability turned on with no real credential behind it.

This is deliberately stricter than scripts/verify_project.py — a missing
google-services.json is fine for local development (PASS_WITH_EXTERNAL_SETUP) but must
hard-fail a release check.

Usage:
    python scripts/release_check.py [APP_SPEC.yaml]
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

from spec_lib import REPO_ROOT, load_spec, validate_spec


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def check_placeholders(spec: dict) -> list[str]:
    """Fail if `ads.enabled` would let a placement resolve to UNCONFIGURED_AD_UNIT_ID."""
    blockers: list[str] = []
    ads_admob_src = REPO_ROOT / "ads" / "ads-admob" / "src" / "main" / "kotlin"
    production_ids_file = ads_admob_src / "AdsModule.kt"
    if spec["ads"]["enabled"] and production_ids_file.exists():
        text = _read(production_ids_file)
        if "ProductionAdUnitIds()" in text:
            blockers.append(
                "ads.enabled is true but AppModule still provides an empty ProductionAdUnitIds() — "
                "every placement will resolve to UNCONFIGURED_AD_UNIT_ID in a release build. "
                "See Docs/setup/admob.md.",
            )

    return blockers


def check_required_keys(spec: dict) -> list[str]:
    blockers: list[str] = []
    build_gradle_text = _read(REPO_ROOT / "app" / "build.gradle.kts")

    def _gradle_property_default(property_name: str) -> str | None:
        match = re.search(
            rf'findProperty\("{property_name}"\)\s*\?\:\s*""',
            build_gradle_text,
        )
        return None if match else "missing"

    if spec["purchases"]["enabled"]:
        if _gradle_property_default("FACTORY_REVENUECAT_API_KEY") == "missing":
            blockers.append("FACTORY_REVENUECAT_API_KEY is no longer wired into app/build.gradle.kts")

    if spec["auth"]["enabled"] and spec["auth"]["providers"].get("google"):
        if _gradle_property_default("FACTORY_GOOGLE_WEB_CLIENT_ID") == "missing":
            blockers.append("FACTORY_GOOGLE_WEB_CLIENT_ID is no longer wired into app/build.gradle.kts")

    return blockers


def check_firebase_config(spec: dict) -> list[str]:
    blockers: list[str] = []
    needs_firebase = (
        spec["auth"]["enabled"] or spec["analytics"]["enabled"] or spec["crash_reporting"]["enabled"]
    )
    if needs_firebase and not (REPO_ROOT / "app" / "src" / "prod" / "google-services.json").exists():
        blockers.append(
            "auth/analytics/crash_reporting is enabled but app/src/prod/google-services.json "
            "is missing — a release build would ship with Firebase silently inert. "
            "See Docs/setup/firebase.md.",
        )
    return blockers


def check_signing(spec: dict) -> list[str]:
    del spec  # not spec-dependent; kept for a consistent check-function signature
    build_gradle_text = _read(REPO_ROOT / "app" / "build.gradle.kts")
    release_block_match = re.search(r"release\s*\{([^}]*)\}", build_gradle_text, re.DOTALL)
    if not release_block_match:
        return []

    # Strip `//` line comments before searching, so a comment that merely *mentions*
    # signingConfig (e.g. explaining that none is set) can't produce a false positive.
    release_block = "\n".join(
        line.split("//", 1)[0] for line in release_block_match.group(1).splitlines()
    )
    if re.search(r"signingConfig\s*=", release_block):
        return [
            "app/build.gradle.kts's release build type declares a signingConfig — this factory "
            "never generates one; confirm it points at a real, externally-managed keystore "
            "(see Docs/release/signing.md) and is not a debug/shared key.",
        ]
    return []


def main(argv: list[str]) -> int:
    spec_path = Path(argv[1]) if len(argv) > 1 else REPO_ROOT / "APP_SPEC.yaml"
    spec = load_spec(spec_path)

    schema_issues = validate_spec(spec)
    if schema_issues:
        print(f"BLOCKED: {spec_path} is invalid:")
        for issue in schema_issues:
            print(f"  - {issue}")
        return 1

    blockers = [
        *check_placeholders(spec),
        *check_required_keys(spec),
        *check_firebase_config(spec),
        *check_signing(spec),
    ]

    if blockers:
        print("FAIL: release is not ready:")
        for blocker in blockers:
            print(f"  - {blocker}")
        return 1

    print("PASS: no known release blockers found (this does not replace a real signed release build)")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
