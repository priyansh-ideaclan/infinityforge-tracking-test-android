"""Tests for the factory automation scripts.

These run each script as a real subprocess against a throwaway copy of the repository
pieces it touches (never the actual working tree) — the same way a human would invoke
`python scripts/configure_app.py`, and the only way to genuinely test idempotency and
file-tree renaming without risking the real repository.

Run with: python -m unittest discover -s scripts/tests -v
(requires PyYAML — see scripts/requirements.txt)
"""
from __future__ import annotations

import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
SCRIPTS_DIR = REPO_ROOT / "scripts"


def run_script(cwd: Path, script_name: str, *args: str) -> subprocess.CompletedProcess:
    return subprocess.run(
        [sys.executable, str(cwd / "scripts" / script_name), *args],
        cwd=cwd,
        capture_output=True,
        text=True,
        check=False,
    )


def make_sandbox() -> Path:
    """Copies just the files the scripts touch into a temp directory."""
    sandbox = Path(tempfile.mkdtemp(prefix="factory-scripts-test-"))
    shutil.copytree(REPO_ROOT / "app", sandbox / "app")
    shutil.copytree(REPO_ROOT / "scripts", sandbox / "scripts")
    shutil.copy(REPO_ROOT / "APP_SPEC.yaml", sandbox / "APP_SPEC.yaml")
    shutil.copy(REPO_ROOT / "MODULES.yaml", sandbox / "MODULES.yaml")
    shutil.copy(REPO_ROOT / "factory-version.txt", sandbox / "factory-version.txt")
    shutil.copy(REPO_ROOT / "settings.gradle.kts", sandbox / "settings.gradle.kts")
    color_kt = (
        "core/core-designsystem/src/main/kotlin/com/factory/core/designsystem/theme/Color.kt"
    )
    (sandbox / Path(color_kt).parent).mkdir(parents=True, exist_ok=True)
    shutil.copy(REPO_ROOT / color_kt, sandbox / color_kt)
    return sandbox


class ValidateSpecTest(unittest.TestCase):
    def test_valid_spec_passes(self) -> None:
        result = run_script(REPO_ROOT, "validate_spec.py", "APP_SPEC.yaml")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("PASS", result.stdout)

    def test_missing_file_fails(self) -> None:
        result = run_script(REPO_ROOT, "validate_spec.py", "does_not_exist.yaml")
        self.assertEqual(result.returncode, 1)
        self.assertIn("FAIL", result.stdout)

    def test_invalid_package_name_fails(self) -> None:
        sandbox = make_sandbox()
        try:
            spec_path = sandbox / "APP_SPEC.yaml"
            text = spec_path.read_text(encoding="utf-8")
            text = text.replace(
                'package_name: "com.ideaclan.infinityforgetrackingtestkotlin"',
                'package_name: "NotValid"',
            )
            spec_path.write_text(text, encoding="utf-8")

            result = run_script(sandbox, "validate_spec.py", "APP_SPEC.yaml")

            self.assertEqual(result.returncode, 1)
            self.assertIn("package_name", result.stdout)
        finally:
            shutil.rmtree(sandbox, ignore_errors=True)


class ConfigureAppIdempotencyTest(unittest.TestCase):
    def test_running_twice_is_idempotent(self) -> None:
        sandbox = make_sandbox()
        try:
            first = run_script(sandbox, "configure_app.py", "APP_SPEC.yaml")
            self.assertEqual(first.returncode, 0, first.stdout + first.stderr)

            second = run_script(sandbox, "configure_app.py", "APP_SPEC.yaml")

            self.assertEqual(second.returncode, 0, second.stdout + second.stderr)
            self.assertIn("changed:   (none)", second.stdout)
        finally:
            shutil.rmtree(sandbox, ignore_errors=True)

    def test_changing_base_url_is_applied_and_scoped_to_one_flavor(self) -> None:
        sandbox = make_sandbox()
        try:
            spec_path = sandbox / "APP_SPEC.yaml"
            text = spec_path.read_text(encoding="utf-8")
            text = text.replace(
                'base_url: "https://dev.api.example.com/"',
                'base_url: "https://changed.example.com/"',
            )
            spec_path.write_text(text, encoding="utf-8")

            result = run_script(sandbox, "configure_app.py", "APP_SPEC.yaml")

            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            build_gradle = (sandbox / "app" / "build.gradle.kts").read_text(encoding="utf-8")
            self.assertIn("https://changed.example.com/", build_gradle)
            self.assertIn("https://staging.api.example.com/", build_gradle)
            self.assertIn("https://api.example.com/", build_gradle)
        finally:
            shutil.rmtree(sandbox, ignore_errors=True)

    def test_invalid_spec_is_rejected_without_writing_anything(self) -> None:
        sandbox = make_sandbox()
        try:
            spec_path = sandbox / "APP_SPEC.yaml"
            text = spec_path.read_text(encoding="utf-8").replace(
                'name: "InfinityForge Tracking Test"', 'name: ""',
            )
            spec_path.write_text(text, encoding="utf-8")
            before = (sandbox / "app" / "build.gradle.kts").read_text(encoding="utf-8")

            result = run_script(sandbox, "configure_app.py", "APP_SPEC.yaml")

            self.assertEqual(result.returncode, 1)
            after = (sandbox / "app" / "build.gradle.kts").read_text(encoding="utf-8")
            self.assertEqual(before, after)
        finally:
            shutil.rmtree(sandbox, ignore_errors=True)


class RenamePackageTest(unittest.TestCase):
    def test_rename_moves_sources_and_rewrites_declarations(self) -> None:
        sandbox = make_sandbox()
        try:
            result = run_script(sandbox, "rename_package.py", "com.example.renamed")
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

            new_dir = sandbox / "app" / "src" / "main" / "kotlin" / "com" / "example" / "renamed"
            self.assertTrue(new_dir.exists())
            old_dir = sandbox / "app" / "src" / "main" / "kotlin" / "com" / "factory" / "app"
            self.assertFalse(old_dir.exists())

            app_module = new_dir / "di" / "AppModule.kt"
            self.assertIn("package com.example.renamed.di", app_module.read_text(encoding="utf-8"))

            second = run_script(sandbox, "rename_package.py", "com.example.renamed")
            self.assertIn("unchanged", second.stdout)
        finally:
            shutil.rmtree(sandbox, ignore_errors=True)


class VerifyProjectTest(unittest.TestCase):
    def test_matches_when_untouched(self) -> None:
        sandbox = make_sandbox()
        try:
            result = run_script(sandbox, "verify_project.py", "APP_SPEC.yaml")
            # 0 = fully clean, 2 = matches but missing external config (expected here,
            # since no real google-services.json exists in this sandbox).
            self.assertIn(result.returncode, (0, 2), result.stdout + result.stderr)
            self.assertIn("PASS", result.stdout)
        finally:
            shutil.rmtree(sandbox, ignore_errors=True)

    def test_detects_drift_after_hand_editing_flags(self) -> None:
        sandbox = make_sandbox()
        try:
            flags_file = next(
                (sandbox / "app" / "src" / "main" / "kotlin").rglob("AppSpecFlags.kt"),
            )
            text = flags_file.read_text(encoding="utf-8")
            text = text.replace("AUTH_ENABLED = false", "AUTH_ENABLED = true")
            flags_file.write_text(text, encoding="utf-8")

            result = run_script(sandbox, "verify_project.py", "APP_SPEC.yaml")

            self.assertEqual(result.returncode, 1)
            self.assertIn("AUTH_ENABLED", result.stdout)
        finally:
            shutil.rmtree(sandbox, ignore_errors=True)


class ReleaseCheckTest(unittest.TestCase):
    def test_fails_without_google_services_json_when_firebase_features_enabled(self) -> None:
        sandbox = make_sandbox()
        result = run_script(sandbox, "release_check.py", "APP_SPEC.yaml")
        shutil.rmtree(sandbox, ignore_errors=True)

        self.assertEqual(result.returncode, 1)
        self.assertIn("google-services.json", result.stdout)

    def test_passes_once_google_services_json_is_present_for_every_environment(self) -> None:
        sandbox = make_sandbox()
        try:
            for flavor in ("dev", "staging", "prod"):
                flavor_dir = sandbox / "app" / "src" / flavor
                flavor_dir.mkdir(parents=True, exist_ok=True)
                (flavor_dir / "google-services.json").write_text("{}", encoding="utf-8")

            result = run_script(sandbox, "release_check.py", "APP_SPEC.yaml")

            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertIn("PASS", result.stdout)
        finally:
            shutil.rmtree(sandbox, ignore_errors=True)


if __name__ == "__main__":
    unittest.main()
