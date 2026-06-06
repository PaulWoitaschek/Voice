#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root/artwork"

if [[ ! -d node_modules ]]; then
  npm install
fi
npx playwright install chromium

npm run render
