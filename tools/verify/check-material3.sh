#!/usr/bin/env bash
# Material3 dependencyInsight gate: fails if any compile/runtime classpath of :library or
# :showcase resolves Material3 to anything other than the pinned stable coordinate, or to an
# alpha build (e.g. via a third-party dependency edge such as Compose Hot Reload's devtools).
#
# Usage: tools/verify/check-material3.sh <log-file>
set -euo pipefail

if [ "$#" -lt 1 ]; then
    echo "Usage: $0 <log-file>" >&2
    exit 1
fi

LOG_FILE="$1"
PINNED_COORDINATE="org.jetbrains.compose.material3:material3:1.9.0"

: > "$LOG_FILE"

run_insight() {
    local module="$1"
    local configuration="$2"
    echo "=== ${module}:dependencyInsight --dependency material3 --configuration ${configuration} ===" >> "$LOG_FILE"
    ./gradlew -q "${module}:dependencyInsight" --dependency material3 --configuration "${configuration}" >> "$LOG_FILE" 2>&1
}

run_insight ":library" "compileClasspath"
run_insight ":library" "runtimeClasspath"
run_insight ":showcase" "compileClasspath"
run_insight ":showcase" "runtimeClasspath"

echo "=== :showcase:dependencies ===" >> "$LOG_FILE"
./gradlew -q ":showcase:dependencies" >> "$LOG_FILE" 2>&1

# The whole log (four dependencyInsight runs + the full :showcase:dependencies graph) must
# never mention "alpha" — this covers every configuration, including ones a future dependency
# (e.g. Compose Hot Reload's devtools) might add outside compile/runtimeClasspath.
if grep -qi "alpha" "$LOG_FILE"; then
    echo "MATERIAL3 FAIL: alpha found on a classpath — see $LOG_FILE" >&2
    grep -i "alpha" "$LOG_FILE" >&2
    exit 1
fi

if ! grep -qF "$PINNED_COORDINATE" "$LOG_FILE"; then
    echo "MATERIAL3 FAIL: pinned coordinate $PINNED_COORDINATE not found in any dependencyInsight output — see $LOG_FILE" >&2
    exit 1
fi

echo "MATERIAL3 OK"
