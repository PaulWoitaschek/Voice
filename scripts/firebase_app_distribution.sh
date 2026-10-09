#!/bin/bash

set -euo pipefail

app_id="1:789552645904:android:804ab75bd5031ced"
# renovate: datasource=npm depName=firebase-tools
firebase_tools_version="15.32.1"
groups="beta-testers"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --groups)
      groups="${2:?--groups needs comma-separated tester group aliases}"
      shift 2
      ;;
    *)
      echo "Usage: $0 [--groups <comma-separated tester group aliases>]" >&2
      exit 1
      ;;
  esac
done

cd "$(git rev-parse --show-toplevel)"

if [[ -n "$(git status --porcelain)" ]]; then
  echo "Error: Commit your changes first. The release notes name the latest commit." >&2
  exit 1
fi

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
# testers see which branch a build comes from, a detached HEAD has none
branch=$(git branch --show-current)
release_notes="$commit: $(git log -1 --format=%s)"
if [[ -n "$branch" ]]; then
  release_notes="$branch · $release_notes"
fi

./gradlew :app:assemblePlayRelease \
  -Pvoice.versionName="$version_name-$commit" \
  -Pvoice.versionCode="$version_code"

npx --yes "firebase-tools@$firebase_tools_version" appdistribution:distribute \
  app/build/outputs/apk/play/release/app-play-release.apk \
  --app "$app_id" \
  --release-notes "$release_notes" \
  --groups "$groups"
