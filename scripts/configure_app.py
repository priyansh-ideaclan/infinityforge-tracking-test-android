#!/usr/bin/env python3
"""Configure this factory checkout into a specific app, from APP_SPEC.yaml.

Idempotent: running it twice with an unchanged spec makes no further edits and reports
"unchanged" for every file. Never invents external credentials (Firebase/RevenueCat/
AdMob) — it only ever prints what to do about those, in the "external setup" section
at the end.

Usage:
    python scripts/configure_app.py [APP_SPEC.yaml]
"""
from __future__ import annotations

import re
import sys
from pathlib import Path
from typing import Any

from rename_package import rename_package
from spec_lib import REPO_ROOT, load_spec, validate_spec

APP_BUILD_GRADLE = REPO_ROOT / "app" / "build.gradle.kts"
STRINGS_XML = REPO_ROOT / "app" / "src" / "main" / "res" / "values" / "strings.xml"
COLOR_KT = REPO_ROOT / "core" / "core-designsystem" / "src" / "main" / "kotlin" / \
    "com" / "factory" / "core" / "designsystem" / "theme" / "Color.kt"


class ChangeSet:
    def __init__(self) -> None:
        self.changed: list[str] = []
        self.unchanged: list[str] = []
        self.missing_external: list[str] = []

    def record(self, label: str, was_changed: bool) -> None:
        (self.changed if was_changed else self.unchanged).append(label)


def _replace_once(
    text: str, pattern: str, replacement: str, label: str, changeset: ChangeSet,
) -> str:
    new_text, count = re.subn(pattern, replacement, text, count=1)
    if count == 0:
        raise ValueError(f"Pattern not found while configuring {label}: {pattern}")
    changeset.record(label, new_text != text)
    return new_text


def _find_app_module_kt() -> Path:
    matches = list((REPO_ROOT / "app" / "src" / "main" / "kotlin").rglob("AppModule.kt"))
    if len(matches) != 1:
        raise FileNotFoundError(
            f"Expected exactly one AppModule.kt under app/, found {len(matches)}",
        )
    return matches[0]


def configure_app_build_gradle(spec: dict[str, Any], changeset: ChangeSet) -> None:
    app = spec["app"]
    text = APP_BUILD_GRADLE.read_text(encoding="utf-8")

    text = _replace_once(
        text, r'versionCode\s*=\s*\d+', f'versionCode = {app["version_code"]}',
        "app/build.gradle.kts:versionCode", changeset,
    )
    text = _replace_once(
        text, r'versionName\s*=\s*"[^"]*"', f'versionName = "{app["version_name"]}"',
        "app/build.gradle.kts:versionName", changeset,
    )

    envs = spec["environments"]
    for flavor, field in (("dev", "dev"), ("staging", "staging"), ("prod", "prod")):
        base_url = envs[field]["base_url"]
        # Each flavor block has its own BASE_URL buildConfigField line; scope the regex
        # to the flavor's `create("<flavor>") { ... }` block so dev/staging/prod can't
        # cross-contaminate each other's BASE_URL. buildConfigField's "String" value is
        # itself a Kotlin string literal (i.e. the source contains an *escaped* quoted
        # string, `"\"https://...\""`), so the replacement matches everything between
        # the opening quote after `BASE_URL",` and the final `")` on that line, rather
        # than trying to parse the embedded escaped quotes character-by-character.
        block_pattern = re.compile(
            rf'(create\("{flavor}"\)\s*\{{[^}}]*?BASE_URL",\s*)"[^\n]*"\)',
            re.DOTALL,
        )
        escaped_value = f'"\\"{base_url}\\""'  # renders as: "\"<base_url>\""
        new_text, count = block_pattern.subn(lambda m: m.group(1) + escaped_value + ")", text, count=1)
        if count == 0:
            raise ValueError(f"Could not find BASE_URL field for flavor '{flavor}'")
        changeset.record(f"app/build.gradle.kts:{flavor}.BASE_URL", new_text != text)
        text = new_text

    APP_BUILD_GRADLE.write_text(text, encoding="utf-8")


def configure_app_spec_flags(spec: dict[str, Any], changeset: ChangeSet) -> None:
    flags_file = next((REPO_ROOT / "app" / "src" / "main" / "kotlin").rglob("AppSpecFlags.kt"))
    text = flags_file.read_text(encoding="utf-8")

    auth = spec["auth"]
    providers = auth.get("providers", {})
    values = {
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

    for name, value in values.items():
        kotlin_bool = "true" if value else "false"
        text, count = re.subn(
            rf'(const val {name}\s*=\s*)(true|false)',
            rf'\g<1>{kotlin_bool}',
            text,
            count=1,
        )
        if count == 0:
            raise ValueError(f"AppSpecFlags constant not found: {name}")

    original = flags_file.read_text(encoding="utf-8")
    changeset.record("AppSpecFlags.kt", text != original)
    flags_file.write_text(text, encoding="utf-8")


def configure_strings_xml(spec: dict[str, Any], changeset: ChangeSet) -> None:
    text = STRINGS_XML.read_text(encoding="utf-8")
    new_text = _replace_once(
        text,
        r'(<string name="app_name">)[^<]*(</string>)',
        rf'\g<1>{spec["app"]["name"]}\g<2>',
        "strings.xml:app_name",
        changeset,
    )
    STRINGS_XML.write_text(new_text, encoding="utf-8")


def configure_branding_colors(spec: dict[str, Any], changeset: ChangeSet) -> None:
    branding = spec["branding"]
    text = COLOR_KT.read_text(encoding="utf-8")
    original = text
    color_map = {
        "FactoryPrimary": branding.get("primary_color"),
        "FactorySecondary": branding.get("secondary_color"),
        "FactoryTertiary": branding.get("tertiary_color"),
    }
    for name, hex_color in color_map.items():
        if not hex_color:
            continue
        argb = "FF" + hex_color.lstrip("#").upper()
        text, count = re.subn(
            rf'(val {name} = Color\(0x)[0-9A-Fa-f]{{8}}(\))',
            rf'\g<1>{argb}\g<2>',
            text,
            count=1,
        )
        if count == 0:
            raise ValueError(f"Color constant not found: {name}")
    changeset.record("core-designsystem/Color.kt", text != original)
    COLOR_KT.write_text(text, encoding="utf-8")


def configure_app_module_constants(spec: dict[str, Any], changeset: ChangeSet) -> None:
    app_module = _find_app_module_kt()
    text = app_module.read_text(encoding="utf-8")
    original = text

    text, count = re.subn(
        r'(DatabaseName\(value = )"[^"]*"(\))',
        rf'\g<1>"{spec["room"]["database_name"]}"\g<2>',
        text,
        count=1,
    )
    if count == 0:
        raise ValueError("DatabaseName(...) not found in AppModule.kt")

    text, count = re.subn(
        r'(PremiumEntitlementId\(value = )"[^"]*"(\))',
        rf'\g<1>"{spec["purchases"]["premium_entitlement_id"]}"\g<2>',
        text,
        count=1,
    )
    if count == 0:
        raise ValueError("PremiumEntitlementId(...) not found in AppModule.kt")

    changeset.record("app/.../AppModule.kt constants", text != original)
    app_module.write_text(text, encoding="utf-8")


def external_setup_checklist(spec: dict[str, Any]) -> list[str]:
    checklist = []
    needs_firebase = spec["auth"]["enabled"] or spec["analytics"]["enabled"] or spec["crash_reporting"]["enabled"]
    if needs_firebase:
        checklist.append(
            "Firebase: create a project, register each environment's applicationId, "
            "download google-services.json into app/src/<flavor>/google-services.json "
            "(see Docs/setup/firebase.md).",
        )
    if spec["auth"]["enabled"] and spec["auth"]["providers"].get("google"):
        checklist.append(
            "Google Sign-In: add your debug + release SHA-1/SHA-256 fingerprints to the "
            "Firebase console and set FACTORY_GOOGLE_WEB_CLIENT_ID (Docs/setup/firebase.md).",
        )
    if spec["ads"]["enabled"]:
        checklist.append(
            "AdMob: create production ad units for each placement in APP_SPEC.yaml's "
            "ads.placements and wire them into ads-admob's ProductionAdUnitIds "
            "(Docs/setup/admob.md). Debug builds keep using Google's test IDs regardless.",
        )
    if spec["purchases"]["enabled"]:
        checklist.append(
            "RevenueCat: create a project, configure products/entitlements matching "
            f"'{spec['purchases']['premium_entitlement_id']}', and set "
            "FACTORY_REVENUECAT_API_KEY (Docs/setup/revenuecat.md).",
        )
    checklist.append(
        "Signing: this factory never generates a signing key — see Docs/release/signing.md "
        "before building a release you intend to publish.",
    )
    return checklist


def main(argv: list[str]) -> int:
    spec_path = Path(argv[1]) if len(argv) > 1 else REPO_ROOT / "APP_SPEC.yaml"
    spec = load_spec(spec_path)

    issues = validate_spec(spec)
    if issues:
        print(f"FAIL: {spec_path} is invalid, not configuring anything:")
        for issue in issues:
            print(f"  - {issue}")
        return 1

    changeset = ChangeSet()

    package_result = rename_package(spec["app"]["package_name"])
    changeset.record("app package name", package_result == "changed")

    configure_app_build_gradle(spec, changeset)
    configure_app_spec_flags(spec, changeset)
    configure_strings_xml(spec, changeset)
    configure_branding_colors(spec, changeset)
    configure_app_module_constants(spec, changeset)

    print(f"Configured from {spec_path}:")
    print(f"  changed:   {changeset.changed or '(none)'}")
    print(f"  unchanged: {changeset.unchanged or '(none)'}")

    print("\nExternal setup still required before this app is production-ready:")
    for item in external_setup_checklist(spec):
        print(f"  - {item}")

    print("\nNext: python scripts/verify_project.py, then ./scripts/verify.sh")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
