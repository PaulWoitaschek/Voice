#!/bin/bash

set -euo pipefail

app_id="1:789552645904:android:804ab75bd5031ced"
# renovate: datasource=npm depName=firebase-tools
firebase_tools_version="15.36.0"
groups="beta-testers"
notes=""
usage="Usage: $0 --notes <what the build changes> [--groups <comma-separated tester group aliases>]"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --groups)
      groups="${2:?--groups needs comma-separated tester group aliases}"
      shift 2
      ;;
    --notes)
      notes="${2:?--notes needs a short description of the build}"
      shift 2
      ;;
    *)
      echo "$usage" >&2
      exit 1
      ;;
  esac
done

if [[ -z "$notes" ]]; then
  echo "$usage" >&2
  exit 1
fi

cd "$(git rev-parse --show-toplevel)"

# Without these the release build is unsigned or lacks Firebase. Signing must use the app signing key so tester
# builds install over Play Store installs.
main_checkout=$(dirname "$(git rev-parse --path-format=absolute --git-common-dir)")
for required_file in "$main_checkout/signing/signing.properties" "app/src/play/google-services.json"; do
  if [[ ! -f "$required_file" ]]; then
    echo "Error: $required_file is missing." >&2
    exit 1
  fi
done

# Tester builds carry the version code of the upcoming release, so Play updates can replace them.
git fetch --tags --quiet origin
version_output=$(mktemp)
trap 'rm -f "$version_output"' EXIT
GITHUB_OUTPUT="$version_output" ./scripts/determine_release_tag.main.kts
version_name=$(sed -n 's/^version_name=//p' "$version_output")
version_code=$(sed -n 's/^version_code=//p' "$version_output")

commit=$(git rev-parse --short=7 HEAD)
if [[ -n "$(git status --porcelain)" ]]; then
  commit="$commit-dirty"
fi

./gradlew :app:assemblePlayRelease \
  -Pvoice.versionName="$version_name-$commit" \
  -Pvoice.versionCode="$version_code"

npx --yes "firebase-tools@$firebase_tools_version" appdistribution:distribute \
  app/build/outputs/apk/play/release/app-play-release.apk \
  --app "$app_id" \
  --release-notes "$notes" \
  --groups "$groups"
