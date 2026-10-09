# Voice with Audiobookshelf

[Audiobookshelf](https://www.audiobookshelf.org) is a self-hosted audiobook server. Connect Voice to it and the books of your server
join your library next to the books on your device. Play them right away, or download them to listen without a connection. Your
position and your bookmarks stay in step with the web app and your other devices.

There is no Voice account and no Voice server in between. Voice only talks to your own server.

## Connecting your server

1. Open **Settings » Library » Audiobookshelf**. During the first start, tap **Connect Audiobookshelf** instead of picking a folder.
2. Type the address of your server, the way you open its web app: `audiobooks.example.com`, `192.168.1.10:13378` or
   `https://example.com/audiobookshelf`. Voice finds the server on its own, with or without `https://`.
3. Sign in with your Audiobookshelf username and password.
4. Pick the libraries you want. Their books show up in your library.

Voice needs Audiobookshelf 2.26 or newer.

## Listening

- **Streaming**: Tap a book and it plays from the server.
- **Downloads**: Long press a book and tap **Download**. Downloads wait for Wi-Fi unless you turn on **Download over mobile data** in the
  Audiobookshelf settings. Remove a download the same way. Voice never deletes anything on your server.
- **Offline**: When Voice can't reach your server, books that aren't downloaded show a crossed out cloud. Downloaded books play as usual.

Speed, skip silence, volume boost, the sleep timer, bookmarks, Android Auto and the widgets work the same as for the books on your
device. Names, authors and covers come from your server, so you edit them there.

## Your progress on every device

Voice sends your position while you listen, and when you pause. When you listened on another device, Voice takes over that position the
next time it syncs: when it starts, when you open the library, and when it gets back online. If the book is open in the player, the
**Jump back** pill takes you back to where you were on this device.

When both sides moved, the newer position wins. What you listened to offline is sent once Voice reaches your server again.

Bookmarks sync in both directions. The bookmarks the sleep timer sets stay on your device.

Your listening shows up in the listening sessions and stats of Audiobookshelf.

## Home servers, VPNs and certificates

Many servers are only reachable at home. Voice works with:

- plain `http://` addresses in your network,
- VPNs like Tailscale or WireGuard,
- reverse proxies with a subfolder, like `https://example.com/audiobookshelf`,
- self-signed certificates, once you install your certificate authority in the Android settings under **Security » Encryption &
  credentials » Install a certificate**.

## FAQ

??? question "Can I sign in with single sign-on (OpenID)?"
    Not yet. If your server only allows single sign-on, ask whoever runs it to also allow signing in with a password for your account.

??? question "Does Voice support podcasts or ebooks from Audiobookshelf?"
    No, Voice is an audiobook player. Podcast libraries don't show up.

??? question "What happens when I sign out?"
    The books of your server and their downloads leave your device. Your progress and bookmarks stay on the server, and come back when you
    sign in again.

??? question "I already download my books into a folder that Voice scans."
    Then those books show up twice. Remove that folder under **Settings » Library » Audiobook folders** to keep the server books only.
