---
name: council
description: Reviewing a branch with a council of independent reviewers, each with one view (bugs, the brief, simplicity, the listener), then fixing the findings and reviewing again until no high or medium finding is left. Use when asked for a council review, a full or thorough review of a branch before its pull request, or when another skill names the council.
---

# Council

Each seat reviews the branch from one view and doesn't know the reasons of the author. You are the chair: you start
the seats, rate and fix their findings, and review again. Don't ask before you fix a high or medium finding.

## 1. Prepare

- Commit all work.
- `git fetch origin main`. Review against `origin/main`, not the local branch. In worktrees the local `main` is often
  stale.
- Find the brief: the text that says what the change must do for listeners, and what it leaves out. It can be an
  issue, a file or the request of the user. With none, write it in three sentences from the commits, and say so in
  your report.

## 2. Seats

Give each seat to its own subagent, all at the same time. A seat gets the brief, the diff against `origin/main` and
its task below. It doesn't get this chat or your reasons. Without subagents, do the seats yourself, one after another.

| Seat | When | Task |
|---|---|---|
| Bugs | Always | Find what breaks: crashes, lost data, wrong behavior, races, broken tests. Run `/code-review high origin/main` where the tool has it. |
| Brief | Always | Each rule and example of the brief has a test that checks it. The diff builds nothing that the brief leaves out, and nothing it doesn't ask for. |
| Simplicity | Always | Find what can go, by the complexity rule in `AGENTS.md`: stored data, a migration, a setting, a library, an abstraction with one user, code for a case that can't happen. Name the smaller version. |
| Rules | Always | Find breaks of `AGENTS.md` and the skills that apply to the changed files: architecture, logging, comments, format-and-test, translate-strings. |
| Listener | When listeners can see or hear the change | Install the branch with the emulator skill and do each example of the brief by hand. For UI, check it against the design skill, in the app and in the previews. When the media session changes, check the notification, and the car with the android-auto skill. |

A finding has:

- the file and the line,
- the case: where the listener is, what they do, and what goes wrong. For the simplicity seat: what can go, and why
  the behavior stays the same,
- a rating.

A seat without a concrete case for a finding leaves it out.

## 3. Rate

Merge findings that are the same. Check each one in the code yourself before you rate it.

- **High:** a crash, lost data or a lost place, a security or privacy problem, wrong behavior in a normal flow, a
  broken build or broken tests, an example of the brief that doesn't work.
- **Medium:** wrong behavior in an edge case that can really happen, no test for new logic, something built that the
  brief leaves out, code that can go without a change for listeners, a break of a rule in `AGENTS.md`.
- **Low:** naming, style, a small cleanup, or a case that can't really happen.

If you are not sure, take the higher rating. Don't talk a finding down.

## 4. Fix

- Fix all high and medium findings. If one is wrong, don't fix it and write down why. When it comes back in a later
  round with no new argument, it is not open.
- Fix a low finding only when the fix is small and clearly makes the code better.
- When a finding says the brief is wrong, don't change the brief. Stop and ask.
- Run the format-and-test skill, then commit the fixes.

## 5. Repeat

Go back to step 2 with the seats that had a high or medium finding. They review the whole diff again. Stop when a
round has no new high or medium finding. Run at most four rounds. If round four still has one, stop, list the open
findings and ask.

## 6. Report

A short table, one row per finding: round, seat, rating, finding, and fixed, skipped or wrong with the reason. The
rounds don't go into the pull request.
