#!/usr/bin/env bash
set -euo pipefail

main_branch="${MAIN_BRANCH:-main}"
strings_remote="${STRINGS_REMOTE:-weblate-strings}"
metadata_remote="${METADATA_REMOTE:-weblate-metadata}"
strings_dir="core/strings/src/main/res"
metadata_dir="fastlane/metadata/android"

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "Refusing to run with local tracked changes." >&2
  exit 1
fi

untracked_target_files="$(git ls-files --others --exclude-standard -- "$strings_dir" "$metadata_dir")"
if [[ -n "$untracked_target_files" ]]; then
  echo "Refusing to overwrite untracked files in $strings_dir or $metadata_dir:" >&2
  echo "$untracked_target_files" >&2
  exit 1
fi

git fetch origin "$main_branch"
git fetch "$strings_remote" main
git fetch "$metadata_remote" main

git switch "$main_branch"
git pull --ff-only origin "$main_branch"

git restore --source="$strings_remote/main" --staged --worktree -- "$strings_dir"
git restore --source="$metadata_remote/main" --staged --worktree -- "$metadata_dir"

echo "Imported Weblate strings from $strings_remote/main into $strings_dir."
echo "Imported Weblate metadata from $metadata_remote/main into $metadata_dir."
echo "You are on $main_branch with the imported files staged."
