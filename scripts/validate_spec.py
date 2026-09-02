#!/usr/bin/env python3
"""Validate APP_SPEC.yaml against the factory's schema. Makes no changes.

Usage:
    python scripts/validate_spec.py [APP_SPEC.yaml]
"""
from __future__ import annotations

import sys
from pathlib import Path

from spec_lib import REPO_ROOT, load_spec, validate_spec


def main(argv: list[str]) -> int:
    spec_path = Path(argv[1]) if len(argv) > 1 else REPO_ROOT / "APP_SPEC.yaml"
    if not spec_path.exists():
        print(f"FAIL: {spec_path} does not exist")
        return 1

    try:
        spec = load_spec(spec_path)
    except Exception as exc:  # noqa: BLE001 - report any parse failure to the user
        print(f"FAIL: could not parse {spec_path}: {exc}")
        return 1

    issues = validate_spec(spec)
    if not issues:
        print(f"PASS: {spec_path} is valid")
        return 0

    print(f"FAIL: {spec_path} has {len(issues)} issue(s):")
    for issue in issues:
        print(f"  - {issue}")
    return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
