# Product specs

What Voice does for listeners, and why. There's one spec per product area. How Voice is built lives in the code and in
[architecture](../docs/architecture.md).

Start with [voice.md](voice.md), the overview for the whole app.

## The shape

A spec is a list of the problems listeners have in its area. Each problem has exactly one status and says what Voice
does about it. A table at the top shows where every problem stands.

| Status | Meaning |
|---|---|
| ✅ Solved | Voice solves it on main. Releases aren't tracked here. |
| 🔜 Planned | A solution is decided but not built yet. |
| ⭕ Open | Nothing solves it yet. |
| ❌ Won't solve | Decided against solving it, with the reason. |

A problem that's only partly solved is two problems: the part that's solved and the part that's still open.

- **Problems are in plain words, from the listener's side**: what goes wrong, not what's missing. "Saving a moment
  means taking out the phone", not "A bookmark widget". That leaves room for the best answer, which can be a better
  default or nothing at all.
- **Solutions** say what Voice does. Under **Turned down**, the alternatives that lost, each with its reason.
- **Sources are links, not quotes.** Each problem links the one issue, discussion or pull request that matters most,
  and a turned-down solution links where its reason was given. Say in your own words what the source says.
- **Guess** marks something with no written source. Confirm or correct it.
- **Sketches** of a planned solution go in `product/sketches/`, drawn with the sketching skill.

## Keeping it current without upkeep

A spec only changes when the product or a decision changes, so it leaves out what goes stale on its own:

- No releases, versions, feature flags or rollout states.
- No counts of votes, comments or threads.
- No bugs. They live in issues.

Each file says how far along it is:

- **Outline**: the structure and main points, waiting to be filled in.
- **Draft**: filled in from sources, waiting for a maintainer to review.
- **Reviewed**: checked by a maintainer.

## Using it before coding

1. **Start from a problem, not a solution.** When a request is a solution ("add a button for X"), find the problem
   behind it. If it's not in the spec yet, add it as ⭕.
2. **Weigh at least three solutions.** Include "a better default, no setting" and "do nothing".
3. **Write down the assumption** the chosen solution depends on. Check it cheaply: analytics events, a tester build, a
   feature flag, or a question in a discussion.
4. **Decide in the spec.** The problem becomes 🔜 with the chosen solution, and the others go under Turned down.
5. **Update the spec in the pull request that ships it.** 🔜 becomes ✅.

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

<One sentence: what changes for listeners when this area works well.>

**When:** <The listeners, and the moment they're in.>

| Problem | Status |
|---|---|
| [<Problem>](#<problem-as-github-anchor>) | ✅ Solved |

## <Problem>

✅ **Solved** · [#123]

<What goes wrong, for whom and when. One to three sentences.>

**Solution:** <What Voice does. A list when there's more than one part.>

**Turned down:**

- <Another solution>: <the reason> ([#456]).

**Assumption:** <What has to be true for the solution to work>: <evidence, or how to check>.

## Open questions

1. <A decision that still has to be made.>

[#123]: https://github.com/PaulWoitaschek/Voice/issues/123
[#456]: https://github.com/PaulWoitaschek/Voice/discussions/456
```

Leave out the parts a problem doesn't need. An open problem has no Solution, and a problem Voice won't solve says why
under **Why not**.
