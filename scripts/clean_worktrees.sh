#!/usr/bin/env bash
# Removes worktrees and local branches whose work is done, and lists the rest.
#
# Work is done when the tip commit is on the default branch, or when it is the
# head commit of a merged pull request. Squash merges count, but only if
# nothing was committed after the pull request merged.
#
# Kept, and listed with the reason:
#   - the main checkout, and the worktree this script runs in
#   - locked worktrees
#   - worktrees with uncommitted changes
#   - worktrees used in the last day (probably an open session)
#   - branches and detached worktrees with commits that are not done
#
# Usage: scripts/clean_worktrees.sh [--apply] [repo path]
# Without --apply it only prints what it would remove.

set -euo pipefail

apply=false
repo="."
for arg in "$@"; do
  case "$arg" in
    --apply) apply=true ;;
    -h | --help) sed -n '2,16p' "$0"; exit 0 ;;
    -*) echo "Unknown option: $arg" >&2; sed -n '15,16p' "$0" >&2; exit 2 ;;
    *) repo="$arg" ;;
  esac
done

cd "$(git -C "$repo" rev-parse --show-toplevel)"
here="$(pwd)"
# The first worktree git lists is always the main checkout.
main_checkout="$(git worktree list --porcelain | head -n 1 | cut -c 10-)"

git fetch --prune --quiet origin
if ! default_ref="$(git symbolic-ref --quiet --short refs/remotes/origin/HEAD)"; then
  git remote set-head origin --auto >/dev/null
  default_ref="$(git symbolic-ref --short refs/remotes/origin/HEAD)"
fi

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
# "branch-name head-oid" of every merged pull request.
gh pr list --state merged --limit 2000 --json headRefName,headRefOid \
  --jq '.[] | "\(.headRefName) \(.headRefOid)"' >"$tmp/merged_prs"
: >"$tmp/kept_branches"

now="$(date +%s)"
recent_seconds=$((24 * 60 * 60))

# GNU stat first: BSD stat rejects -c without printing anything, but GNU stat
# reads -f as a filesystem query and prints to stdout before it fails.
mtime() { stat -c %Y "$1" 2>/dev/null || stat -f %m "$1"; }

# Is the commit $1 (optionally on branch $2) done?
is_done() {
  local commit="$1" branch="${2:-}"
  git merge-base --is-ancestor "$commit" "$default_ref" && return 0
  if [ -n "$branch" ]; then
    grep -qxF "$branch $commit" "$tmp/merged_prs" && return 0
  else
    cut -d' ' -f2 "$tmp/merged_prs" | grep -qxF "$commit" && return 0
  fi
  return 1
}

act() {
  if $apply; then
    echo "remove  $1"
    shift
    "$@"
  else
    echo "would remove  $1"
  fi
}

keep() { echo "keep  $1  ($2)"; }

echo "== Worktrees"
git worktree list --porcelain | awk '
  /^worktree / { path = substr($0, 10) }
  /^HEAD / { head = $2 }
  /^branch / { branch = substr($2, 12) }
  /^detached/ { branch = "" }
  /^locked/ { locked = "locked" }
  /^$/ { print path "\t" head "\t" branch "\t" locked; path = head = branch = locked = "" }
' >"$tmp/worktrees"

while IFS=$'\t' read -r path head branch locked; do
  [ "$path" = "$main_checkout" ] && continue
  label="$path [${branch:-detached}]"
  if [ "$path" = "$here" ] || [ -n "$locked" ]; then
    [ "$path" = "$here" ] && keep "$label" "this script runs in it" || keep "$label" "locked"
    [ -n "$branch" ] && echo "$branch" >>"$tmp/kept_branches"
    continue
  fi

  if [ ! -d "$path" ]; then
    act "$label (folder is gone)" git worktree prune
    continue
  fi
  # Check this before git status, and keep git status from writing the
  # index, so that this script never makes a worktree look used.
  git_dir="$(git -C "$path" rev-parse --absolute-git-dir)"
  last_used="$(mtime "$git_dir/index" 2>/dev/null || mtime "$git_dir/HEAD")"
  if [ $((now - last_used)) -lt $recent_seconds ]; then
    keep "$label" "used in the last day"
    [ -n "$branch" ] && echo "$branch" >>"$tmp/kept_branches"
    continue
  fi
  if [ -n "$(git -C "$path" --no-optional-locks status --porcelain | grep -v '\.DS_Store$' || true)" ]; then
    keep "$label" "uncommitted changes"
    [ -n "$branch" ] && echo "$branch" >>"$tmp/kept_branches"
    continue
  fi
  if ! is_done "$head" "$branch"; then
    keep "$label" "work not done"
    [ -n "$branch" ] && echo "$branch" >>"$tmp/kept_branches"
    continue
  fi
  act "$label" git worktree remove --force "$path" || keep "$label" "could not remove"
done <"$tmp/worktrees"
$apply && git worktree prune

echo
echo "== Branches"
current_branch="$(git -C "$main_checkout" branch --show-current)"
default_branch="${default_ref#origin/}"
git for-each-ref --format='%(refname:short) %(objectname)' refs/heads >"$tmp/branches"
while read -r branch tip; do
  [ "$branch" = "$default_branch" ] && continue
  if [ "$branch" = "$current_branch" ]; then
    keep "$branch" "checked out in the main checkout"
    continue
  fi
  grep -qxF "$branch" "$tmp/kept_branches" && continue
  if ! is_done "$tip" "$branch"; then
    keep "$branch" "work not done"
    continue
  fi
  # A branch can still be checked out in a worktree that was not removed.
  act "$branch" git branch -D --quiet "$branch" || keep "$branch" "could not delete"
done <"$tmp/branches"

if ! $apply; then
  echo
  echo "Dry run. Run again with --apply to remove."
fi
