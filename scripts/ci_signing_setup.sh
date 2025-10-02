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
mkdir -p signing/github

decode_env_to_file "SIGNING_KEYSTORE_PLAY" "signing/play/signing.keystore"
decode_env_to_file "SIGNING_PROPERTIES_PLAY" "signing/play/signing.properties"

decode_env_to_file "SIGNING_KEYSTORE_GITHUB" "signing/github/signing.keystore"
decode_env_to_file "SIGNING_PROPERTIES_GITHUB" "signing/github/signing.properties"

decode_env_to_file "GOOGLE_SERVICES" "app/google-services.json"
