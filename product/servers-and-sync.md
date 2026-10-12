# Servers, sync and backup

**Status:** Outline.

## Outcome

Listeners have their books, progress and bookmarks wherever they need them: on any of their devices, and safe when they
change phones. There's no Voice account and no Voice server.

## Who and when

- Listeners with a home server (Audiobookshelf, Jellyfin, Plex).
- Listeners whose books live in cloud storage (Drive, Dropbox, Nextcloud).
- Listeners with more than one device, or who change or reset their phone.

The limit on every solution: "Still no Voice account and no Voice server: Voice only talks to your own server."
([#3863])

## Opportunities

### "My books are on my own server, and I want an audiobook app, not a media player"

"there are no native good apps for ABS, and this app is awesome" ([#2385]). About Jellyfin: "it is not like using an
audiobook-focused app" ([#1715])

- ✅ **Audiobookshelf.** You can stream, or download for offline use (over Wi-Fi by
  default), and your progress and bookmarks stay in step ([docs](../docs/audiobookshelf.md)).
- ⭕ **Books on Jellyfin, Plex or a plain web folder can't be played** ([#2385]).
- ⭕ **Servers that only allow single sign-on can't be connected**: "Only password login, no OpenID." ([#3866])

Assumptions:

- **Audiobookshelf covers most people who run a server.** **Guess:** the reason it was picked first was never written
  down.
- **Seeing a book twice is acceptable** when it's both on the device and on the server ([#3866]).

### "My books are in Drive, Dropbox or Nextcloud"

Open since 2015: "All my Audiobooks are in my Google Drive folder, so this app was completely unusable for me"
([#1294]). "I wanted a way to keep my whole collection in the cloud then "cache" the couple I'm actively listening to so
I can play them offline." ([#1294])

- ✅ **Folders from any app that works with Android's folder picker.** This is slow and has gaps. The
  advice: "Syncing the folder to your phone and adding that works much better." ([#2803])
- ❌ **Network shares (SMB)**: "This is too specific to add a custom solution for this." ([#450])
- ⭕ **A library in the cloud has to be copied to the phone in full**, or it plays slowly and with gaps.

### "I listen on two devices and lose my place"

Open since 2015: "so I can switch from phone to tablet and have the app simply know where I was" ([#1315])

- ✅ **For Audiobookshelf books**: the newest position wins, and the jump-back pill takes you to where you were on this
  device.
- ❌ **Voice's own sync**, in 2015: "Sry, want to keep it simple." ([#65]) The hard part: "How do we identify the books
  as the same?" ([#295])
- ⭕ **Without a server, progress doesn't follow the listener to a second device**: "would it be possible to have an
  optional, local-only sync?" ([#3707]) One answer: a progress file next to the book, for tools like Syncthing to
  copy.

### "New phone, and my progress and bookmarks are gone"

"I keep switching or resetting my phone all the
time!" "I personally would rather start fresh than backup to Google." ([#1668])

- ✅ **Android's own backup**, since 2022.
- ❌ **Import and export**, in 2016: "An import / export functionality would require too much work with little benefit
  for most of the users." ([#367])
- ❌ **Backing up the database** ([#2302]): "Backing up the pure SQL database is very error prone, especially around
  database scheme updates."
- ⭕ **Without Google's backup, a new phone means starting over.**

### "I renamed or moved a folder and lost everything"

See [Getting started](getting-started.md). "Voice uses the path to identify books from each other." ([#2678])

## Open questions

1. **A book identity that survives moves, new phones and second devices.** Using the path blocks renaming, backup and
   syncing without a server. Is it worth solving once, for all three?
2. Why Audiobookshelf first, and will it stay the only server?
3. A progress file next to the book fits "no Voice server". Is it acceptable?

[#65]: https://github.com/PaulWoitaschek/Voice/issues/65
[#295]: https://github.com/PaulWoitaschek/Voice/issues/295
[#367]: https://github.com/PaulWoitaschek/Voice/issues/367
[#450]: https://github.com/PaulWoitaschek/Voice/issues/450
[#1294]: https://github.com/PaulWoitaschek/Voice/discussions/1294
[#1315]: https://github.com/PaulWoitaschek/Voice/discussions/1315
[#1668]: https://github.com/PaulWoitaschek/Voice/discussions/1668
[#1715]: https://github.com/PaulWoitaschek/Voice/discussions/1715
[#2302]: https://github.com/PaulWoitaschek/Voice/pull/2302
[#2385]: https://github.com/PaulWoitaschek/Voice/discussions/2385
[#2678]: https://github.com/PaulWoitaschek/Voice/discussions/2678
[#2803]: https://github.com/PaulWoitaschek/Voice/issues/2803
[#3707]: https://github.com/PaulWoitaschek/Voice/discussions/3707
[#3863]: https://github.com/PaulWoitaschek/Voice/discussions/3863
[#3866]: https://github.com/PaulWoitaschek/Voice/pull/3866
