#!/bin/bash

set -euo pipefail

decode_env_to_file() {
  local env_var="${1}"
  local dest_file="${2}"

  if [[ -z "${!env_var:-}" ]]; then
    echo "Error: Environment variable ${env_var} is not set or empty." >&2
    exit 1
  fi

  echo "${!env_var}" | base64 --decode >"${dest_file}"
  echo "Success: Written to ${dest_file}"
}

decode_env_to_file "PLAY_SERVICE_ACCOUNT" "app/play_service_account.json"
