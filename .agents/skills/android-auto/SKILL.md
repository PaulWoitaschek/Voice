---
name: android-auto
description: Showing Voice in Android Auto on the Desktop Head Unit (DHU), connected to an agent emulator. Use when asked to check, screenshot, or test something in Android Auto or the car, like the library list, now playing, skip buttons, covers, media keys, or what happens when a car connects.
---

# Running Voice in Android Auto

Android Auto runs on your agent emulator from the emulator skill and shows up on the DHU, Google's head unit for the
desktop. Start your emulator first. Every command here needs its `ANDROID_SERIAL`.

```sh
export ANDROID_SERIAL=emulator-55xx; .agents/skills/android-auto/scripts/car.py start
```

- The first `start` on a booted emulator takes about 45 seconds. It installs the real Android Auto over the stub in
  the image, turns on its developer settings and Unknown sources so the debug build shows up, accepts its terms, and
  starts the head unit server. After that, `start` takes a few seconds until the emulator stops.
- It disables the Play Store on the emulator. Otherwise Play updates Play services a few minutes after boot, which
  ends the projection.
- The DHU runs without a window. `--window` shows it, e.g. when the user wants to watch. `--config` picks another car
  from the DHU's `config` folder, like `default_720p`, `default_1080p`, `default_wide`, `default_6in` or `rotary`, or an
  `.ini` file.
- `car.py stop` quits the DHU. Stopping the emulator quits it too.

It needs two things on the machine:

- The DHU, from `sdkmanager 'extras;google;auto'`. It's looked up in the SDK from `local.properties` first.
- Android Auto's APKs in `~/.android/voice-agent/android-auto`. Play needs a Google account, so they're copied from a
  phone. If they're missing, `start` says so. Ask the user to connect a phone with Android Auto from Play, and only
  with their OK run `car.py pull <phone serial>`. It only reads from the phone. Pull again when Android Auto on the
  emulator asks for an update.

## Look and act

```sh
car.py screenshot           # prints the path of a PNG of the car screen, 800 × 480 by default
car.py tap 501 438          # in screenshot pixels
car.py send keycode back    # any DHU console command
```

- Android Auto draws the car screen on private displays, so there's no tree of labels to tap by. Read the screenshot
  and tap by its pixels. Give the car a second or two after a tap before the next screenshot.
- On a fresh emulator Android Auto starts on YouTube Music. Open Voice from the app grid, the button in the bottom left
  corner, or from the bar at the bottom once it's there.
- The first time a car connects, an Assistant tip covers the bottom left of the screen for a while.
- Media keys go to the app that played last, so play something in Voice before you test them.
- The phone screen still works with `ui.py`, and `adb shell dumpsys media_session` shows the playback state.
- `./gradlew :app:installFreeDebug` works while the car is connected. The car picks up the new build within a few
  seconds.

Useful commands for `send`, the full list is in the
[DHU docs](https://developer.android.com/training/cars/testing/dhu#dhu-commands):

- `keycode home`, `back`, `media`, `media_play_pause`, `media_next`, `media_previous`
- `day`, `night`
- `focus audio off` for when the car plays its own source, like the radio. `focus audio on` gives it back.
- `mic play <file>.wav` talks to Assistant, e.g. with a file from
  `say -o ask.wav --data-format=LEI16@16000 "pause music"`. Assistant writes out what it heard, but in tests "play
  Treeman on Voice" never reached Voice.
