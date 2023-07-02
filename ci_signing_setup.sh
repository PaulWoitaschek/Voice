#!/bin/bash

decode_env_to_file() {
  local env_var="${1}"
  local dest_file="${2}"
  if [[ -n "${!env_var}" ]]; then
    if [[ ! -f "${dest_file}" ]]; then
      echo "${!env_var}" | base64 --decode >"${dest_file}"
      echo "Success: Written to ${dest_file}"
    else
      echo "Warning: File ${dest_file} already exists, not overwritten."
    fi
  else
    echo "Warning: Environment variable ${env_var} is empty or not set, no action taken."
  fi
}

mkdir -p signing/play
mkdir -p signing/github

decode_env_to_file "SIGNING_KEYSTORE_PLAY" "signing/play/signing.keystore"
decode_env_to_file "SIGNING_PROPERTIES_PLAY" "signing/play/signing.properties"

decode_env_to_file "SIGNING_KEYSTORE_GITHUB" "signing/github/signing.keystore"
decode_env_to_file "SIGNING_PROPERTIES_GITHUB" "signing/github/signing.properties"

decode_env_to_file "GOOGLE_SERVICES" "app/google-services.json"
