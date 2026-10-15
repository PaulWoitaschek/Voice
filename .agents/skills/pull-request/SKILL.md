---
name: pull-request
description: Writing pull requests for Voice, from the branch name to the title, description and screenshots. Use when opening a pull request, or when editing the title or description of one.
---

# Writing a pull request

The reviewer reads the diff. The description says what the diff can't: why the change exists, what changes for
listeners, and what the reviewer should look at or decide. Keep the prose under 150 words. A big change can go up to
250. If it's longer than that, cut it down.

## Branch

Branch off the latest `main` and name the branch after the change, in short kebab case: `lock-screen-slider`, or
`fix-delete-book-anr` for a bug fix. Unless your environment hands you a branch, don't use a `claude/` prefix or the
generated name of a worktree.

## Title

What changes for listeners, in plain words: "Let listeners turn off the lock screen slider". For internal work, name
the effect: "Keep unit tests on the pinned Robolectric SDK after a clean build".

## Body

```markdown
## Summary

<The problem, in one to three sentences. Link the issue.>

<What changes, in one to three sentences. Describe behavior, not code.>

<Optional: a side effect, trade-off or open question the reviewer should weigh in on.>

<Screenshots, if the UI changed.>

### Verified

- <Something checked by hand, such as on an emulator.>
- <Something that wasn't tested, like "Not tried on Android Auto".>
```

- **Screenshots:** one before and after table, see [Screenshots](#screenshots). Add a dark mode or large font
  screenshot only when the change is about that.
- **Verified:** only what a reviewer can't see in the diff. Leave out unit tests, which are in the diff, and the CI
  commands, which CI runs. If you only ran tests, drop the section.
- **Limitations:** add a short "Not changed" list only for gaps a reviewer would otherwise assume are covered. Keep it
  to three bullets at most.

## Leave out

- Things the diff shows: class, file and column names, string keys, the number of locales, how a migration works,
  code that moved, comments that were cleaned up, new previews.
- How you got there: approaches you dropped, review rounds, "Follow-up to" stories. One clause is enough when it
  explains a decision.
- A bold label on every bullet, nested bullets, and a heading for each area of the change.
- Small details listed one by one. If a detail matters, write a sentence about it. If not, leave it out.

When you push more commits, rewrite the description so it describes the pull request as it is now. Don't add a log
of what changed since the last version.

## Screenshots

Take before and after on the same screen, with the same book and settings, such as on the emulator (emulator skill).
The before comes from `main`. The app in a fresh emulator only stands in for it if the screen hasn't changed since
that build.

Upload them with `--attach` on `gh pr create` or `gh pr edit`. Don't push them to a branch. `gh` replaces a Markdown
reference like `![Before](./before.png)` in the body with the uploaded image. It leaves `<img src>` alone and appends
the image instead.

```sh
gh pr create --title "…" --body-file body.md --attach ./before.png --attach ./after.png
```

Phone screenshots are too wide to sit side by side at full size. Once they're uploaded, turn each one into
`<img src="https://github.com/user-attachments/assets/…" width="400">` with `gh pr edit --body-file`.

## Example

Before, #3912 had 520 words. This says everything a reviewer needs:

```markdown
## Summary

A stray touch on the notification or lock screen slider moves the listener's place (#3229), but some listeners want
to seek from there (#3289). A new "Lock screen slider" switch in the Listening settings, on by default, turns it off.
The slider then still shows progress, but without a handle. Skip back and forward, the in-app slider, the widget and
Tasker keep working.

Bluetooth and Android Auto read the same media session, so their sliders follow the switch too. Media3 can't set
this separately for each controller.

| On (default) | Off |
|---|---|
| <img src="…" width="400"> | <img src="…" width="400"> |

### Verified

- On an emulator, with the switch off, the notification slider has no handle and dragging it doesn't seek.
- Not tried on Android Auto.
```
