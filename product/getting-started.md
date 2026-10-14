# Getting started

**Status:** Outline.

A new listener with a folder of audiobook files is listening within a few minutes, with books that look right, and
without having to understand folders.

**When:** The first minutes after installing, and every time new books are added. Most listeners aren't technical. They
add audiobook files without a plan and just want to play them, and for many, folders are a strange idea ([#3044]).

| Problem | Status |
|---|---|
| [My books come out wrong when I add a folder](#my-books-come-out-wrong-when-i-add-a-folder) | ✅ Solved |
| [My author, series and book folders fit no layout](#my-author-series-and-book-folders-fit-no-layout) | ⭕ Open |
| [Voice can't see my files](#voice-cant-see-my-files) | ✅ Solved |
| [My new books don't show up](#my-new-books-dont-show-up) | ✅ Solved |
| [My books have the wrong names](#my-books-have-the-wrong-names) | ✅ Solved |
| [Fixing wrong tags means renaming every book by hand](#fixing-wrong-tags-means-renaming-every-book-by-hand) | ⭕ Open |
| [My chapters are missing, or my big book doesn't show up](#my-chapters-are-missing-or-my-big-book-doesnt-show-up) | ✅ Solved |
| [Moving or renaming a folder loses my progress and bookmarks](#moving-or-renaming-a-folder-loses-my-progress-and-bookmarks) | ✅ Solved |
| [After installing, I don't know where to start](#after-installing-i-dont-know-where-to-start) | ✅ Solved |

## My books come out wrong when I add a folder

✅ **Solved** · [#1365]

Listeners don't know which option to pick. They end up with one giant book, or one book split into its CDs. This is the
biggest topic in this area. One listener was confused the first time and moved folders around until it worked.

**Solution:**

- Three ways to find books: every folder is a book, one book, or folders are authors. Plus adding a single file.
- Voice guesses the layout ("Looks like yours") and shows the books it found before adding them. You can change the
  layout later and keep your progress.

**Assumption:** The guess is right for most libraries. This is untested. Analytics were added partly to find out,
because the maintainer suspected that the onboarding and the folder layouts confuse listeners ([#3212]).

## My author, series and book folders fit no layout

⭕ **Open** · [#3044]

Listeners who sort their folders by author, then series, then book find that none of the layouts fits.

**Turned down:**

- More layouts or templates: the layouts Voice has are already hard for listeners to understand ([#3044]).

## Voice can't see my files

✅ **Solved** · [#2847]

The folder button does nothing, or the folder looks empty. Most of the cases left are outside Voice: the system Files
app is turned off, Family Link blocks it, or another file manager gets in the way.

**Solution:**

- Android's own folder picker, since 2022.
- A "Permission required" card for a system bug on some devices.

## My new books don't show up

✅ **Solved** · [#892]

The first scan works, but books added after it never appear, and there's no way to scan again.

**Solution:**

- Voice scans automatically and shows its progress in the search field.
- Files that change size are read again.

**Turned down:**

- A rescan button: a manual scan doesn't fit how the player works ([#555]).

## My books have the wrong names

✅ **Solved** · [#433]

One listener's books were all named "Dune Saga", because that was their album tag. Another called wrong names the
biggest flaw of the app ([#781]). Garbled Cyrillic tags are another version of this.

**Solution:**

- Books with many files use the folder name.
- Genre, narrator and series are read from the tags.
- Renaming a book (see [Library](library.md)).

## Fixing wrong tags means renaming every book by hand

⭕ **Open** · [#3502]

When the tags are wrong, each book has to be renamed by hand.

## My chapters are missing, or my big book doesn't show up

✅ **Solved**

Chapters inside m4b files go missing, and very large files don't show up. Reading media files is messy ([#3044]). The
bar is high: during one regression, the maintainer pinned the issue and stopped the rollout until it was fixed
([#2840]).

**Solution:**

- Chapters inside m4b files show up, since chapter reading was rewritten.
- Large m4b files can be added.

## Moving or renaming a folder loses my progress and bookmarks

✅ **Solved** · [#2636]

Voice told books apart by their path ([#2678]). Changing a folder's layout kept the progress, but moving or renaming the
folder lost it, and the bookmarks too.

**Solution:** A book that shows up under a new path with the same files keeps its progress, bookmarks and history
([#3911]). Files are recognized by their name and size, in folders on the device. The same recognition takes the place
to other devices, see [Servers, sync and backup](servers-and-sync.md).

## After installing, I don't know where to start

✅ **Solved** · [#1365]

Listeners rarely ask for this. The push came from the maintainer in 2018: new listeners were thrown in at the deep end,
and probably only the technical ones understood how to add folders.

**Solution:**

- A short onboarding that asks about analytics. Analytics are off unless you agree.
- "Connect Audiobookshelf" as an option instead of picking a folder.

## Open questions

1. How many listeners use each layout, and how often do they change the guess? Analytics can tell.
2. Tags or file names: a setting, or a smarter default?

[#433]: https://github.com/PaulWoitaschek/Voice/issues/433
[#555]: https://github.com/PaulWoitaschek/Voice/issues/555
[#781]: https://github.com/PaulWoitaschek/Voice/issues/781
[#892]: https://github.com/PaulWoitaschek/Voice/issues/892
[#1365]: https://github.com/PaulWoitaschek/Voice/discussions/1365
[#2636]: https://github.com/PaulWoitaschek/Voice/issues/2636
[#2678]: https://github.com/PaulWoitaschek/Voice/discussions/2678
[#2840]: https://github.com/PaulWoitaschek/Voice/issues/2840
[#2847]: https://github.com/PaulWoitaschek/Voice/issues/2847
[#3044]: https://github.com/PaulWoitaschek/Voice/issues/3044
[#3212]: https://github.com/PaulWoitaschek/Voice/pull/3212
[#3502]: https://github.com/PaulWoitaschek/Voice/discussions/3502
[#3911]: https://github.com/PaulWoitaschek/Voice/pull/3911
