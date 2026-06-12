# Download Sources

Voice is available from several sources. The app is the same audiobook player, but the source changes who builds and signs it, how updates
arrive, and whether Play-specific integrations are available.

For most users, **Google Play is recommended**. Use **F-Droid** if you prefer a Google-free build.

| Source                                                                         | Auto updates | Google-free   | Android Auto   | APK/build               | Good fit                            |
|--------------------------------------------------------------------------------|--------------|---------------|----------------|-------------------------|-------------------------------------|
| [Google Play](https://play.google.com/store/apps/details?id=de.ph1b.audiobook) | ✅            | ❌<sup>1</sup> | ✅<sup>2</sup>  | Project / Google Play   | Most users                          |
| [F-Droid](https://f-droid.org/packages/de.ph1b.audiobook/)                     | ✅            | ✅             | ❌<sup>2</sup>  | Built by F-Droid        | Open-source repository users        |
| [GitHub](https://github.com/PaulWoitaschek/Voice/releases)                     | ❌            | ✅             | ⚠️<sup>2</sup> | Built by project        | Direct APK downloads                |
| [IzzyDroid](https://apt.izzysoft.de/fdroid/index/apk/de.ph1b.audiobook)        | ✅            | ✅             | ⚠️<sup>2</sup> | Project APK<sup>3</sup> | GitHub APKs with repository updates |

## Notes

1. Google Play uses the `play` build and can include Play/Firebase integrations such as analytics, crash reporting, remote config, and the
   Play review flow.
2. Google Play is the officially supported Android Auto source. GitHub and IzzyDroid builds may work after the Play build has been approved,
   but this is not guaranteed. F-Droid builds do not support Android Auto.
3. IzzyDroid does not build the APK. It republishes the project's APK and adds repository metadata, scans, and verification checks where
   available.

F-Droid, GitHub, and IzzyDroid use the `free` build without those Play-specific integrations.
