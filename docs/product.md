# Product spec

Listeners press play on their own audiobooks, stay in the story, and never lose their place.

Most of them drop in their audiobook files and just want them to play ([#3162]). Power users matter too, as long as the
basics stay simple ([#1034]).

## Principles

- **Clean, not minimal.** A feature has to make listening better for many listeners, because each one is maintained for
  years ([FAQ](faq.md)).
- **Good defaults before settings.** Every setting makes Voice a bit harder to use ([#860]).
- **The data stays with the listener.** No account, no ads, nothing behind a paywall, and no Voice server.
- **Never interrupt listening.** Prompts wait for a calm moment, ask once, and accept no.
- **Reliable before new.** A feature that breaks listening waits until it has tests.

Decisions follow upvotes on ideas ([#1330]) and opt-in analytics in the Play build ([#3212]). Voice is neither a podcast
or music player ([#970]) nor an app for other platforms than Android ([#83]).

## Keeping this spec

Each area lists the problems listeners have, under **Solved**, **Planned**, **Open** and **Won't solve**.

- Write the problem the way a listener would say it, and what Voice does about it in a sentence or two.
- Link the one issue or discussion that matters, for open problems and decisions. Put the links below their area.
- Only add something under **Won't solve** if it would otherwise be proposed again, and give the reason.
- Leave out history, releases, counts, bugs, one-off opinions and how it's built.
- Update the spec in the pull request that changes what it describes.

[#83]: https://github.com/PaulWoitaschek/Voice/issues/83
[#860]: https://github.com/PaulWoitaschek/Voice/issues/860
[#970]: https://github.com/PaulWoitaschek/Voice/issues/970
[#1034]: https://github.com/PaulWoitaschek/Voice/issues/1034
[#1330]: https://github.com/PaulWoitaschek/Voice/discussions/1330
[#3162]: https://github.com/PaulWoitaschek/Voice/discussions/3162
[#3212]: https://github.com/PaulWoitaschek/Voice/pull/3212

## Getting started

A new listener adds a folder of audiobooks and is listening within minutes, with books that look right.

**Solved**

- **My books come out wrong when I add a folder.** Voice guesses how the folder is laid out and shows the books it
  found before adding them. The layout can be changed later without losing progress.
- **Voice can't see my files.** Android's folder picker. When Voice loses access to a folder, the library asks to allow
  it again.
- **My new books don't show up.** Voice scans on its own and reads changed files again.
- **Moving or renaming a folder loses my progress.** Voice recognizes a book by its files, so progress and bookmarks
  stay.

**Planned**, see the [library spec](library.md)

- **My author, series and book folders fit no layout.** With "Folders are authors", the folder is the author when the
  tags have none, and a folder of books in it is a series. [#3162]

**Open**

- **Wrong tags mean renaming every book by hand.** [#3502]

**Won't solve**

- **More folder layouts:** the ones Voice has are already hard to understand ([#3162]).
- **A rescan button:** Voice scans on its own ([#555]).

[#555]: https://github.com/PaulWoitaschek/Voice/issues/555
[#3502]: https://github.com/PaulWoitaschek/Voice/discussions/3502

## Library

Listeners find the book they want in seconds and see how far along they are in each one.

**Solved**

- **I have to look for the book I'm listening to.** Continue listening sits at the top, then Current, Not started and
  Completed.
- **Many books in progress take long to scroll through.** Current is a list or grid, like the other sections.
- **I can't see how far along I am.** Every book shows its progress.
- **Finding a book takes too long.** Search finds titles, authors, narrators, series and genres, and can browse by each.
- **A book's name or cover is wrong.** Rename a book, or pick a cover from a file or the internet.
- **Deleting a book can delete more than I meant.** Voice shows which files go and that they can't be brought back.

**Planned**, see the [library spec](library.md)

- **The books of a series aren't grouped or in order.** A series becomes one stack, its books are in order, and Voice
  offers the next one when a book ends. [#1748]
- **I can't put my books in the order I want.** Sort by last listened, title, author or recently added. [#1543]
- **Fixing the author means editing the files.** Edit the title, author, series and part in Voice. [#1407]
- **Removing a book from the library deletes its files.** Hide a book or a series and keep its files. [#3397]
- **The episodes of a radio play all have the album's name.** Each episode keeps its own title, the album becomes the
  series, and search finds an episode by its number. [#3132]

**Open**

- **Covers that aren't square get cropped.** [#2856]
- **A cover I pick isn't saved with the book's files.** [#2303]
- **I can't see how much I've listened.** [#2826]

[#1407]: https://github.com/PaulWoitaschek/Voice/discussions/1407
[#1543]: https://github.com/PaulWoitaschek/Voice/discussions/1543
[#1748]: https://github.com/PaulWoitaschek/Voice/discussions/1748
[#2303]: https://github.com/PaulWoitaschek/Voice/discussions/2303
[#3132]: https://github.com/PaulWoitaschek/Voice/issues/3132
[#2826]: https://github.com/PaulWoitaschek/Voice/discussions/2826
[#2856]: https://github.com/PaulWoitaschek/Voice/discussions/2856
[#3397]: https://github.com/PaulWoitaschek/Voice/discussions/3397

## Player

Listeners press play, follow the story at their own pace, and never lose their place.

**Solved**

- **Playback stops on its own.** Voice asks to lift battery limits and keeps the phone awake while playing.
- **I lose the thread after a pause.** Auto rewind goes back a little when playback pauses, 2 seconds by default.
- **I have to set the speed again for every book.** Speed is saved per book, and a new book starts at the speed
  listened at last.
- **I can't tell how much of the book is left.** The player shows the progress and the time left at the current speed.
- **One slip loses my place.** After a big seek, a chapter change or going to a bookmark, the jump-back pill takes the
  listener back.
- **A book is too quiet or full of pauses.** Volume boost and skip silence, saved per book.
- **Calls and navigation prompts talk over my book.** Voice pauses and carries on afterwards.

**Open**

- **After a call or a navigation prompt, I miss what was said.** [#1368]
- **A stray touch still skips or seeks.** [#2195]
- **Getting to an exact time takes many skips.** [#1901]
- **One skip amount doesn't suit both directions.** [#1670]
- **Times in different places disagree**, because not all of them follow the speed. [#3226]
- **Skip silence clips words.** [#2252]
- **Some books still sound wrong**: shrill, uneven, or meant for two ears. [#2292]
- **I can't play music under my book.** [#2259]

**Won't solve**

- **A default speed setting:** starting a new book at the last speed needs no setting ([#1790]).

[#1368]: https://github.com/PaulWoitaschek/Voice/discussions/1368
[#1670]: https://github.com/PaulWoitaschek/Voice/discussions/1670
[#1790]: https://github.com/PaulWoitaschek/Voice/discussions/1790
[#1901]: https://github.com/PaulWoitaschek/Voice/discussions/1901
[#2195]: https://github.com/PaulWoitaschek/Voice/discussions/2195
[#2252]: https://github.com/PaulWoitaschek/Voice/discussions/2252
[#2259]: https://github.com/PaulWoitaschek/Voice/discussions/2259
[#2292]: https://github.com/PaulWoitaschek/Voice/discussions/2292
[#3226]: https://github.com/PaulWoitaschek/Voice/discussions/3226

## Sleep timer

Listeners fall asleep to a book and find their place the next night.

**Solved**

- **The book keeps playing after I fall asleep.** A timer from the moon button, in minutes or to the end of the chapter.
  It only counts listening time.
- **I forget to set the timer.** The automatic sleep timer starts one when playback begins at night.
- **The stop wakes me up.** The last 10 seconds fade out, and Voice rewinds a little.
- **The timer stops while I'm still awake.** Shaking the phone within 30 seconds of the stop starts the timer again.
- **I don't know where I fell asleep.** The night shows in the listening history, with what the listener last did before
  the timer stopped.

**Open**

- **With short chapters, end of chapter stops too soon.** [#1421]
- **Pressing play after the timer stopped plays on without a timer.** [#2233]
- **Setting the timer means opening the app.** [#3410]

**Won't solve**

- **A bookmark for every night:** bookmarks are what the listener saved, and the night is in the history ([#3903]).
- **A new timer on every play, at any time of day:** too specific for everyone ([#535]).

[#535]: https://github.com/PaulWoitaschek/Voice/issues/535
[#1421]: https://github.com/PaulWoitaschek/Voice/discussions/1421
[#2233]: https://github.com/PaulWoitaschek/Voice/discussions/2233
[#3410]: https://github.com/PaulWoitaschek/Voice/discussions/3410
[#3903]: https://github.com/PaulWoitaschek/Voice/pull/3903

## Bookmarks and history

Listeners keep the moments that matter and can always get back to where they were. Bookmarks are only what the
listener saved. Everything Voice records on its own goes into the history.

**Solved**

- **Saving a moment means stopping.** A long-press on the bookmark button saves right away. A kind or a note can follow.
- **I can't find the bookmark I'm looking for.** Bookmarks are newest first, or in story order with chapter headers,
  and can be filtered by kind.
- **A tap or a seek loses my place.** The listening history shows what happened, newest first: plays, pauses, the
  sleep timer and every big jump. Every place in it is a way back. It's always on.

**Open**

- **Saving a moment means taking out the phone.** [#2983]
- **My notes can't be read outside Voice.** [#2549]

**Won't solve**

- **Import and export:** too much work for what most listeners get out of it ([#367]).

[#367]: https://github.com/PaulWoitaschek/Voice/issues/367
[#2549]: https://github.com/PaulWoitaschek/Voice/discussions/2549
[#2983]: https://github.com/PaulWoitaschek/Voice/discussions/2983

## Outside the app

Listeners control their book from headphones, the lock screen, the car and the home screen, without opening Voice.

**Solved**

- **The car doesn't remember my book.** Android Auto has the current book ready when it connects. Builds from outside
  Google Play only show up once the listener allows unknown sources in Android Auto.
- **The car's skip buttons look like track buttons.** Rewind and fast forward have skip icons.
- **Headphone and car buttons lose my place.** Next and previous skip by the skip amount everywhere outside the app.
- **The lock screen slider loses my place.** It only covers the current chapter, and a setting turns it off.
- **My book isn't on my home screen.** A Now playing widget, down to 1×1, and a Shelf widget with the books in
  progress. As space runs out, Now playing keeps play first, then the cover, the title, rewind and fast forward.
- **Automations have to open Voice.** Tasker can play, pause, skip and start a book.

**Open**

- **I can't change the speed in the car.** [#1878]
- **I can't play my book on a speaker or TV.** [#1317]
- **I can't listen on my watch without my phone.** [#1749]

**Won't solve**

- **Next and previous change the chapter:** seeking matters far more than chapters ([#3846]).
- **Choosing what each headphone press does:** too specific ([#456]).
- **Jumping to a chapter by voice:** a misheard request could lose the listener's place ([#231]).
- **Controlling Voice through the phone's Assistant:** only players on Google's allowlist can be controlled ([#1071]).

[#231]: https://github.com/PaulWoitaschek/Voice/issues/231
[#456]: https://github.com/PaulWoitaschek/Voice/issues/456
[#1071]: https://github.com/PaulWoitaschek/Voice/issues/1071
[#1317]: https://github.com/PaulWoitaschek/Voice/discussions/1317
[#1749]: https://github.com/PaulWoitaschek/Voice/discussions/1749
[#1878]: https://github.com/PaulWoitaschek/Voice/discussions/1878
[#3846]: https://github.com/PaulWoitaschek/Voice/issues/3846

## Servers, sync and backup

Listeners have their books, progress and bookmarks on all their devices, and keep them when they change phones. There's
no Voice account and no Voice server.

**Solved**

- **There's no good audiobook app for my server.** Audiobookshelf: stream or download books, with progress and
  bookmarks kept in step ([docs](audiobookshelf.md)).
- **My books in cloud storage can't be added.** Folders from apps that work with Android's folder picker. Syncing the
  folder to the phone works better.
- **I lose my place when I switch devices.** With Audiobookshelf, or by syncing the library folders with a tool like
  Syncthing. Voice keeps places, speed, names and bookmarks in a hidden `.voice` folder there ([FAQ](faq.md)).
- **A new phone means losing my progress.** Android's backup, or the synced library folders. After a restore, the
  library asks to allow access to the folders again.

**Open**

- **Books on Jellyfin, Plex or a web folder can't be played.** [#2385]
- **A cloud library has to be copied to the phone.** [#1294]
- **Without Google's backup, a new phone loses my settings and history.** [#1668]

**Won't solve**

- **Network shares (SMB):** too specific to build a custom solution for ([#450]).
- **Backing up the database:** a raw copy breaks easily when its schema changes ([#2302]).
- **A switch for folder sync:** sync tools can already leave `.voice` out.

[#450]: https://github.com/PaulWoitaschek/Voice/issues/450
[#1294]: https://github.com/PaulWoitaschek/Voice/discussions/1294
[#1668]: https://github.com/PaulWoitaschek/Voice/discussions/1668
[#2302]: https://github.com/PaulWoitaschek/Voice/pull/2302
[#2385]: https://github.com/PaulWoitaschek/Voice/discussions/2385

## Look and feel

Voice feels calm day and night and works in the listener's language. Good defaults come before options, see the
[design principles](https://github.com/PaulWoitaschek/Voice/blob/main/.agents/skills/design/SKILL.md).

**Solved**

- **I want Voice dark or light, whatever my phone does.** Light, dark or follow the system.
- **Dark mode isn't black enough for bed.** A pure black switch for the dark theme.
- **The colors don't match my taste or my book.** Voice blue, dynamic color or one of six palettes. The player takes
  its colors from the cover.
- **Large fonts or a small window break the layout.** Layouts follow large fonts and right-to-left languages, and the
  play controls stay on screen in a short split screen.
- **Voice isn't in my language.** A language is added when a listener asks for it.

**Open**

- **Voice may be hard to use with a screen reader.** Nobody has checked it with TalkBack.

## Feedback and support

Listeners who are stuck get help, and listeners who want to give back can, without it getting in the way of listening.

**Solved**

- **There's no way to give something back.** Support in Settings: monthly, yearly or once in the Play build, and Ko-fi
  in the free build. It unlocks nothing, and nothing is behind a paywall.
- **I'm stuck and don't know where to get help.** Settings has the FAQ, getting help, reporting a problem and suggesting
  an idea.
- **I don't want an audiobook app talking to the internet.** The free build has no Google libraries. The Play build
  sends crash reports, and analytics only with consent. The [privacy policy](privacy-policy.md) lists what each build
  sends.
- **I don't know which version to install.** A page compares the [download sources](download-sources.md).

**Open**

- **Getting help needs a GitHub account.** [#1711]

[#1711]: https://github.com/PaulWoitaschek/Voice/discussions/1711
