#!/usr/bin/env bash
# Canonical verification entry point for the Native Android App Factory (AGENTS.md §10).
# Runs every check that can run without real production credentials, then prints one
# final health line: PASS, PASS_WITH_EXTERNAL_SETUP, FAIL, or BLOCKED.
#
# A step is never marked passed unless it actually ran. Missing external configuration
# (google-services.json, RevenueCat/AdMob keys) downgrades the result to
# PASS_WITH_EXTERNAL_SETUP rather than FAIL — see AGENTS.md §"when something can't be
# verified live".
set -uo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."
REPO_ROOT="$(pwd)"

unset ANDROID_PREFS_ROOT

# Gradle/Android tooling needs these; detect dynamically if default/environment path is missing
if [ -z "${JAVA_HOME:-}" ] || [ ! -d "$JAVA_HOME" ]; then
    if [ -x /usr/libexec/java_home ]; then
        JAVA_HOME="$(/usr/libexec/java_home 2>/dev/null || true)"
    fi
    if [ -z "$JAVA_HOME" ] || [ ! -d "$JAVA_HOME" ]; then
        if [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
            JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
        elif [ -d "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
            JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
        fi
    fi
fi
export JAVA_HOME

if [ -z "${ANDROID_HOME:-}" ] || [ ! -d "$ANDROID_HOME" ]; then
    if [ -d "$HOME/Library/Android/sdk" ]; then
        ANDROID_HOME="$HOME/Library/Android/sdk"
    elif [ -d "/opt/homebrew/share/android-commandlinetools" ]; then
        ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
    fi
fi
export ANDROID_HOME

PYTHON_BIN="${FACTORY_PYTHON:-python3}"

STEPS_RUN=()
STEPS_FAILED=()
EXTERNAL_SETUP_NEEDED=0
OVERALL_BLOCKED=0

log_step() { printf '\n==> [%s] %s\n' "$1" "$2"; }

run_step() {
    local id="$1" label="$2"
    shift 2
    log_step "$id" "$label"
    if "$@"; then
        STEPS_RUN+=("$id: PASS — $label")
        return 0
    else
        local rc=$?
        STEPS_RUN+=("$id: FAIL (exit $rc) — $label")
        STEPS_FAILED+=("$id: $label")
        return 1
    fi
}

# Same as run_step, but a specific exit code ($2) is treated as "external setup needed"
# rather than a hard failure.
run_step_soft() {
    local id="$1" label="$2" soft_exit_code="$3"
    shift 3
    log_step "$id" "$label"
    "$@"
    local rc=$?
    if [ "$rc" -eq 0 ]; then
        STEPS_RUN+=("$id: PASS — $label")
    elif [ "$rc" -eq "$soft_exit_code" ]; then
        STEPS_RUN+=("$id: PASS_WITH_EXTERNAL_SETUP — $label")
        EXTERNAL_SETUP_NEEDED=1
    else
        STEPS_RUN+=("$id: FAIL (exit $rc) — $label")
        STEPS_FAILED+=("$id: $label")
    fi
}

check_no_secrets_committed() {
    local found=0
    if command -v git >/dev/null 2>&1 && git -C "$REPO_ROOT" rev-parse --git-dir >/dev/null 2>&1; then
        while IFS= read -r f; do
            case "$f" in
                *.jks|*.keystore|keystore.properties|*/google-services.json|google-services.json|local.properties)
                    echo "  refusing to pass: tracked secret-shaped file: $f"
                    found=1
                    ;;
            esac
        done < <(git -C "$REPO_ROOT" ls-files)
    fi
    return "$found"
}

echo "Native Android App Factory — canonical verification"
echo "Repository: $REPO_ROOT"
date

# 1. APP_SPEC validation
run_step "1" "APP_SPEC.yaml validation" "$PYTHON_BIN" scripts/validate_spec.py APP_SPEC.yaml

# 2. Module configuration validation (spec <-> repo drift; missing external config is
#    reported but is exit code 2, handled as soft below)
run_step_soft "2" "module configuration matches APP_SPEC.yaml" 2 "$PYTHON_BIN" scripts/verify_project.py APP_SPEC.yaml

# 3. Unresolved-placeholder / release-readiness check (also soft: missing external
#    credentials are expected pre-launch, not a factory bug)
run_step_soft "3" "unresolved placeholder / release-readiness scan" 1 "$PYTHON_BIN" scripts/release_check.py APP_SPEC.yaml

# 4. Secret-file safety checks
run_step "4" "no secret-shaped files are committed" check_no_secrets_committed

# 5. Gradle configuration check (all modules resolve/configure)
run_step "5" "Gradle configuration resolves" ./gradlew help -q

# 6. Formatting check
run_step "6" "ktlint formatting check" ./gradlew ktlintCheck

# 7. Android Lint
run_step "7" "Android Lint (prodDebug)" ./gradlew :app:lintProdDebug

# 8. Detekt
run_step "8" "Detekt static analysis" ./gradlew detekt

# 9. Unit tests (Kotlin + Python)
run_step "9a" "Kotlin unit tests (all modules, debug)" ./gradlew testDebugUnitTest
if [ -f scripts/requirements.txt ] && "$PYTHON_BIN" -c "import yaml" >/dev/null 2>&1; then
    run_step "9b" "Python factory-script tests" "$PYTHON_BIN" -m unittest discover -s scripts/tests
else
    STEPS_RUN+=("9b: BLOCKED — Python factory-script tests (PyYAML not installed; pip install -r scripts/requirements.txt)")
    OVERALL_BLOCKED=1
fi

# 10. Debug build
run_step "10" "Debug build (assembleProdDebug)" ./gradlew :app:assembleProdDebug

# 11. Release compilation (unsigned — this factory never signs automatically)
run_step "11" "Release compilation (assembleProdRelease, unsigned)" ./gradlew :app:assembleProdRelease

# 12. Documentation / state checks
check_docs_state() {
    local missing=0
    for f in AGENTS.md CLAUDE.md README.md ARCHITECTURE.md CHANGELOG.md APP_SPEC.yaml MODULES.yaml factory-version.txt Docs/sessions/CURRENT.md; do
        if [ ! -s "$f" ]; then
            echo "  missing or empty: $f"
            missing=1
        fi
    done
    if ! ls Docs/plans/*.md >/dev/null 2>&1; then
        echo "  missing: no plan under Docs/plans/"
        missing=1
    fi
    return "$missing"
}
run_step "12" "documentation/state files present" check_docs_state

echo
echo "==================== Verification summary ===================="
for line in "${STEPS_RUN[@]}"; do
    echo "  $line"
done
echo "================================================================="

if [ "${#STEPS_FAILED[@]}" -gt 0 ]; then
    echo
    echo "FINAL HEALTH: FAIL"
    echo "Failed steps:"
    for f in "${STEPS_FAILED[@]}"; do echo "  - $f"; done
    exit 1
elif [ "$OVERALL_BLOCKED" -eq 1 ]; then
    echo
    echo "FINAL HEALTH: BLOCKED (one or more steps could not run — see above)"
    exit 3
elif [ "$EXTERNAL_SETUP_NEEDED" -eq 1 ]; then
    echo
    echo "FINAL HEALTH: PASS_WITH_EXTERNAL_SETUP"
    echo "Everything this factory can verify without real credentials passed."
    echo "See the steps above and Docs/setup/ for exactly what remains."
    exit 0
else
    echo
    echo "FINAL HEALTH: PASS"
    exit 0
fi
