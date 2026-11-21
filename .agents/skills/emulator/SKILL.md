---
name: emulator
description: Running and driving Voice on an Android emulator to see a change in the real app. Every agent leases its own emulator with a real test library from simlock and drives it with agent-device, so parallel agents don't get in each other's way. Use when asked to run, start, or screenshot the app, to check a UI or behavior change on a device, to open a specific screen, to run or extend the smoke test, or for the emulator checks in the design skill.
---

# Running Voice on an emulator

Every agent leases its own emulator from [simlock](https://incubator.callstack.com/simlock/docs/getting-started) and
drives it with [agent-device](https://agent-device.dev). You may start, use, change, and stop it at any time without
asking. Never touch another device: not the user's phone, not their own emulators, not another agent's emulator.

To see how a composable looks at a size, font scale, dark, or right to left, the previews skill is faster. Use the
emulator for what previews can't show: real data and playback, flows across screens, sheets and dialogs, system bars,
and Dynamic color. For Android Auto, the android-auto skill connects a desktop head unit to your emulator.

`.agents/skills/emulator/scripts/emulator.py` leases the emulator and does the Voice-specific parts. Run it from the
repository root. It needs Python 3.9+, and `simlock` and `agent-device` from npm. If either is missing, ask the user to
run `npm install -g simlock agent-device`.

## Start

```sh
.agents/skills/emulator/scripts/emulator.py start
```

It prints one `export ANDROID_SERIAL=… ANDROID_ADB_SERVER_PORT=… AGENT_DEVICE_…` line. Start every Bash command that
uses adb, Gradle, agent-device, or `emulator.py` with it. Shell state doesn't carry over between commands. The line
points adb and Gradle at simlock's adb server, and agent-device at your emulator and your session. Without it, adb sees
no emulator and agent-device may pick another agent's.

- It takes a few seconds and gives you a copy of the `voice` baseline: the debug app past onboarding, and the six books
  from `scripts/populate_test_files.py`, one of them in progress. They cover MP3 folders and single files, an M4B with
  embedded chapters, and Opus with a `cover.jpg` next to it. The books are silent, so no emulator plays sound. Break
  what you like: `stop`, then `start` gives you a fresh copy.
- Voice is open in your agent-device session when `start` returns.
- `start` again returns the emulator you already have and renews the lease for 4 hours. simlock takes the emulator
  back when the lease runs out, so run `start` again in a long task.
- simlock gives each agent session one emulator and queues a request while the machine is full. Never run
  `simlock release --all` or `simlock nuke`, and never stop an emulator with adb.
- Run `emulator.py stop` when you're done with the task. `simlock status` shows all emulators and leases.

The first `start` on a machine runs `prepare`, which builds the `voice` baseline with your worktree's app in about 5
minutes. It replays the `first-start` and `listen` trails, so it needs Trailblaze, see [Smoke test](#smoke-test).
Run `prepare` yourself when the app in it is too old for your branch, e.g. after a database migration. Agents on the
old baseline keep it until they stop.

## Install your build

```sh
export ANDROID_SERIAL=…; ./gradlew :app:installFossDebug
```

It installs over the app in the baseline and keeps the library.

## Look and act

```sh
agent-device snapshot -i                       # what's on screen, with @e refs
agent-device press @e3 --settle                # or by label: press 'label="Settings"' --settle
agent-device longpress 'label="Bookmark"'
agent-device fill @e7 "lucia" --settle         # replaces the text, type appends
agent-device scroll down --until 'label="Sleep timer"' --settle
agent-device wait 'label="Chapters"' 15000     # also wait absent …
agent-device back --system --settle
agent-device screenshot                        # prints the path of a PNG at half size
agent-device record start /tmp/flow.mp4        # record stop ends it
agent-device audit                             # controls without an accessible name
```

- `--settle` waits until the screen is still and prints what changed, with fresh refs. Refs go stale after each
  change, so continue from the last diff or snapshot.
- Labels match without case. A text inside a control is often its own node, and pressing it presses the control.
- `audit` reports `missing-name` for a control without an accessible label, which is an accessibility bug. It also
  flags controls cut off at the edge of the screen, so scroll them into view before you report one.
- Use the snapshot to find and press things, and a screenshot (read the PNG) to judge how it looks.
- `agent-device help <command>` and `agent-device help workflow` have the rest.

## Open any screen

Debug builds start on a back stack passed as JSON in the `debugBackStack` extra (see `StartDestinationProvider`).
`emulator.py open` builds it from `Destination.Compose` names in
`navigation/src/main/kotlin/voice/navigation/Destination.kt`, with JSON for destinations that take arguments:

```sh
emulator.py open BookOverview Settings DeveloperSettings
emulator.py books               # titles and book ids, last played first
emulator.py open BookOverview '{"type":"Playback","bookId":"<id>"}' '{"type":"Bookmarks","bookId":"<id>"}'
```

`open` force-stops the app first and reports a crash on start, like an unknown destination. New destinations work
without extra code, as long as they stay `@Serializable`.

## Smoke test

```sh
emulator.py smoke          # all trails
emulator.py smoke listen   # only the trails you name
```

It replays the trails in `smoke-tests` on your emulator with [Trailblaze](https://block.github.io/trailblaze),
without a model. Run it after you install your build, when the change touches playback or a screen that a trail goes
through. If `trailblaze` is missing, ask the user to run `brew install block/tap/trailblaze`.

A full run takes about 7 minutes. Run it in the background or with a timeout of 10 minutes or more. When the run
stops early, it still loads the snapshot. Only a hard kill stops it before that.

Before the first trail, `smoke` saves an emulator snapshot. It loads that snapshot before each trail and at the end.
So each trail starts from your emulator as it was, and your library and places are the same after the run. A
failed trail doesn't stop the others: at the end, the run names each failed trail. There is one Trailblaze daemon per
machine. When it doesn't list your emulator, `smoke` starts it again on simlock's adb server.

When a step fails, the output names the trail, the step and what it looked for. Then decide:

- Voice does the wrong thing: your change has a bug. Fix the app.
- The screen changed on purpose, like a button with a new name: correct the step in the trail in the same pull
  request.

A trail is a YAML file. Each step has its intent in plain words and the recorded tool calls that replay it. To add or
repair one, do the journey with `trailblaze snapshot` and `trailblaze tool`, then write it with
`trailblaze session save`. `trailblaze skill show` explains the commands.

- Run Trailblaze from `build/smoke`, as it writes its settings into the working directory. Pass
  `-d android/$ANDROID_SERIAL` to each command.
- Select by text or content description, never by position.
- Don't expect a value that the trail changes itself, like the position of a book. Remember it and check that it
  changes.
- Don't use `verify` steps, `trailblaze step` or `--self-heal`. They call a model.
- Start from the library with the six books, like the baseline. Change what you like: the next trail starts from the
  snapshot.
- Android's folder picker ignores Trailblaze's taps. `first-start` opens its folders with the keyboard.
- Trailblaze reads only the screen. To check what Voice does in the background, like `headset` does with the play
  key, wait a few seconds and then open Voice.
- When you're done, run `adb shell am force-stop xyz.block.trailblaze.runner`. agent-device can't read the screen
  while that app runs.

## Device settings

For the checks in the design skill:

```sh
agent-device settings appearance dark                                 # light for light
agent-device settings animations off                                  # on turns them on
agent-device orientation landscape-left                               # portrait for portrait
adb shell settings put system font_scale 1.5                          # also 2.0, 1.0 resets
adb shell cmd locale set-app-locales de.ph1b.audiobook --locales iw   # RTL with real translations
```

Reopen the screen with `emulator.py open` after a locale change. Palettes, Dynamic color, and the theme are in the
app's settings.

## Logs and data

- Crashes: `adb logcat -b crash -d`. The app's log: `adb logcat -d --pid=$(adb shell pidof de.ph1b.audiobook)`.
- The app's files: `adb shell run-as de.ph1b.audiobook ls files databases`.
- Playback state: `adb shell dumpsys media_session`.
- `/sdcard/testfiles` also has `Authors` and `SingleAudiobook`, the same books laid out for the other folder types.

Don't use kiosk mode (the `kiosk_mode` flag) to check behavior. It swaps the real view state for static demo data and
nothing plays. It's for store screenshots.
