---
name: emulator
description: Running and driving Voice on an Android emulator to see a change in the real app. Every agent gets its own emulator with a real test library, so parallel agents don't get in each other's way. Use when asked to run, start, or screenshot the app, to check a UI or behavior change on a device, to open a specific screen, or for the emulator checks in the design skill.
---

# Running Voice on an emulator

Every agent gets its own emulator. You may start, use, change, and stop it at any time without asking. Never touch
another device: not the user's phone, not their own emulators, not another agent's emulator.

To see how a composable looks at a size, font scale, dark, or right to left, the snapshots skill is faster. Use the
emulator for what previews can't show: real data and playback, flows across screens, sheets and dialogs, system bars,
and Dynamic color.

The scripts live in `.agents/skills/emulator/scripts/`: `emulator.py` for the emulator's lifecycle and `ui.py` for the
screen. Run them from the repository root. They need only Python 3.9+.

## Start

```sh
.agents/skills/emulator/scripts/emulator.py start
```

It prints `ANDROID_SERIAL=emulator-55xx`. Start every Bash command that uses adb, Gradle, or `ui.py` with
`export ANDROID_SERIAL=emulator-55xx;`. Shell state doesn't carry over between commands, and without it adb picks
whatever device is attached or fails.

- It boots in a few seconds as a read-only clone of the `voice-agent` AVD: the debug app past onboarding, and the six
  books from `scripts/populate_test_files.py`, one of them in progress. They cover MP3 folders and single files,
  an M4B with embedded chapters, and Opus with a `cover.jpg` next to it. Changes don't survive a restart, so break
  what you like. `stop`, then `start` gives you a clean device.
- `start` again returns the emulator you already have. It belongs to your git worktree; `--name` picks another owner.
- `--window` shows the emulator window, e.g. when the user wants to watch.
- At most 4 run at once (`VOICE_EMULATOR_MAX`). `start` stops emulators that were unused for 3 hours or whose
  worktree is gone. If all are taken, tell the user instead of stopping someone else's.
- Run `emulator.py stop` when you're done with the task. `emulator.py list` shows all agent emulators and the base
  AVD.

The first `start` on a machine runs `prepare`, which builds the base AVD with your worktree's app in under 90 seconds.
Run `prepare` yourself when the app in it is too old for your branch, e.g. after a database migration. It refuses to
run while agent emulators are running.

## Install your build

```sh
export ANDROID_SERIAL=emulator-55xx; ./gradlew :app:installFreeDebug
```

It installs over the app in the snapshot and keeps the library.

## Look and act

```sh
ui.py dump                # what's on screen: center x,y, label, state
ui.py tap "Settings"      # by text, content description, or resource id
ui.py tap Bookmark --long
ui.py wait "Chapters"     # also --gone
ui.py type "lucia"        # into the focused field
ui.py scroll down
ui.py screenshot          # prints the path of a PNG, scaled to 1200px
adb shell input keyevent KEYCODE_BACK
```

- `dump` merges the texts inside a control into its label, so tapping by any of them works. `NO LABEL` marks a
  control without an accessible label, which is an accessibility bug.
- `tap` prefers exact matches over substrings. With several matches it taps the first and says so; `--nth` picks
  another.
- Use `dump` to find and tap things, and a screenshot (read the PNG) to judge how it looks.

## Open any screen

Debug builds start on a back stack passed as JSON in the `debugBackStack` extra (see `StartDestinationProvider`).
`ui.py open` builds it from `Destination.Compose` names in `navigation/src/main/kotlin/voice/navigation/Destination.kt`,
with JSON for destinations that take arguments:

```sh
ui.py open BookOverview Settings DeveloperSettings
ui.py books               # titles and book ids, last played first
ui.py open BookOverview '{"type":"Playback","bookId":"<id>"}' '{"type":"Bookmarks","bookId":"<id>"}'
```

`open` force-stops the app first and reports a crash on start, like an unknown destination. New destinations work
without extra code, as long as they stay `@Serializable`.

## Device settings

For the checks in the design skill:

```sh
adb shell cmd uimode night yes                                        # dark mode, `no` for light
adb shell settings put system font_scale 1.5                          # also 2.0, 1.0 resets
adb shell cmd locale set-app-locales de.ph1b.audiobook --locales iw   # RTL with real translations
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1                         # landscape, 0 for portrait
for scale in animator_duration_scale window_animation_scale transition_animation_scale; do
  adb shell settings put global $scale 0                              # animations off, 1 turns them on
done
```

Reopen the screen with `ui.py open` after a locale change. Palettes, Dynamic color, and the theme are in the app's
settings.

## Logs and data

- Crashes: `adb logcat -b crash -d`. The app's log: `adb logcat -d --pid=$(adb shell pidof de.ph1b.audiobook)`.
- The app's files: `adb shell run-as de.ph1b.audiobook ls files databases`.
- Playback state: `adb shell dumpsys media_session`.
- `/sdcard/testfiles` also has `Authors` and `SingleAudiobook`, the same books laid out for the other folder types.

Don't use kiosk mode (the `kiosk_mode` flag) to check behavior. It swaps the real view state for static demo data and
nothing plays. It's for store screenshots.
