# Servers, sync and backup

Listeners have their books, progress and bookmarks on all their devices, and keep them when they change phones. There's
no Voice account and no Voice server.

## Solved

- **There's no good audiobook app for my server.** Audiobookshelf: stream or download books, with progress and
  bookmarks kept in step ([docs](../docs/audiobookshelf.md)).
- **My books in cloud storage can't be added.** Folders from apps that work with Android's folder picker. Syncing the
  folder to the phone works better.
- **I lose my place when I switch devices.** With Audiobookshelf, or by syncing the library folders with a tool like
  Syncthing. Voice keeps places, speed, names and bookmarks in a hidden `.voice` folder there ([FAQ](../docs/faq.md)).
- **A new phone means losing my progress.** Android's backup, or the synced library folders. After a restore, the
  library asks to allow access to the folders again.

## Open

- **Books on Jellyfin, Plex or a web folder can't be played.** [#2385]
- **A cloud library has to be copied to the phone.** [#1294]
- **Without Google's backup, a new phone loses my settings and history.** [#1668]

## Won't solve

- **Network shares (SMB):** too specific to build a custom solution for ([#450]).
- **Backing up the database:** a raw copy breaks easily when its schema changes ([#2302]).
- **A switch for folder sync:** sync tools can already leave `.voice` out.

[#450]: https://github.com/PaulWoitaschek/Voice/issues/450
[#1294]: https://github.com/PaulWoitaschek/Voice/discussions/1294
[#1668]: https://github.com/PaulWoitaschek/Voice/discussions/1668
[#2302]: https://github.com/PaulWoitaschek/Voice/pull/2302
[#2385]: https://github.com/PaulWoitaschek/Voice/discussions/2385
