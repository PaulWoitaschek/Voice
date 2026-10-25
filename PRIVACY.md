# Privacy Policy

Voice is an audiobook player made by Paul Woitaschek. Your audiobooks, your progress, your bookmarks and your listening
history stay on your device and in your own folders. This page lists every case where Voice sends something off your
device, who gets it and why.

## Which build you have

Voice comes in two builds, and they differ in what they send:

- The **Google Play** build includes Firebase from Google, for crash reports, remote settings and, only if you agree,
  usage analytics.
- The **F-Droid, GitHub and IzzyDroid** builds contain no Google libraries. They send nothing to Google or to me.

The parts under "In every build" apply to both.

## Google Play build

### Crash reports

When Voice crashes or runs into an error, it sends a report to Firebase Crashlytics, so I can fix it. A report contains
what went wrong, your device model, your Android and app version, a random ID for this installation, and the last lines
of Voice's log, like when a scan started or playback paused. Voice keeps the names of your books and files out of
its log, but an error from Android itself can name the file it happened with.

Crash reports are always on in the Google Play build. Crashlytics deletes them after 90 days.

### Usage analytics, only if you agree

When you set up Voice, it asks if it may collect anonymous usage data. Analytics stays off until you agree, and you can
turn it off anytime in the settings.

With analytics on, Voice sends Google Analytics for Firebase which screens you open and which features you use, like
play and pause, the sleep timer or adding a folder. It never sends the names of your books or files. Google adds your
device model, your Android and app version, your language and an approximate location, like your country, taken from
your IP address. Google keeps this data for at most 14 months.

### Remote settings

When Voice starts, it asks Firebase Remote Config for settings, like whether a new feature is turned on. The request
includes a random ID for this installation and the app version.

### Support and ratings

Supporting Voice runs through Google Play's billing, and the rating prompt opens Google Play's review dialog. Google
handles both under its own privacy policy.

## In every build

### Cover search

When you search for a cover online, Voice sends your search, which starts as the book's name, to DuckDuckGo. The
previews come from DuckDuckGo, and the cover you pick is downloaded from the website it's on.

### Audiobookshelf

You can connect Voice to an Audiobookshelf server that you or someone you trust runs. Voice then sends your username
and password to that server to sign in, and exchanges your listening progress, bookmarks and listening sessions,
including your device model and the app version, with it. It also streams and downloads audiobooks from it. This data
goes only to the server you entered. I run no server for this and never receive this data. Signing out removes the
books of the server and their downloads from your device.

### Backups

If backups are on on your phone, Android backs up Voice's data, like your library, progress and bookmarks, to your
backup account, on most phones your Google account. The login to an Audiobookshelf server isn't backed up.

### Links

Help, feedback, the FAQ, Ko-fi and surveys open in your browser. The sites they open have their own privacy policies.
A survey's link includes your app version, the app that installed Voice, such as the Play Store or F-Droid, and how many
days ago you installed Voice, so answers can be told apart. Your answers go to the site that runs the survey.

## Who sees the data

Only I see the crash reports and analytics, in the Firebase console. I use them to fix bugs and to decide what to work
on next. I don't sell data, show ads or use data for marketing. Google stores the data for me and may process it
outside the EU. See [Firebase's privacy information](https://firebase.google.com/support/privacy).

Analytics runs on your consent (Art. 6(1)(a) GDPR). Crash reports and remote settings rely on my legitimate interest in
keeping Voice working (Art. 6(1)(f) GDPR).

## Your rights

You can turn analytics off anytime, and uninstalling Voice stops everything. You can ask me what data I have about you,
and have it corrected or deleted. You can also object to its use and complain to a data protection authority.

## Contact

Paul Woitaschek, [woitaschek@gmail.com](mailto:woitaschek@gmail.com)

When Voice starts sending something new, this page changes with that release.

Last updated: 2026-10-24
