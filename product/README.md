# Product specs

What Voice does for listeners, and why. There's one spec per product area. How Voice is built lives in the code and in
[architecture](../docs/architecture.md).

Start with [voice.md](voice.md), the tree for the whole app.

## The shape

Each spec is an [opportunity solution tree](https://www.producttalk.org/opportunity-solution-trees/):

1. **Outcome**: what changes for listeners when this area works well. One sentence.
2. **Opportunities**: listener needs and pain points, in their own words, with evidence.
3. **Solutions**: ways to meet each opportunity, whether shipped, turned down or still an idea.
4. **Assumptions**: what has to be true for a solution to work, and how we'd find out.

Opportunities can have smaller opportunities under them. Solutions always hang under an opportunity, never on their own.

## Markers

- ✅ **Shipped**: in the app today. *(next release)* means it's merged but not released yet. The latest release is
  26.6.1, from June 2026.
- ❌ **Not doing**: decided against, with the reason.
- 💡 **Idea**: an open request or candidate. Not decided.
- **Guess**: no written source. Confirm or correct it.

Opportunity headings are in the listener's voice. They sum up many threads, so they aren't quotes. Links like
[#464](https://github.com/PaulWoitaschek/Voice/issues/464) go to GitHub issues, pull requests and
discussions. Quotes are verbatim.

Each file says how far along it is:

- **Outline**: the structure and main points, waiting to be filled in.
- **Draft**: filled in from sources, waiting for a maintainer to review.
- **Reviewed**: checked by a maintainer.

## Using it before coding

1. **Start from an opportunity, not a solution.** When a request is a solution ("add a button for X"), find the
   opportunity it serves and file it there. If there isn't one, that's the first question.
2. **List at least three solutions.** Include "a better default, no setting" and "do nothing".
3. **Write down the assumptions** each solution depends on. Check the riskiest one cheaply: analytics events, a tester
   build, a feature flag, or a question in a discussion.
4. **Update the spec in the pull request that ships it.** Mark the solution ✅, and add the ones that lost under ❌ with
   the reason.

## Areas

| Area | What it covers | Status |
|---|---|---|
| [Getting started](getting-started.md) | First run, adding folders, how books are found | Outline |
| [Library](library.md) | Finding and managing your books | Outline |
| [Player](player.md) | Listening to a book: controls, chapters, speed, position | Outline |
| [Sleep timer](sleep-timer.md) | Falling asleep to a book | Draft |
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
- 💡 <Idea>: <link>

Assumptions:

- <What has to be true>: <evidence, or how to check>

## Open questions

## How it got here (optional)
```
