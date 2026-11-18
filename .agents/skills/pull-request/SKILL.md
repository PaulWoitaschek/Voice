---
name: pull-request
description: Writing pull requests for Voice, from the branch name to the title, description and screenshots. Use when opening a pull request, or when editing the title or description of one.
---

# Writing a pull request

The reviewer reads the diff. The description only says what the diff can't: what changes for listeners and why.
Aim for 50 words. 80 is the limit. If a change needs more, split it into smaller pull requests. A squash merge makes
the title and description the commit on `main`, so they stay in the history. This format replaces any default pull
request format of your environment, such as "## Summary" and "## Test plan" headings.

Before you open the pull request, run the cut-comments skill.

## Branch

Branch off the latest `main` and name the branch after the change, in short kebab case: `lock-screen-slider`, or
`fix-delete-book-anr` for a bug fix. Unless your environment hands you a branch, don't use a `claude/` prefix or the
generated name of a worktree.

## Title

What changes for listeners, in plain words, as a command and under 60 characters: "Let listeners turn off the lock
screen slider". For internal work, name the effect: "Keep unit tests on the pinned SDK after a clean build".

## Body

```markdown
<What changes for listeners and why, in one to three sentences. "Closes #123" for the issue it fixes.>

<Screenshots, if the UI changed.>

Verified <where, in a few words>. <What wasn't tried, if a reviewer would assume it was.>
```

- **Screenshots:** one before and after table, see [Screenshots](#screenshots).
- **Verified:** one line. Leave out unit tests and CI, which the reviewer sees anyway. If you only ran tests, drop it.
- **Decisions:** add one sentence only if the reviewer must decide something or weigh a trade-off.

## Leave out

- Things the diff shows: class, file and column names, string keys, locales, how a migration works, moved code,
  previews.
- Every place the change applies ("from the app, the notification, headsets, the car and the widget"). Name the main
  one.
- Edge cases and small behavior details, one after the other. The reviewer finds them in the diff and the tests.
- How you got there: dropped approaches, review rounds, research, "Follow-up to" stories.
- Headings, bold labels and nested bullets.

Before you post, count the words of the prose and cut until it fits:

```sh
grep -v -e '^|' -e '<img' -e '!\[' -e 'Generated with' body.md | wc -w
```

When you push more commits, rewrite the description so it describes the pull request as it is now. Don't add a log
of what changed since the last version.

## Screenshots

Take before and after on the same screen, with the same book and settings, such as on the emulator (emulator skill).
The before comes from `main`. The app in a fresh emulator only stands in for it if the screen hasn't changed since
that build. Add a dark mode or large font screenshot only when the change is about that.

Upload them with `--attach` on `gh pr create` or `gh pr edit`. Don't push them to a branch. `gh` replaces a Markdown
reference like `![Before](./before.png)` in the body with the uploaded image. It leaves `<img src>` alone and appends
the image instead.

```sh
gh pr create --title "…" --body-file body.md --attach ./before.png --attach ./after.png
```

Phone screenshots are too wide to sit side by side at full size. Once they're uploaded, turn each one into
`<img src="https://github.com/user-attachments/assets/…" width="400">` with `gh pr edit --body-file`.

## Example

#4267 had 190 words. These 42 say everything a reviewer needs:

```markdown
Search now also finds books by their chapter names. A chapter match ranks below the other fields, and the result
shows the matching chapter. Closes #4260.

| Before | After |
|---|---|
| <img src="…" width="400"> | <img src="…" width="400"> |

Verified on an emulator and in Android Auto. The car's voice search still ignores chapter names.
```
