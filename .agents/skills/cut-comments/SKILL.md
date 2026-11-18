---
name: cut-comments
description: Cutting the code comments and commit messages of a branch down to what Voice keeps, before its pull request opens. Use before opening a pull request, when the pull-request skill names it, or when asked to cut or clean up comments or commit messages.
---

# Cutting comments and commits

A comment stays only when it explains a non-obvious *why*. Commit messages stay one short line. Only touch what the
branch added or changed, not older code.

## Comments

Find the comments the branch adds:

```sh
git diff origin/main...HEAD -U0 -- . ':!*.md' | grep -nE '^\+.*(//|/\*|<!--)|^\+[[:space:]]*(\*|#)'
```

The grep misses some comments and finds `//` in URLs, so also read the diff. For each comment:

- **Delete** it when it restates the code or the name, describes the change ("now uses", "changed to", "new"), tells
  how you got there, or repeats a signature in KDoc. Delete commented-out code too.
- **Keep** it when the code alone can't say why: a platform or library bug, an order that must stay, a value that
  looks wrong but isn't. Cut it to one sentence in Simplified Technical English.
- **Leave** the comments in `core/strings/src/main/res/values/strings.xml`. Translators need them, see the
  translate-strings skill.

If a comment explains code that is hard to follow, a better name or a smaller function often replaces it.

Commit the cut as its own commit, or amend it into the last commit when the branch isn't pushed.

## Commit messages

The squash merge takes the pull request title and description, so commit messages don't reach `main`. Reviewers
still read them. Write one line in the imperative, under 60 characters, with no body except the trailers your
environment asks for.

When the branch isn't pushed and has long messages or many small commits, squash them into one:

```sh
git reset --soft "$(git merge-base origin/main HEAD)"
git commit -m "Let listeners turn off the lock screen slider"
```

Don't rewrite commits that are already pushed, unless the user asks.
