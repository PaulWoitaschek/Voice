### Why doesn't Voice support the xyz Media Format?

Voice relies on the media formats that are natively supported by the Android platform.

You can review the currently supported file extensions [here](https://developer.android.com/media/media3/exoplayer/supported-formats).
If a file that should be supported is not displayed, it is most likely either corrupted or incompatible with your Android version.

### Why isn’t feature xyz available in the app?

I adhere to a core design principle of minimalism. As such, the app will only include settings and UI components that are absolutely
essential.

### How can I join the beta?

To participate in the public beta, you can either:

- [Join via the Web](https://play.google.com/store/apps/details?id=de.ph1b.audiobook)
- [Join through Google Play](https://play.google.com/apps/testing/de.ph1b.audiobook)

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

!!! tip

    On some devices (e.g. Samsung S20fe) shake-to-resume may not work reliably.

### How does the Auto-Sleep Timer work?

The Auto-Sleep Timer automatically activates when playback starts during a configured time period. You can enable this feature in the settings and configure the time range as well as the auto sleep timer duration.