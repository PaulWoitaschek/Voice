#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root/artwork"

if [[ ! -d node_modules ]]; then
  npm install
  npx playwright install chromium
fi

npm run render
