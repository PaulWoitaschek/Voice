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

mkdir -p signing/play
mkdir -p signing/free
mkdir -p app/src/play

decode_env_to_file "SIGNING_KEYSTORE_PLAY" "signing/play/signing.keystore"

decode_env_to_file "SIGNING_KEYSTORE_GITHUB" "signing/free/signing.keystore"

decode_env_to_file "GOOGLE_SERVICES" "app/src/play/google-services.json"
