# Getting started

**Status:** Outline.

## Outcome

A new listener with a folder of audiobook files is listening within a few minutes, with books that look right, and
without having to understand folders.

## Who and when

The first minutes after installing, and every time new books are added. Most listeners aren't technical: "the majority
of our users are people who just randomly throw in audiobook files and they just want to play it." "For a lot of users
the concept of folders is strange even." ([#3044])

## Opportunities

### "I don't know which option to pick, and my books come out wrong"

They end up with one giant book, or one book split into its CDs. This is the biggest topic in this area ([#809],
[#1365]): "I was confused when adding files the first time I used the app, and just messed around with folder layout
until it worked." ([#1365])

- ✅ **Three ways to find books** (every folder is a book, one book, folders are authors), plus adding a single file.
- ✅ **Voice guesses the layout** ("Looks like yours") and shows the books it found before adding them. You can change
  the layout later and keep your progress ([#3803], [#3810]). *(next release)*
- ❌ **More modes or templates**: "I am very hesitant on adding another way of adding folders because it is already
  quite complex for users to understand the current folder modes right now." ([#3044])
- 💡 **Author, series and book as three levels** ([#3044]).

Assumptions:

- **The guess is right for most libraries.** This is untested. Analytics were added partly for this: "I have the
  feeling that our onboarding and the folder modes are highly confusing to users." ([#3212])

### "Voice can't see my files"

The folder button does nothing, or the folder looks empty. There are about 15 issues like this, for example "nothing
happens, no dialogue" ([#2847]).

- ✅ **Android's own folder picker**, since 2022.
- ✅ **A "Permission required" card** for a system bug on some devices ([#1952], 2023).
- Most remaining cases are outside Voice: the system Files app is turned off, Family Link blocks it, or another file
  manager gets in the way.

### "My new books don't show up"

"the initial scan works fine, it just never rescans and I can't find any way to manual rescan." ([#892])

- ✅ **Automatic scanning**, with its progress shown in the search field ([#3831]). *(next release)*
- ✅ **Files that change size are read again** ([#3535], 2026).
- ❌ **A rescan button**: "A manual file scan does not fit in how the player works." ([#555])

### "The names are wrong"

"its categorizing them all as Dune Saga because thats the Album tag." ([#433]) "It is absolutely the biggest flaw with
the app right now" ([#781]). Garbled Cyrillic tags are another version of this ([#2421]).

- ✅ **Books with many files use the folder name** ([#3455], 2026).
- ✅ **Genre, narrator and series are read from the tags** ([#3054], 2025).
- ✅ **Renaming a book** (see [Library](library.md)).
- 💡 **Use file and folder names instead of tags** ([#3502]). With its duplicates, this has 17 upvotes.

### "My chapters are missing, or my big book doesn't show up"

Chapters inside m4b files ([#3062]) and very large files ([#3672]).

- ✅ **Chapter reading was rewritten** ([#2865], 2025).
- ✅ **Large m4b files can be added** ([#3799]), but playing them still fails ([#3672]). *(next release)*

"Media parsing is ugly" ([#3044]). The bar is high: during one regression, "I'm pinning this issue and will stop the
rollout until this is fixed." ([#2840])

### "I moved or renamed a folder and lost my progress and bookmarks"

"If I move the directory the audio file is in, these bookmarks are lost!" ([#2636], a confirmed bug that's still open.)
The cause: "Voice uses the path to identify books from each other." ([#2678])

- ✅ **Changing a folder's layout keeps your progress** ([#3803]). *(next release)*
- 💡 **Recognize a book after it moved or was renamed** ([#2636], [#3221]).

Backup and sync without a server have the same root problem. See [Servers, sync and backup](servers-and-sync.md).

### Onboarding

Listeners rarely ask for this. The push came from the maintainer: "Right now the user is thrown into cold water and I
assume that mostly users with a technical background understand how to add folders." ([#1365], 2018)

- ✅ **A short onboarding** ([#1987], 2023) that asks about analytics, with analytics off unless you agree ([#3212]).
- ✅ **"Connect Audiobookshelf"** as an option instead of picking a folder.

## Open questions

1. How many listeners use each layout, and how often do they change the guess? Analytics can tell.
2. Should Voice recognize a book after it moves? The same problem blocks renaming files ([#2636]), backup ([#1668])
   and syncing without a server ([#1315]).
3. Tags or file names ([#3502]): a setting, or a smarter default?

[#555]: https://github.com/PaulWoitaschek/Voice/issues/555
[#433]: https://github.com/PaulWoitaschek/Voice/issues/433
[#781]: https://github.com/PaulWoitaschek/Voice/issues/781
[#809]: https://github.com/PaulWoitaschek/Voice/issues/809
[#892]: https://github.com/PaulWoitaschek/Voice/issues/892
[#1365]: https://github.com/PaulWoitaschek/Voice/discussions/1365
[#1315]: https://github.com/PaulWoitaschek/Voice/discussions/1315
[#1668]: https://github.com/PaulWoitaschek/Voice/discussions/1668
[#1952]: https://github.com/PaulWoitaschek/Voice/pull/1952
[#1987]: https://github.com/PaulWoitaschek/Voice/pull/1987
[#2421]: https://github.com/PaulWoitaschek/Voice/issues/2421
[#2636]: https://github.com/PaulWoitaschek/Voice/issues/2636
[#2678]: https://github.com/PaulWoitaschek/Voice/discussions/2678
[#2840]: https://github.com/PaulWoitaschek/Voice/issues/2840
[#2847]: https://github.com/PaulWoitaschek/Voice/issues/2847
[#2865]: https://github.com/PaulWoitaschek/Voice/pull/2865
[#3044]: https://github.com/PaulWoitaschek/Voice/issues/3044
[#3054]: https://github.com/PaulWoitaschek/Voice/pull/3054
[#3062]: https://github.com/PaulWoitaschek/Voice/issues/3062
[#3212]: https://github.com/PaulWoitaschek/Voice/pull/3212
[#3221]: https://github.com/PaulWoitaschek/Voice/discussions/3221
[#3455]: https://github.com/PaulWoitaschek/Voice/pull/3455
[#3502]: https://github.com/PaulWoitaschek/Voice/discussions/3502
[#3535]: https://github.com/PaulWoitaschek/Voice/pull/3535
[#3672]: https://github.com/PaulWoitaschek/Voice/issues/3672
[#3799]: https://github.com/PaulWoitaschek/Voice/pull/3799
[#3803]: https://github.com/PaulWoitaschek/Voice/pull/3803
[#3810]: https://github.com/PaulWoitaschek/Voice/pull/3810
[#3831]: https://github.com/PaulWoitaschek/Voice/pull/3831
