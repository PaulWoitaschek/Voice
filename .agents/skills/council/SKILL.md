---
name: council
description: Reviewing a branch with a council of independent reviewers, each with one view (bugs, the brief, simplicity, the listener), then fixing the findings and reviewing again until no high or medium finding is left. Use when asked for a council review, a full or thorough review of a branch before its pull request, or when another skill names the council.
---

# Council

Each seat reviews the branch from one view and doesn't know the reasons of the author. You are the chair: you start
the seats, keep the ledger, rate and fix the findings, and review again. Don't ask before you fix a high or medium
finding.

## 1. Prepare

- Commit all work.
- `git fetch origin main`. Review against `origin/main`, not the local branch. In worktrees the local `main` is often
  stale.
- Find the brief: the text that says what the change must do for listeners, and what it leaves out. It can be an
  issue, a file or the request of the user. With none, write it in three sentences from the commits, and say so in
  your report.
- Open the [ledger](#the-ledger). If the branch has one, continue it.

## 2. Seats

You decide which seats run, from what the diff can break. A small fix with tests can need one or two seats. A new
screen, stored data or a change for the car needs all of them. Say in the report which seats ran and why.

Give each seat to its own subagent, all at the same time, except the Simplicity seat: it runs [last](#5-repeat). A
seat gets the brief, the diff against `origin/main`, the ledger and its task below. It doesn't get this chat or your
reasons. Without subagents, do the seats yourself, one after another.

Where the tool lets you set the model of a subagent, run the Rules seat on Sonnet. It compares the diff with written
rules, and you check each finding in step 3. The other seats keep your model.

| Seat | When | Task |
|---|---|---|
| Bugs | Most diffs | Find what breaks: crashes, lost data, wrong behavior, races, broken tests. Run `/code-review high origin/main` where the tool has it. |
| Brief | Most diffs | Each rule and example of the brief has a test that checks it. The diff builds nothing that the brief leaves out, and nothing it doesn't ask for. |
| Simplicity | Last, when the diff adds logic | Find what can go, by the complexity rule in `AGENTS.md`: stored data, a migration, a setting, a library, an abstraction with one user, code for a case that can't happen. Name the smaller version. |
| Rules | When the diff adds code, comments or strings | Find breaks of `AGENTS.md` and the skills that apply to the changed files: architecture, logging, comments, format-and-test, translate-strings. |
| Listener | Only with a [reason](#the-listener-seat) | Check what its reasons name. |

A finding has:

- the file and the line,
- the case: where the listener is, what they do, and what goes wrong. For the simplicity seat: what can go, and why
  the behavior stays the same,
- a rating.

A seat without a concrete case for a finding leaves it out. A seat doesn't report a finding that is in the ledger
again. When a fix didn't work, or a seat has a new argument against a `wrong`, it names the row.

### The Listener seat

This seat costs the most: it builds the branch, starts an emulator and does the examples by hand. Start it only with
a reason from this table, and tell the seat its reasons. The seat checks what they name, and not the rest of the
brief.

| Reason | Check |
|---|---|
| The diff changes the layout of a screen, a sheet, a list row, a widget or the notification. | Render the previews of the changed UI and check them against the design skill. Use the emulator skill only for what previews can't show, like a flow, motion, the widget or the notification. |
| No unit test can show an example of the brief, like a widget tap or a cold start. | Install the branch with the emulator skill and do these examples by hand. |
| The diff changes the media session. | Check the notification on the emulator, and the car with the android-auto skill. |
| The diff changes a page of the website. | Build the website and look at the page in a browser, also at phone width. |

With no reason, the seat doesn't run. That is so for logic that unit tests show in full, a changed string, a
refactoring and a build file.

## 3. Rate

Merge findings that are the same. Check each one in the code yourself before you rate it.

- **High:** a crash, lost data or a lost place, a security or privacy problem, wrong behavior in a normal flow, a
  broken build or broken tests, an example of the brief that doesn't work.
- **Medium:** wrong behavior in an edge case that can really happen, no test for new logic, something built that the
  brief leaves out, code that can go without a change for listeners, a break of a rule in `AGENTS.md`.
- **Low:** naming, style, a small cleanup, or a case that can't really happen.

If you are not sure, take the higher rating. Don't talk a finding down.

Give each finding a row in the ledger, with the state `open`, before you fix anything.

## 4. Fix

- Fix all high and medium findings. If one is wrong, don't fix it. Set its row to `wrong` with the reason.
- Weigh a medium edge case against the complexity rule in `AGENTS.md` first. When the case is rare and its fix adds
  new logic, like an effect, stored state or a branch that needs its own tests, don't fix it. Set its row to
  `left out` with the reason, and name the case in the pull request.
- Fix a low finding only when the fix is small and clearly makes the code better. If not, set its row to `skipped`.
- When a finding says the brief is wrong, don't change the brief. Stop and ask.
- Run the format-and-test skill, then commit the fixes. Set each fixed row to `fixed` with the commit.

## 5. Repeat

You decide if another round is worth it. Run one when a fix changed something that a seat should see again, like new
logic or a fix you are not sure of. Pick the seats for it the same way as in step 2. Stop when the ledger has no open
row and no fix needs another look. Run at most four rounds. If round four still has an open high or medium row, stop,
show it and ask.

Then run the Simplicity seat on the final diff, so that it also sees the code that the fixes added. Its fixes only
remove code. When one changes behavior, run the seat that checks it again.

## 6. Report

Show the ledger. When the Listener seat didn't run, say so. The rounds don't go into the pull request.

## The ledger

The ledger is the list of all findings of the branch, over all rounds. It keeps a fixed finding fixed and a wrong
finding wrong, also after a long run or in a new session.

Keep it in the Git directory of the worktree, so that it stays with the branch and can't be committed:

```sh
echo "$(git rev-parse --git-dir)/council.md"
```

One row for each finding. A finding from several seats gets one row that names them all.

```markdown
| # | Round | Seat | Rating | Finding | State |
|---|---|---|---|---|---|
| 1 | 1 | Bugs | High | `PlayerViewModel.kt:120`: Next in the last chapter of a paused book starts playback. | fixed in `1a2b3c4` |
| 2 | 1 | Simplicity | Medium | The new store can go, the book has the value already. | wrong: the car reads it before the book loads |
| 3 | 2 | Rules | Low | The log line in the scanner has no use. | skipped |
```

- **State:** `open`, `fixed in <commit>`, `wrong: <reason>`, `left out: <reason>` or `skipped`.
- Never delete a row. When a seat shows that a fix didn't work, set the row back to `open` and add the round.
- A `wrong` row only opens again when a seat gives an argument that the reason doesn't answer.
