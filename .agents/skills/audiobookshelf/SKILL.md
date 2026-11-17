---
name: audiobookshelf
description: Running a real Audiobookshelf server in Docker on this Mac, with the Voice test books, to check Voice's server support against it. Every worktree gets its own server. Use when asked to test, reproduce or check something with Audiobookshelf or a server, like signing in, libraries, streaming, downloads, progress or bookmark sync, edits, an old server version, or what happens when the server is away.
---

# A local Audiobookshelf server

`.agents/skills/audiobookshelf/scripts/audiobookshelf.py` runs [Audiobookshelf](https://www.audiobookshelf.org) in
Docker, one container per worktree. You may start, change and remove your own server at any time without asking.
Never touch another container. Run the script from the repository root.

It needs the Docker daemon. When the script says it isn't running, ask the user to start Docker Desktop, or run
`open -a Docker` and try again once it's up. It doesn't need anything else: the image comes from `ghcr.io`, and the
test books come from `scripts/populate_test_files.py`, which needs `ffmpeg`.

How Voice works with a server is in `behavior/audiobookshelf.md` of the voice-strategy repository. The code is in
`core/audiobookshelf` and `features/audiobookshelf`.

## Start

```sh
.agents/skills/audiobookshelf/scripts/audiobookshelf.py start
```

- The first start takes a few seconds. It copies the six test books to `build/audiobookshelf/audiobooks`, starts
  Audiobookshelf 2.37.2 on a free port of `127.0.0.1`, creates the accounts and the library "Audiobooks", and waits
  for the scan. All six books have covers, four have chapters.
- It prints the address for this Mac and for the emulator (`http://10.0.2.2:<port>`, the emulator's name for the
  Mac). The port changes when the container starts again, so take it from `status`.
- `start` again keeps the server and its state. `stop` removes the container and all its data, and the next `start`
  gives you a fresh server.
- `start --version 2.20.0` replaces the server with another version, e.g. one older than 2.26 that Voice turns down.
  `start --version 2.37.2` goes back.

| Account | Password | For |
|---|---|---|
| `root` | `voice` | The admin. Signs in by default. |
| `listener` | `voice` | Another account, which can't edit books. |

These are test accounts of this local server only.

## Connect Voice

Lease an emulator with the emulator skill first. Server support is behind the `audiobookshelf` feature flag, which is
off by default. Turn it on in Developer settings, then open the sign-in:

```sh
emulator.py open BookOverview DeveloperSettings
emulator.py open BookOverview '{"type":"AudiobookshelfLogin","origin":"Default"}'
```

Type `10.0.2.2:<port>` as the address. Voice tries `http` first for it, like for a server in the home network.

## Change the server

`api` calls the [server API](https://api.audiobookshelf.org) as the admin and prints the JSON. Use it to set up the
server side of a case, like playing on another device:

```sh
audiobookshelf.py api GET /api/libraries
audiobookshelf.py api GET '/api/libraries/<library id>/items'
audiobookshelf.py api PATCH '/api/me/progress/<item id>' '{"currentTime": 120, "duration": 600}'
audiobookshelf.py api PATCH '/api/me/progress/<item id>' '{"isFinished": true}'
audiobookshelf.py api POST '/api/me/item/<item id>/bookmark' '{"time": 60, "title": "From the server"}'
audiobookshelf.py api PATCH '/api/items/<item id>/media' '{"metadata": {"title": "New title"}}'
audiobookshelf.py api DELETE '/api/items/<item id>'
audiobookshelf.py api POST '/api/libraries' '{"name": "More", "folders": [{"fullPath": "/audiobooks"}], "mediaType": "book"}'
```

- Files: add, change or remove them in `build/audiobookshelf/audiobooks`, then scan with
  `api POST '/api/libraries/<library id>/scan'`.
- The web app at the address for this Mac shows the same server, for a look with the built-in browser.
- The server away: `docker stop <container>`, the name is in `status`. `start` brings it back on a new port, so Voice
  sees another address. To keep the port, use `docker pause <container>` and `docker unpause <container>`.
- `logs` prints the end of the server log.

## Done

Run `audiobookshelf.py stop` when you're done with the task.
