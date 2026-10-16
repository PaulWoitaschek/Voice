# Servers, sync and backup

**Status:** Outline.

Listeners have their books, progress and bookmarks wherever they need them: on any of their devices, and safe when they
change phones.

**When:** Listeners with a home server (Audiobookshelf, Jellyfin, Plex), or with their books in cloud storage (Drive,
Dropbox, Nextcloud). Listeners with more than one device, or who change or reset their phone. Every solution has the
same limit: there's still no Voice account and no Voice server, and Voice only talks to the listener's own server
([#3863]).

| Problem | Status |
|---|---|
| [There's no good audiobook app for my server](#theres-no-good-audiobook-app-for-my-server) | ✅ Solved |
| [Books on Jellyfin, Plex or a web folder can't be played](#books-on-jellyfin-plex-or-a-web-folder-cant-be-played) | ⭕ Open |
| [A server without password login can't be connected](#a-server-without-password-login-cant-be-connected) | ⭕ Open |
| [My books in cloud storage can't be added](#my-books-in-cloud-storage-cant-be-added) | ✅ Solved |
| [A cloud library has to be copied to the phone](#a-cloud-library-has-to-be-copied-to-the-phone) | ⭕ Open |
| [I lose my place when I switch devices](#i-lose-my-place-when-i-switch-devices) | ✅ Solved |
| [Without a server, my place doesn't follow me to another device](#without-a-server-my-place-doesnt-follow-me-to-another-device) | ✅ Solved |
| [Without a server, my bookmarks don't follow me to another device](#without-a-server-my-bookmarks-dont-follow-me-to-another-device) | ✅ Solved |
| [A new phone means losing my progress and bookmarks](#a-new-phone-means-losing-my-progress-and-bookmarks) | ✅ Solved |
| [Without Google's backup, a new phone means starting over](#without-googles-backup-a-new-phone-means-starting-over) | ⭕ Open |

Keeping the progress after renaming or moving a folder is in [Getting started](getting-started.md). Voice recognizes a
book by its files there, and on other devices too.

## There's no good audiobook app for my server

✅ **Solved** · [#2385]

Listeners keep their books on their own server and want an audiobook app for them, not a media player. Audiobookshelf
had no good native app.

**Solution:** Audiobookshelf. You can stream, or download for offline use (over Wi-Fi by default), and your progress
and bookmarks stay in step ([docs](../docs/audiobookshelf.md)).

**Assumption:** Audiobookshelf covers most people who run a server. Guess: the reason it was picked first was never
written down.

**Assumption:** Seeing a book twice is acceptable when it's both on the device and on the server ([#3866]).

## Books on Jellyfin, Plex or a web folder can't be played

⭕ **Open** · [#2385]

Voice only connects to Audiobookshelf. Jellyfin plays audiobooks, but it doesn't feel like an app made for them
([#1715]).

## A server without password login can't be connected

⭕ **Open** · [#3866]

Voice only logs in with a password, not with OpenID. Servers that only allow single sign-on can't be connected.

## My books in cloud storage can't be added

✅ **Solved** · [#1294]

Listeners keep their books in Drive, Dropbox or Nextcloud, and asked for a way to play them since 2015. Without one,
Voice is no use to them.

**Solution:** Folders from any app that works with Android's folder picker. This is slow and has gaps, so the advice is
to sync the folder to the phone and add that instead ([#2803]).

**Turned down:**

- Network shares (SMB): too specific to build a custom solution for ([#450]).

## A cloud library has to be copied to the phone

⭕ **Open** · [#1294]

Playing straight from the cloud is slow and has gaps, so the whole library has to be on the phone. Listeners want to
keep their collection in the cloud and keep only the couple of books they're listening to on the phone for offline use.

## I lose my place when I switch devices

✅ **Solved** · [#1315]

Listeners switch from phone to tablet and want Voice to know where they were. They have asked for this since 2015.

**Solution:** For Audiobookshelf books, the newest position wins, and the jump-back pill takes you to where you were on
this device.

## Without a server, my place doesn't follow me to another device

✅ **Solved** · [#3707]

Listeners without a server asked for an optional sync that stays local.

**Solution:** Each device keeps its places, with the speed and the names listeners gave their books, in its own file
in a hidden `.voice` folder inside each library folder. Listeners sync their library folders with a tool of their own,
like Syncthing. When Voice comes to the foreground or the library changes, it takes over what changed later on another
device, and the jump-back pill takes you to where you were on this device. A book is recognized by the names and
sizes of its files, so the folder can be anywhere on each device. This works for folders on the device that are added
the same way on every device, not for single files or folders from other apps. There's no switch: listeners who don't
want a folder's places shared let their sync tool ignore `.voice` ([FAQ](../docs/faq.md)).

**Assumption:** Sync tools copy hidden folders. Guess: Syncthing does by default, the others weren't checked.

**Assumption:** Few listeners share one library folder with someone else. They'd get each other's places, names and
bookmarks unless their sync tool ignores `.voice`. Guess.

**Assumption:** Listeners don't mind the hidden `.voice` folder in their library folders, as there's no way to turn it
off. Guess.

**Turned down:**

- Voice's own sync: in 2015, to keep Voice simple ([#65]). The hard part is telling that two copies are the same book
  ([#295]).
- One file that every device writes: two devices writing it at once would leave conflict copies behind. Each device
  only writes its own file.
- A file next to each book: it would add a file to every book folder.
- A switch in the settings: sync tools can already leave `.voice` out.
- Storing when a name changed in the database: comparing with what the device last wrote into its own file tells the
  same, without a migration.

## Without a server, my bookmarks don't follow me to another device

✅ **Solved** · [#3707]

Listeners who sync their place through their library folders want their bookmarks on every device too.

**Solution:** The same `.voice` files carry the bookmarks of each book. A deleted bookmark stays deleted, and when two
devices changed the same bookmark, the later change wins.

## A new phone means losing my progress and bookmarks

✅ **Solved**

After changing or resetting their phone, listeners find their progress and bookmarks gone.

**Solution:** Android's own backup, since 2022. Android doesn't back up the access to the library folders, so after a
restore the library shows a card until the listener allows access again. The folders screen marks each of these
folders, and the system picker opens where the folder was. Picking the same folder brings its books back as they
were. Picking another one keeps the progress too, as Voice recognizes the books by their files.

Android skips the cloud backup of an app with more than 25 MB, so covers from the files or a server stay out of it.
Voice gets them from there again after a restore. Only the covers listeners pick are backed up. The ones picked in
26.10.2 or earlier were stored with the others, so after a restore the book shows the cover from its files.

## Without Google's backup, a new phone means starting over

⭕ **Open** · [#1668]

Some listeners switch or reset their phone often, and would rather start fresh than back up to Google.

**Turned down:**

- Import and export: too much work for what most listeners get out of it ([#367]). That was in 2016.
- Backing up the database: a raw copy of the database breaks easily, especially when its schema changes ([#2302]).
- Putting settings and the listening history into the `.voice` files: they hold what belongs to a book. The places,
  names and bookmarks in them already come along when the library folders move to the new phone.

**Assumption:** Listeners without Google's backup miss their places and bookmarks, not their settings and listening
history. Guess.

## Open questions

1. Why Audiobookshelf first, and will it stay the only server?

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
