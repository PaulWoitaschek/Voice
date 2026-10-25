---
name: release-notes
description: Writing the GitHub release notes for a Voice release. Use when asked to write, fill in or edit the notes or the title of a GitHub release or draft.
---

# Release notes

The release workflow creates a draft release from `.github/release_notes_template.md`. Fill in its title and its
`## What's New`, and keep the "Support Voice" block below it as it is.

Find the changes with `git log <previous tag>..<tag>` and read the pull requests behind them.

## Title

The version without the version code, a dash and a short headline for the biggest change, with one emoji:
`26.10.1 - A fresh new look ✨`.

## What's New

One bullet per change a listener notices, in plain words, in one or two sentences. A feature big enough for several
bullets gets its own `###` section with an emoji in the heading, most important first. Group the smaller changes under
a shared heading, such as `### More ✨`. Fixes go last under `### Fixes 🐞`, each saying what was broken.

When someone outside the project wrote a change, or sent the corrections it applies, thank them by their GitHub handle
at the end of its bullet: `Thanks to @steliyan for fixing it!`.

Leave out tests, CI, dependencies, refactors, changes to the Google Play support sheet, and GitHub's generated
changelog.

## Images

A section can show its change with images, right under its heading, on one line:

```html
<img src="…" alt="…" width="250"> <img src="…" alt="…" width="250">
```

Take them from the pull requests when they show the change as it ships: the after image, on a phone with real books, not
outdated by a later pull request. For a very important change without a fitting image, take one yourself with the
emulator skill. `gh release edit` can't upload images, so attach it to that change's pull request with
`gh pr edit <number> --attach ./image.png` and use the uploaded URL from its body.

## Updating the draft

```sh
gh release edit <tag> --title "…" --notes-file notes.md
```

Don't publish the release. The maintainer does that.
