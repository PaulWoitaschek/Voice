---
layout: ../layouts/Prose.astro
title: FAQ
description: Answers to common questions about Voice, the audiobook player for Android, and how it finds the books in your folders.
---

# Questions and answers

## Does Voice support Android Auto?

Yes, Voice fully supports Android Auto!

Android Auto only shows apps installed from a trusted source such as
[Google Play](https://play.google.com/store/apps/details?id=de.ph1b.audiobook). If you installed Voice from F-Droid, GitHub
or IzzyDroid, follow [Google's steps to allow unknown sources](https://developer.android.com/training/cars/testing#unknown-sources)
in Android Auto.

Google Play is the only officially supported source for Android Auto. See [where to get Voice](/#download) for details.

## Why doesn't Voice support the xyz Media Format?

Voice relies on the media formats that are natively supported by the Android platform.

You can review the currently supported file extensions [here](https://developer.android.com/media/media3/exoplayer/supported-formats).
If a file that should be supported is not displayed, it is most likely either corrupted or incompatible with your Android version.

Books from stores that lock them to their own app with DRM, like Audible, can't be played in Voice. Books without DRM work.

## Why isn’t feature xyz available in the app?

I add features when they make listening better for many listeners. Each one has to be maintained for years, and every new setting makes
the app a bit harder to use. So I prefer good defaults over options and keep each screen focused on listening.

If you're missing something, [open a feature request](https://github.com/PaulWoitaschek/Voice/discussions/new?category=ideas) and describe
what you'd like to do.

## Which Voice version should I use on older Android?

If you’re running an Android release that’s not supported by the latest Voice build, pick the version below that matches your OS/API level.
To check your API level, go to **Settings » About » Android version** on your device.

| Android Version | API Level (SDK) | Voice Version                                                           |
|-----------------|-----------------|-------------------------------------------------------------------------|
| Android 9+      | 28+             | Supported in the latest version 🎉                                      |
| Android 8.1     | 27              | [8.2.4‑2](https://github.com/PaulWoitaschek/Voice/releases/tag/8.2.4-2) |
| Android 8       | 26              | [8.2.4‑2](https://github.com/PaulWoitaschek/Voice/releases/tag/8.2.4-2) |
| Android 7.1     | 25              | [8.2.4‑2](https://github.com/PaulWoitaschek/Voice/releases/tag/8.2.4-2) |
| Android 7.0     | 24              | [6.0.10](https://github.com/PaulWoitaschek/Voice/releases/tag/6.0.10)   |

## How do I resume playback after the sleep timer stops?

Before the sleep timer elapses, the sound fades out for a few seconds, then Voice pauses playback. To keep listening,
you have two options:

- **Shake to keep listening**: Shake your device while the sound fades out, or within 30 seconds of the pause. The
  sleep timer starts again.
- **Press play**: Press play within 30 seconds of the pause, in the app, in the notification or on your headphones,
  and the sleep timer starts again. If you press play later, Voice keeps playing without a sleep timer.

On some devices (e.g. Samsung S20fe) shake-to-resume may not work reliably.

## How do I continue a book on another device?

Voice keeps your place, your playback speed, your bookmarks and the titles and authors you gave your books in a
hidden `.voice` folder inside each audiobook folder you added. Sync these folders between your devices with a tool of
your own, like [Syncthing](https://syncthing.net), and when you open Voice on the other device, it picks up where you
left off.

- Add the same folder on every device, and the same way, for example as "Every folder is a book" on both. Where the
  folder is on each device doesn't matter.
- The folder has to be on the device. Folders from cloud apps like Google Drive don't sync.
- The audio files of a book have to be the same on every device.
- Books you added as a single file don't sync. Add the folder they're in instead.
- Your sync tool has to copy hidden folders.
- A book that is playing stays where it is.

If you share a folder with your family and don't want each other's place, names and bookmarks, let your sync tool
ignore `.voice`. In Syncthing, add `.voice` to the folder's ignore patterns.

<h2 id="folders">How does Voice find the books in my folder?</h2>

Pick the folder with your audiobooks. Voice looks at how the files inside are organized and shows you the books it found before
adding them. If the books don't look right, tap **Change how books are found** and pick one of the three options below. You can
change this later in the settings under **Audiobook folders** by tapping the folder. Your progress and bookmarks stay with your books.

### Every folder is a book

Each folder within your selected folder is a separate audiobook. Any audio files directly inside the selected folder (not in a
subfolder) are audiobooks as well.

```
/Audiobooks
├─ TheHobbit/
│   ├─ chapter1.mp3
│   └─ chapter2.mp3
├─ MobyDick/
│   ├─ chapter1.mp3
│   └─ chapter2.mp3
└─ LittlePrince.mp3
```

Voice recognizes three audiobooks: `TheHobbit`, `MobyDick`, and the single-file book `LittlePrince`.

### One book

The selected folder itself is one audiobook, with files inside treated as chapters. This includes files in subfolders, such as
`CD 1` and `CD 2`.

```
/PrideAndPrejudice
├─ Chapter1.mp3
├─ Chapter2.mp3
└─ Chapter3.mp3
```

Voice recognizes one audiobook: `PrideAndPrejudice`.

### Folders are authors

First-level folders represent authors (or series), and their subfolders represent audiobooks.

```
/Authors
├─ Woolf/
│   ├─ MrsDalloway/
│   │   ├─ chapter1.mp3
│   │   └─ chapter2.mp3
│   └─ ToTheLighthouse/
│       ├─ chapter1.mp3
│       └─ chapter2.mp3
└─ Tolkien/
    └─ TheHobbit/
        ├─ chapter1.mp3
        └─ chapter2.mp3
```

Voice recognizes authors (`Woolf`, `Tolkien`) and their audiobooks (`MrsDalloway`, `ToTheLighthouse`, `TheHobbit`).

## Didn't find an answer?

Ask your question in [Discussions](https://github.com/PaulWoitaschek/Voice/discussions/categories/q-a).
