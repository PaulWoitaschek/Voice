# Product specs

What Voice does for listeners, and why. There's one spec per product area. How Voice is built lives in the code and in
[architecture](../docs/architecture.md).

Start with [voice.md](voice.md), the tree for the whole app.

## The shape

Each spec is an [opportunity solution tree](https://www.producttalk.org/opportunity-solution-trees/):

1. **Outcome**: the vision for this area, meaning what changes for listeners when it works well. One sentence.
2. **Opportunities**: listener needs and pain points, in their own words, with evidence.
3. **Solutions**: what's implemented for each opportunity, and what was turned down.
4. **Open problems**: what listeners still struggle with that nothing solves yet.
5. **Assumptions**: what has to be true for a solution to work, and how we'd find out.

Opportunities can have smaller opportunities under them. Solutions always hang under an opportunity, never on their own.

## Markers

- ✅ **Implemented**: merged into main. Releases aren't tracked here.
- ❌ **Not doing**: decided against, with the reason.
- 🔜 **Decided**: chosen but not built yet. It says what it replaces. The pull request that builds it turns it into ✅,
  and what it replaces into ❌ with the reason.
- ⭕ **Open**: a problem nothing solves yet, written as the problem and not as a solution. A problem leaves room for
  the best answer, which can be a better default or nothing at all. An idea from a listener can follow it as a hint.
- **Guess**: no written source. Confirm or correct it.

Opportunity headings are in the listener's voice. They sum up many threads, so they aren't quotes. Quotes are verbatim.

## Keeping it current without upkeep

A spec only changes when the product or a decision changes, so it leaves out what goes stale on its own:

- No releases, versions, feature flags or rollout states.
- No counts of votes, comments or threads.
- No bugs. They live in issues.
- At most one link per point: the discussion that matters most, or the source of a quote. No lists of related threads
  or duplicates.

Each file says how far along it is:

- **Outline**: the structure and main points, waiting to be filled in.
- **Draft**: filled in from sources, waiting for a maintainer to review.
- **Reviewed**: checked by a maintainer.

## Using it before coding

1. **Start from an opportunity, not a solution.** When a request is a solution ("add a button for X"), find the
   problem behind it and add it as ⭕ under its opportunity. If there isn't one, that's the first question.
2. **List at least three solutions.** Include "a better default, no setting" and "do nothing".
3. **Write down the assumptions** each solution depends on. Check the riskiest one cheaply: analytics events, a tester
   build, a feature flag, or a question in a discussion.
4. **Update the spec in the pull request that ships it.** Mark the solution ✅ (it may have been 🔜), add the ones
   that lost under ❌ with the reason, and remove or narrow the ⭕ it solves.

## Areas

| Area | What it covers | Status |
|---|---|---|
| [Getting started](getting-started.md) | First run, adding folders, how books are found | Outline |
| [Library](library.md) | Finding and managing your books | Outline |
| [Player](player.md) | Listening to a book: controls, chapters, speed, position | Outline |
| [Sleep timer](sleep-timer.md) | Falling asleep to a book | Outline |
| [Bookmarks and history](bookmarks.md) | Saving moments and finding your way back | Outline |
| [Outside the app](outside-the-app.md) | Notification, headphones, car, widgets | Outline |
| [Servers, sync and backup](servers-and-sync.md) | Your own server, cloud storage, more devices, a new phone | Outline |
| [Look and feel](look-and-feel.md) | Themes, colors, motion, accessibility, languages | Outline |
| [Feedback and support](feedback-and-support.md) | Getting help, giving feedback, supporting Voice | Outline |

## Template

```markdown
# <Area>

**Status:** Outline

## Outcome

<One sentence: what changes for listeners when this works well.>

## Who and when

<The listeners, and the moment they're in.>

## Opportunities

### "<The problem, in the listener's words>"

<Why it matters, with a quote and a link.>

- ✅ <Shipped solution>: <how it helps>
- ❌ <Solution we turned down>: <the reason, quoted and linked>
- ⭕ <What listeners still struggle with>: <a link to the main discussion, if there is one>

Assumptions:

- <What has to be true>: <evidence, or how to check>

## Open questions

## How it got here (optional)
```
