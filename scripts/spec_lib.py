"""Shared helpers for reading and validating APP_SPEC.yaml.

Every factory script that touches APP_SPEC.yaml imports this module instead of
re-implementing YAML loading/validation, so `validate_spec.py`'s rules are exactly the
rules every other script trusts. Python 3.12+, stdlib + PyYAML only (see
scripts/requirements.txt) — see AGENTS.md / CLAUDE.md for why nothing heavier is added.
"""
from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import yaml

REPO_ROOT = Path(__file__).resolve().parent.parent

REQUIRED_TOP_LEVEL_KEYS = (
    "factory",
    "app",
    "branding",
    "environments",
    "onboarding",
    "auth",
    "analytics",
    "crash_reporting",
    "ads",
    "purchases",
    "room",
    "networking",
    "notifications",
)

REQUIRED_ENVIRONMENTS = ("dev", "staging", "prod")

PACKAGE_NAME_PATTERN = re.compile(r"^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$")
HEX_COLOR_PATTERN = re.compile(r"^#[0-9A-Fa-f]{6}$")


@dataclass
class ValidationIssue:
    path: str
    message: str

    def __str__(self) -> str:  # pragma: no cover - trivial
        return f"{self.path}: {self.message}"


def load_spec(spec_path: Path) -> dict[str, Any]:
    with spec_path.open("r", encoding="utf-8") as f:
        data = yaml.safe_load(f)
    if not isinstance(data, dict):
        raise ValueError(f"{spec_path} did not parse to a YAML mapping")
    return data


def read_factory_version() -> str:
    return (REPO_ROOT / "factory-version.txt").read_text(encoding="utf-8").strip()


def validate_spec(spec: dict[str, Any]) -> list[ValidationIssue]:
    issues: list[ValidationIssue] = []

    for key in REQUIRED_TOP_LEVEL_KEYS:
        if key not in spec:
            issues.append(ValidationIssue(key, "missing required top-level key"))

    factory = spec.get("factory", {})
    if isinstance(factory, dict):
        source_version = factory.get("source_version")
        if not source_version:
            issues.append(ValidationIssue("factory.source_version", "must be set"))
        else:
            current = read_factory_version()
            if str(source_version) != current:
                issues.append(
                    ValidationIssue(
                        "factory.source_version",
                        f"spec says '{source_version}' but this factory checkout is "
                        f"'{current}' — this is a warning, not a hard failure, but "
                        "confirm the spec was written for this factory version",
                    ),
                )

    app = spec.get("app", {})
    if isinstance(app, dict):
        if not app.get("name"):
            issues.append(ValidationIssue("app.name", "must be a non-empty string"))
        package_name = app.get("package_name", "")
        if not PACKAGE_NAME_PATTERN.match(str(package_name)):
            issues.append(
                ValidationIssue(
                    "app.package_name",
                    f"'{package_name}' is not a valid reverse-DNS package name "
                    "(expected e.g. com.company.app)",
                ),
            )
        version_code = app.get("version_code")
        if not isinstance(version_code, int) or version_code < 1:
            issues.append(ValidationIssue("app.version_code", "must be a positive integer"))
        if not app.get("version_name"):
            issues.append(ValidationIssue("app.version_name", "must be a non-empty string"))

    branding = spec.get("branding", {})
    if isinstance(branding, dict):
        for color_key in ("primary_color", "secondary_color", "tertiary_color"):
            value = branding.get(color_key)
            if value is not None and not HEX_COLOR_PATTERN.match(str(value)):
                issues.append(
                    ValidationIssue(f"branding.{color_key}", f"'{value}' is not a #RRGGBB hex color"),
                )

    environments = spec.get("environments", {})
    if isinstance(environments, dict):
        for env_name in REQUIRED_ENVIRONMENTS:
            env = environments.get(env_name)
            if not isinstance(env, dict):
                issues.append(ValidationIssue(f"environments.{env_name}", "missing"))
                continue
            if not env.get("base_url", "").startswith(("http://", "https://")):
                issues.append(
                    ValidationIssue(f"environments.{env_name}.base_url", "must be an http(s) URL"),
                )
    else:
        issues.append(ValidationIssue("environments", "must be a mapping of dev/staging/prod"))

    auth = spec.get("auth", {})
    if isinstance(auth, dict) and auth.get("enabled"):
        providers = auth.get("providers", {})
        if isinstance(providers, dict) and not any(providers.values()):
            issues.append(
                ValidationIssue(
                    "auth.providers",
                    "auth.enabled is true but no provider (email_password/google/anonymous) is enabled",
                ),
            )

    ads = spec.get("ads", {})
    if isinstance(ads, dict) and ads.get("enabled") and not ads.get("placements"):
        issues.append(ValidationIssue("ads.placements", "ads.enabled is true but no placements are defined"))

    purchases = spec.get("purchases", {})
    if isinstance(purchases, dict) and purchases.get("enabled") and not purchases.get("premium_entitlement_id"):
        issues.append(
            ValidationIssue("purchases.premium_entitlement_id", "purchases.enabled is true but this is empty"),
        )

    notifications = spec.get("notifications", {})
    if isinstance(notifications, dict):
        extra_keys = set(notifications.keys()) - {"enabled"}
        if extra_keys:
            issues.append(
                ValidationIssue(
                    "notifications",
                    f"V1 only supports 'enabled: false' — unexpected keys: {sorted(extra_keys)}",
                ),
            )
        if notifications.get("enabled"):
            issues.append(
                ValidationIssue("notifications.enabled", "notifications are not implemented in factory v1; must be false"),
            )

    return issues
