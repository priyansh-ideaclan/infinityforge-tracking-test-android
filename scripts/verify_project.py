#!/usr/bin/env python3
"""Read-only check: does the repository actually match what APP_SPEC.yaml says it
should? Unlike configure_app.py, this never writes anything — it's what
scripts/verify.sh runs to catch "someone hand-edited AppSpecFlags.kt / build.gradle.kts
and forgot to update APP_SPEC.yaml (or vice versa)" drift.

Usage:
    python scripts/verify_project.py [APP_SPEC.yaml]

Exit codes: 0 = everything matches and no external config is missing. 1 = the spec is
invalid or the repository has actually drifted from it (a real bug). 2 = the repository
matches the spec, but some external configuration (google-services.json, an API key)
is missing — expected until real credentials are added; see scripts/verify.sh's
PASS_WITH_EXTERNAL_SETUP handling.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

from spec_lib import REPO_ROOT, load_spec, validate_spec

APP_KOTLIN_ROOT = REPO_ROOT / "app" / "src" / "main" / "kotlin"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def check_app_spec_flags(spec: dict) -> list[str]:
    flags_file = next(APP_KOTLIN_ROOT.rglob("AppSpecFlags.kt"))
    text = _read(flags_file)

    auth = spec["auth"]
    providers = auth.get("providers", {})
    expected = {
        "AUTH_ENABLED": auth["enabled"],
        "AUTH_EMAIL_PASSWORD": providers.get("email_password", False),
        "AUTH_GOOGLE": providers.get("google", False),
        "AUTH_ANONYMOUS": providers.get("anonymous", False),
        "ONBOARDING_ENABLED": spec["onboarding"]["enabled"],
        "ADS_ENABLED": spec["ads"]["enabled"],
        "PURCHASES_ENABLED": spec["purchases"]["enabled"],
        "ANALYTICS_ENABLED": spec["analytics"]["enabled"],
        "CRASH_REPORTING_ENABLED": spec["crash_reporting"]["enabled"],
    }

    conflicts = []
    for name, expected_value in expected.items():
        match = re.search(rf"const val {name}\s*=\s*(true|false)", text)
        if not match:
            conflicts.append(f"AppSpecFlags.kt: constant {name} not found")
            continue
        actual_value = match.group(1) == "true"
        if actual_value != expected_value:
            conflicts.append(
                f"AppSpecFlags.kt.{name} is {actual_value} but APP_SPEC.yaml says {expected_value} "
                "— run `python scripts/configure_app.py` to sync it",
            )
    return conflicts


def check_external_config(spec: dict) -> list[str]:
    missing: list[str] = []

    needs_firebase = (
        spec["auth"]["enabled"] or spec["analytics"]["enabled"] or spec["crash_reporting"]["enabled"]
    )
    if needs_firebase:
        for flavor in ("dev", "staging", "prod"):
            if not (REPO_ROOT / "app" / "src" / flavor / "google-services.json").exists():
                missing.append(
                    f"app/src/{flavor}/google-services.json is missing (required because "
                    "auth/analytics/crash_reporting is enabled) — see Docs/setup/firebase.md",
                )

    build_gradle_text = _read(REPO_ROOT / "app" / "build.gradle.kts")

    if spec["auth"]["enabled"] and spec["auth"]["providers"].get("google"):
        if 'findProperty("FACTORY_GOOGLE_WEB_CLIENT_ID")' not in build_gradle_text:
            missing.append(
                "app/build.gradle.kts no longer reads FACTORY_GOOGLE_WEB_CLIENT_ID — investigate",
            )

    if spec["purchases"]["enabled"]:
        if 'findProperty("FACTORY_REVENUECAT_API_KEY")' not in build_gradle_text:
            missing.append(
                "app/build.gradle.kts no longer reads FACTORY_REVENUECAT_API_KEY — investigate",
            )

    return missing


def main(argv: list[str]) -> int:
    spec_path = Path(argv[1]) if len(argv) > 1 else REPO_ROOT / "APP_SPEC.yaml"
    spec = load_spec(spec_path)

    schema_issues = validate_spec(spec)
    if schema_issues:
        print(f"FAIL: {spec_path} does not pass validate_spec.py:")
        for issue in schema_issues:
            print(f"  - {issue}")
        return 1

    conflicts = check_app_spec_flags(spec)
    missing_external = check_external_config(spec)

    if conflicts:
        print(f"FAIL: repository does not match {spec_path}:")
        for conflict in conflicts:
            print(f"  - {conflict}")
        return 1

    print(f"PASS: repository matches {spec_path}")

    if missing_external:
        print("\nExternal configuration still missing (expected until a real backend is set up):")
        for item in missing_external:
            print(f"  - {item}")
        return 2

    print("No missing external configuration detected.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
