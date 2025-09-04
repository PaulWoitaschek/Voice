#!/usr/bin/env bash
# Minimal, quiet Gradle JVM bootstrap. Always configures Kotlin daemon.
set -euo pipefail

# Policy (percent values are integers)
LOCAL_MEM_PCT=50    # 50% of RAM locally
CI_MEM_PCT=75       # 75% of RAM in CI
GRADLE_SHARE_PCT=50 # 50% of the pool to Gradle
KOTLIN_SHARE_PCT=50 # 50% of the pool to Kotlin
LOCAL_XMS_PCT=25    # local Xms = 25% of Xmx
MIN_XMX_GB=1
MIN_XMS_GB=1

is_ci() {
  [[ "${CI:-}" == "true" ]] || [[ -n "${GITHUB_ACTIONS:-}" ]] || [[ -n "${JENKINS_URL:-}" ]] || [[ -n "${BUILDKITE:-}" ]]
}

get_memory_gb() {
  if [[ "$OSTYPE" == "darwin"* ]]; then
    local bytes; bytes=$(sysctl -n hw.memsize); echo $((bytes / 1024 / 1024 / 1024))
  elif [[ "$OSTYPE" == "linux"* ]]; then
    local kb; kb=$(grep MemTotal /proc/meminfo | awk '{print $2}'); echo $((kb / 1024 / 1024))
  else
    echo "Unsupported OS: $OSTYPE" >&2
    exit 1
  fi
}

ensure_min() {
  local val=$1 min=$2
  if [[ $val -lt $min ]]; then
    echo $min
  else
    echo $val
  fi
}

main() {
  local mem_gb; mem_gb=$(get_memory_gb)
  if [[ -z "$mem_gb" || "$mem_gb" -lt 1 ]]; then
    mem_gb=1
  fi

  local ci=false
  is_ci && ci=true

  local mem_pct
  if $ci; then
    mem_pct=$CI_MEM_PCT
  else
    mem_pct=$LOCAL_MEM_PCT
  fi

  # pool_xmx = floor(mem_gb * mem_pct / 100)
  local pool_xmx=$(( mem_gb * mem_pct / 100 ))
  pool_xmx=$(ensure_min "$pool_xmx" "$MIN_XMX_GB")

  local gradle_xmx=$(( pool_xmx * GRADLE_SHARE_PCT / 100 ))
  gradle_xmx=$(ensure_min "$gradle_xmx" "$MIN_XMX_GB")

  local kotlin_xmx=$(( pool_xmx * KOTLIN_SHARE_PCT / 100 ))
  kotlin_xmx=$(ensure_min "$kotlin_xmx" "$MIN_XMX_GB")

  local gradle_xms kotlin_xms
  if $ci; then
    gradle_xms=$gradle_xmx
    kotlin_xms=$kotlin_xmx
  else
    gradle_xms=$(( gradle_xmx * LOCAL_XMS_PCT / 100 ))
    gradle_xms=$(ensure_min "$gradle_xms" "$MIN_XMS_GB")
    kotlin_xms=$(( kotlin_xmx * LOCAL_XMS_PCT / 100 ))
    kotlin_xms=$(ensure_min "$kotlin_xms" "$MIN_XMS_GB")
  fi

  local GRADLE_JVM_ARGS="-Dfile.encoding=UTF-8 -XX:+ExitOnOutOfMemoryError -Xms${gradle_xms}g -Xmx${gradle_xmx}g"
  local KOTLIN_JVM_ARGS="-Dfile.encoding=UTF-8 -XX:+ExitOnOutOfMemoryError -Xms${kotlin_xms}g -Xmx${kotlin_xmx}g"

  local gradle_user_home="${GRADLE_USER_HOME:-$HOME/.gradle}"
  local file="${gradle_user_home}/gradle.properties"
  mkdir -p "$gradle_user_home"
  [[ -f "$file" ]] || : > "$file"

  local prefix="# Begin: Gradle JVM bootstrap-generated properties"
  local suffix="# End: Gradle JVM bootstrap-generated properties"

  # Remove existing section if present
  if grep -q "^${prefix}$" "$file" 2>/dev/null; then
    local tmp; tmp=$(mktemp)
    awk -v b="$prefix" -v e="$suffix" '
      $0 == b {inblock=1; next}
      $0 == e {inblock=0; next}
      !inblock {print}
    ' "$file" > "$tmp"
    mv "$tmp" "$file"
  fi

  {
    printf '%s\n' "$prefix"
    printf '%s\n' "org.gradle.jvmargs=${GRADLE_JVM_ARGS}"
    printf '%s\n' "kotlin.daemon.jvm.options=${KOTLIN_JVM_ARGS}"
    printf '%s\n' "$suffix"
  } >> "$file"
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  main "$@"
fi
