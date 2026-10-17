### Does Voice support Android Auto?

Yes, Voice fully supports Android Auto!

Android Auto only shows apps installed from the
[Google Play Store](https://play.google.com/store/apps/details?id=de.ph1b.audiobook). If you installed Voice from F-Droid,
GitHub or IzzyDroid, allow apps from other sources in Android Auto:

1. Open the Android Auto settings. On most phones, search for "Android Auto" in the system settings.
2. Tap **Version** at the bottom 10 times and allow development settings.
3. Open the three-dot menu, tap **Developer settings** and turn on **Unknown sources**.
4. If Voice still doesn't show up, add it under **Customize launcher** in the Android Auto settings.

Google describes the developer settings in
its [Android Auto developer docs](https://developer.android.com/training/cars/testing#developer-mode). Google Play is the
only officially supported source for Android Auto. See the [download source comparison](download-sources.md) for details.

### Why doesn't Voice support the xyz Media Format?

Voice relies on the media formats that are natively supported by the Android platform.

You can review the currently supported file extensions [here](https://developer.android.com/media/media3/exoplayer/supported-formats).
If a file that should be supported is not displayed, it is most likely either corrupted or incompatible with your Android version.

### Why isn’t feature xyz available in the app?

I add features when they make listening better for many listeners. Each one has to be maintained for years, and every new setting makes
the app a bit harder to use. So I prefer good defaults over options and keep each screen focused on listening.

If you're missing something, [open a feature request](https://github.com/PaulWoitaschek/Voice/discussions/new?category=ideas) and describe
what you'd like to do.

### Which Voice version should I use on older Android?

!!! tip

    To check your API level, go to **Settings » About » Android version** on your device.

If you’re running an Android release that’s not supported by the latest Voice build, pick the version below that matches your OS/API level:

| Android Version | API Level (SDK) | Voice Version                                                           |
|-----------------|-----------------|-------------------------------------------------------------------------|
| Android 9+      | 28+             | Supported in the latest version 🎉                                      |
| Android 8.1     | 27              | [8.2.4‑2](https://github.com/PaulWoitaschek/Voice/releases/tag/8.2.4-2) |
| Android 8       | 26              | [8.2.4‑2](https://github.com/PaulWoitaschek/Voice/releases/tag/8.2.4-2) |
| Android 7.1     | 25              | [8.2.4‑2](https://github.com/PaulWoitaschek/Voice/releases/tag/8.2.4-2) |
| Android 7.0     | 24              | [6.0.10](https://github.com/PaulWoitaschek/Voice/releases/tag/6.0.10)   |

### How do I resume playback after the sleep timer stops?

Once the sleep timer elapses, Voice pauses playback (after a brief fade-out). To keep listening, you have two options:

- **Shake to resume**: Shake your device within 30 seconds of pause to restart playback.
- **Open to resume**: Open the App and simply press on play again

!!! warning

    On some devices (e.g. Samsung S20fe) shake-to-resume may not work reliably.

### How do I continue a book on another device?

Voice keeps your place, your playback speed, your bookmarks and the names you gave your books in a hidden `.voice`
folder inside each audiobook folder you added. Sync these folders between your devices with a tool of your own, like
[Syncthing](https://syncthing.net), and when you open Voice on the other device, it picks up where you left off.

- Add the same folder on every device, and the same way, for example as "Every folder is a book" on both. Where the
  folder is on each device doesn't matter.
- The folder has to be on the device. Folders from cloud apps like Google Drive don't sync.
- The audio files of a book have to be the same on every device.
- Books you added as a single file don't sync. Add the folder they're in instead.
- Your sync tool has to copy hidden folders.
- A book that is playing stays where it is.

If you share a folder with your family and don't want each other's place, names and bookmarks, let your sync tool
ignore `.voice`. In Syncthing, add `.voice` to the folder's ignore patterns.
