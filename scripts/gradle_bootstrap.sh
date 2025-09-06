#!/usr/bin/env bash
# Gradle/Kotlin JVM bootstrap – CGroup-aware, with debug output
set -euo pipefail

# Policy
LOCAL_MEM_PCT=75
CI_LINUX_MEM_PCT=20
CI_DARWIN_MEM_PCT=80
CGROUP_MEM_PCT=85
GRADLE_SHARE_PCT=50
KOTLIN_SHARE_PCT=50
LOCAL_XMS_PCT=25
MIN_XMX_GB=1
MIN_XMS_GB=1

is_ci() {
  [[ "${CI:-}" == "true" ]] || [[ -n "${GITHUB_ACTIONS:-}" ]] || [[ -n "${JENKINS_URL:-}" ]] || [[ -n "${BUILDKITE:-}" ]]
}

read_bytes_file() {
  local f=$1
  [[ -r "$f" ]] || return 1
  tr -d '[:space:]' < "$f" || return 1
}

get_effective_memory_bytes_linux() {
  local limit_bytes=""
  local host_kb host_bytes

  # Host memory
  host_kb=$(awk '/MemTotal/ {print $2}' /proc/meminfo)
  host_bytes=$((host_kb * 1024))

  # Try cgroup v2
  if [[ -r /sys/fs/cgroup/cgroup.controllers ]]; then
    local v; v=$(read_bytes_file /sys/fs/cgroup/memory.max || true)
    if [[ -n "${v:-}" && "$v" != "max" ]]; then
      limit_bytes=$v
    else
      v=$(read_bytes_file /sys/fs/cgroup/memory.high || true)
      if [[ -n "${v:-}" && "$v" != "max" ]]; then
        limit_bytes=$v
      fi
    fi
  fi

  # Try cgroup v1
  if [[ -z "${limit_bytes:-}" && -r /sys/fs/cgroup/memory/memory.limit_in_bytes ]]; then
    limit_bytes=$(read_bytes_file /sys/fs/cgroup/memory/memory.limit_in_bytes || true)
  fi

  local huge=$((1<<60))
  if [[ -z "${limit_bytes:-}" || "$limit_bytes" -le 0 || "$limit_bytes" -ge "$huge" ]]; then
    # No real cgroup limit, fall back to host
    echo "$host_bytes" "$host_bytes" "host"
  else
    # Enforce lower of host vs cgroup
    if [[ "$limit_bytes" -gt "$host_bytes" ]]; then
      limit_bytes=$host_bytes
    fi
    echo "$limit_bytes" "$host_bytes" "cgroup"
  fi
}

get_memory_bytes() {
  if [[ "$OSTYPE" == "darwin"* ]]; then
    local bytes; bytes=$(sysctl -n hw.memsize)
    echo "$bytes" "$bytes" "darwin"
  elif [[ "$OSTYPE" == "linux"* ]]; then
    get_effective_memory_bytes_linux
  else
    echo "Unsupported OS: $OSTYPE" >&2
    exit 1
  fi
}

ensure_min() {
  local val=$1 min=$2
  if [[ $val -lt $min ]]; then echo $min; else echo $val; fi
}

main() {
  local bytes host_bytes source
  read bytes host_bytes source < <(get_memory_bytes)
  local mem_gb=$(( bytes / 1024 / 1024 / 1024 ))
  local host_gb=$(( host_bytes / 1024 / 1024 / 1024 ))
  if [[ -z "$mem_gb" || "$mem_gb" -lt 1 ]]; then mem_gb=1; fi

  local ci=false; is_ci && ci=true

  local mem_pct reason
  if [[ "$source" == "cgroup" ]]; then
    mem_pct=$CGROUP_MEM_PCT
    reason="cgroup"
  else
    if $ci; then
      if [[ "$OSTYPE" == "darwin"* ]]; then
        mem_pct=$CI_DARWIN_MEM_PCT
        reason="ci-darwin"
      else
        mem_pct=$CI_LINUX_MEM_PCT
        reason="ci-linux"
      fi
    else
      mem_pct=$LOCAL_MEM_PCT
      reason="local"
    fi
  fi

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
    gradle_xms=$(ensure_min $(( gradle_xmx * LOCAL_XMS_PCT / 100 )) "$MIN_XMS_GB")
    kotlin_xms=$(ensure_min $(( kotlin_xmx * LOCAL_XMS_PCT / 100 )) "$MIN_XMS_GB")
  fi

  local COMMON_JVM="-Dfile.encoding=UTF-8 -XX:+ExitOnOutOfMemoryError -XX:MaxMetaspaceSize=1g"
  local GRADLE_JVM_ARGS="${COMMON_JVM} -Xms${gradle_xms}g -Xmx${gradle_xmx}g"
  local KOTLIN_JVM_ARGS="${COMMON_JVM} -Xms${kotlin_xms}g -Xmx${kotlin_xmx}g"

  local gradle_user_home="${GRADLE_USER_HOME:-$HOME/.gradle}"
  local file="${gradle_user_home}/gradle.properties"
  mkdir -p "$gradle_user_home"
  [[ -f "$file" ]] || : > "$file"

  local prefix="# Begin: Gradle JVM bootstrap-generated properties"
  local suffix="# End: Gradle JVM bootstrap-generated properties"

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

  # Debug output
  echo "=== Gradle bootstrap debug ==="
  echo "Host RAM (GB):            $host_gb"
  echo "Effective RAM (GB):       $mem_gb"
  echo "Detected source:          $source"
  echo "Mode used:                $reason"
  echo "Memory percentage:        $mem_pct%"
  echo "Pool Xmx (GB):            $pool_xmx"
  echo "Gradle Xms/Xmx (GB):      ${gradle_xms}/${gradle_xmx}"
  echo "Kotlin Xms/Xmx (GB):      ${kotlin_xms}/${kotlin_xmx}"
  echo "Properties written to:    $file"
  echo "=============================="
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  main "$@"
fi
