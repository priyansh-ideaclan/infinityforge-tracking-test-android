#!/usr/bin/env python3
"""Rename the `app` module's package (namespace/applicationId), including moving its
Kotlin source tree and rewriting `package`/self-referential `import` statements.

This intentionally only touches the `app` module. `core-*`/`feature-*`/`ads-*`/
`purchases-*` modules keep their internal `com.factory.*` namespace regardless of the
product's public package name — see ARCHITECTURE.md. Re-running with the same
`new_package` is a no-op (idempotent): it detects the current package from
`app/build.gradle.kts` and does nothing if it already matches.

Usage:
    python scripts/rename_package.py <new.package.name>
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

from spec_lib import REPO_ROOT

APP_BUILD_GRADLE = REPO_ROOT / "app" / "build.gradle.kts"
APP_KOTLIN_ROOT = REPO_ROOT / "app" / "src" / "main" / "kotlin"

NAMESPACE_PATTERN = re.compile(r'namespace\s*=\s*"([a-zA-Z0-9_.]+)"')
APPLICATION_ID_PATTERN = re.compile(r'applicationId\s*=\s*"([a-zA-Z0-9_.]+)"')


def current_package() -> str:
    text = APP_BUILD_GRADLE.read_text(encoding="utf-8")
    match = NAMESPACE_PATTERN.search(text)
    if not match:
        raise ValueError(f"Could not find `namespace = \"...\"` in {APP_BUILD_GRADLE}")
    return match.group(1)


def rename_package(new_package: str) -> str:
    """Returns 'unchanged' or 'changed'."""
    old_package = current_package()
    if old_package == new_package:
        return "unchanged"

    _rewrite_build_gradle(old_package, new_package)
    _move_and_rewrite_sources(old_package, new_package)
    return "changed"


def _rewrite_build_gradle(old_package: str, new_package: str) -> None:
    text = APP_BUILD_GRADLE.read_text(encoding="utf-8")
    text = NAMESPACE_PATTERN.sub(f'namespace = "{new_package}"', text, count=1)
    text = APPLICATION_ID_PATTERN.sub(f'applicationId = "{new_package}"', text, count=1)
    APP_BUILD_GRADLE.write_text(text, encoding="utf-8")


def _move_and_rewrite_sources(old_package: str, new_package: str) -> None:
    old_dir = APP_KOTLIN_ROOT / Path(*old_package.split("."))
    new_dir = APP_KOTLIN_ROOT / Path(*new_package.split("."))

    if not old_dir.exists():
        raise FileNotFoundError(f"Expected source directory not found: {old_dir}")

    new_dir.parent.mkdir(parents=True, exist_ok=True)
    old_dir.rename(new_dir)

    for kt_file in new_dir.rglob("*.kt"):
        text = kt_file.read_text(encoding="utf-8")
        text = re.sub(
            rf"\bpackage {re.escape(old_package)}\b",
            f"package {new_package}",
            text,
        )
        text = re.sub(
            rf"\bimport {re.escape(old_package)}\.",
            f"import {new_package}.",
            text,
        )
        kt_file.write_text(text, encoding="utf-8")

    # Clean up now-empty intermediate directories left behind by the old package path.
    old_top = APP_KOTLIN_ROOT / old_package.split(".")[0]
    for directory in sorted(old_top.rglob("*"), reverse=True):
        if directory.is_dir() and not any(directory.iterdir()):
            directory.rmdir()
    if old_top.exists() and not any(old_top.iterdir()):
        old_top.rmdir()


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        print("Usage: python scripts/rename_package.py <new.package.name>")
        return 2

    new_package = argv[1]
    result = rename_package(new_package)
    print(f"{result}: app package is now '{new_package}'")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
