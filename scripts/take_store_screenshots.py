#!/usr/bin/env python3
"""Takes the raw Play Store screenshots in artwork/public/raw on an agent emulator of its own."""

import json
import math
import os
import re
import shutil
import subprocess
import sys
import time
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from types import SimpleNamespace
from urllib.parse import quote

ROOT = Path(__file__).resolve().parent.parent
EMULATOR_SCRIPTS = ROOT / ".agents" / "skills" / "emulator" / "scripts"
sys.dont_write_bytecode = True
sys.path.insert(0, str(EMULATOR_SCRIPTS))
import ui  # noqa: E402

RAW = ROOT / "artwork" / "public" / "raw"
LIBRARY = ROOT / "build" / "store-library"
DEMO_DATA = ROOT / "artwork" / "demo-data"
EMULATOR_NAME = "store-screenshots"
APP_LABEL = "Voice [Debug]"


@dataclass(frozen=True)
class DemoBook:
  title: str
  author: str
  cover: str
  duration: str
  chapters: list[str]

  @property
  def id(self):
    return f"content://com.android.externalstorage.documents/tree/primary%3AAudiobooks/document/primary%3AAudiobooks%2F{quote(self.title)}"

  def chapter_seconds(self):
    hours, minutes, seconds = map(int, self.duration.split(":"))
    weights = [1 + 0.2 * math.sin(index * 1.7) for index in range(len(self.chapters))]
    return [(hours * 3600 + minutes * 60 + seconds) * weight / sum(weights) for weight in weights]


# The books of KioskModeDemoData, so that the kiosk screens and the real ones show the same library.
ECHOES = DemoBook(
  title="Echoes of Tomorrow",
  author="Daniel Hartwell",
  cover="echoes.webp",
  duration="14:27:33",
  chapters=[
    "Departure", "The Long Quiet", "Static on the Line", "Night Shift", "Orbit", "The Archive", "Cold Light", "Mira",
    "Dead Reckoning", "Second Sun", "Interference", "The Signal", "Decoding", "Static", "The Choir", "Drift",
    "Old Frequencies", "Contact", "The Answer", "Fault Lines", "Homeward", "Glass Harbor", "Tomorrow's Echo", "Arrival",
  ],
)
BOOKSHOP = DemoBook(
  title="The Bookshop on Maple Street",
  author="Lena Morgan",
  cover="bookshop.webp",
  duration="8:19:47",
  chapters=[
    "The Bell Above the Door", "First Editions", "Rain on Maple Street", "The Regulars", "A Missing Ledger",
    "Tea and Alibis", "The Strange Receipt", "After Closing", "Footprints in the Dust", "The Locked Cabinet",
    "A Clue in the Window", "Margins", "The Night Market", "Confessions", "Closing Time", "Open Again",
  ],
)
HORIZON = DemoBook(
  title="Beyond the Horizon",
  author="Alexander Reeves",
  cover="horizon.webp",
  duration="11:05:22",
  chapters=[
    "The Letter", "Harbor Town", "Northbound", "The Pass", "The Old Map", "Crossing the Ridge", "River of Stones",
    "The Compass Turns", "Snowline", "The Hermit", "Glacier Light", "Lost Bearings", "Storm Camp", "The Summit Road",
    "Echo Valley", "Far Shore", "The Lighthouse", "Driftwood", "Homecoming", "Beyond",
  ],
)
KIOSK_BOOK_ID = "echoes_of_tomorrow"


def ffmpeg(*arguments):
  subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", *map(str, arguments)], check=True)


def generate_library():
  shutil.rmtree(LIBRARY, ignore_errors=True)
  LIBRARY.mkdir(parents=True)
  # The chapters loop this with stream copy, which keeps 34 hours of audio at about 20 MB.
  silence = LIBRARY / "silence.m4a"
  ffmpeg("-f", "lavfi", "-i", "anullsrc=r=22050:cl=mono", "-t", 600, "-c:a", "aac", "-b:a", "16k", silence)
  for book in [ECHOES, BOOKSHOP, HORIZON]:
    folder = LIBRARY / "Audiobooks" / book.title
    folder.mkdir(parents=True)
    ffmpeg("-i", DEMO_DATA / book.cover, folder / "cover.jpg")
    for index, (chapter, seconds) in enumerate(zip(book.chapters, book.chapter_seconds())):
      tags = {"title": chapter, "album": book.title, "artist": book.author, "track": f"{index + 1}/{len(book.chapters)}"}
      ffmpeg(
        "-stream_loop", -1, "-i", silence, "-t", f"{seconds:.3f}", "-c", "copy", "-map_metadata", -1,
        *[argument for key, value in tags.items() for argument in ["-metadata", f"{key}={value}"]],
        folder / f"{index + 1:02d} - {chapter}.m4a",
      )
  silence.unlink()


def adb(*arguments):
  return ui.adb(*map(str, arguments)).stdout.decode(errors="replace").strip()


def emulator(*arguments):
  return subprocess.run([EMULATOR_SCRIPTS / "emulator.py", *arguments, "--name", EMULATOR_NAME], capture_output=True, text=True)


def start_emulator():
  result = emulator("start")
  match = re.search(r"ANDROID_SERIAL=(\S+)", result.stdout)
  if not match:
    sys.exit(f"The emulator didn't start:\n{result.stdout}{result.stderr}")
  os.environ["ANDROID_SERIAL"] = match.group(1)


def wait(query, exact=True, timeout=30):
  deadline = time.monotonic() + timeout
  while time.monotonic() < deadline:
    matches = ui.find(ui.dump(), query, exact=exact)
    if matches:
      return matches[0]
    time.sleep(0.5)
  stuck = LIBRARY / "stuck.png"
  stuck.write_bytes(ui.adb("exec-out", "screencap", "-p").stdout)
  sys.exit(f'"{query}" didn\'t show up. This is the screen: {stuck}')


def tap(query, exact=True):
  x, y = wait(query, exact=exact).center
  adb("shell", "input", "tap", x, y)
  time.sleep(1)


def long_press(x, y):
  adb("shell", "input", "swipe", x, y, x, y, 1000)
  time.sleep(1)


def drag(start, end):
  # `input draganddrop` is too quick for the launcher to pick an icon up
  (x1, y1), (x2, y2) = start, end
  moves = "; ".join(f"input motionevent MOVE {x1 + (x2 - x1) * step // 4} {y1 + (y2 - y1) * step // 4}" for step in range(1, 5))
  adb("shell", f"input motionevent DOWN {x1} {y1}; sleep 1.5; {moves}; sleep 0.5; "
               f"input motionevent MOVE {x2 + 2} {y2 + 2}; sleep 0.5; input motionevent UP {x2 + 2} {y2 + 2}")
  time.sleep(1)


def open_screen(*destinations):
  ui.command_open(SimpleNamespace(destinations=[d if isinstance(d, str) else json.dumps(d) for d in destinations]))
  time.sleep(2)


def capture(name):
  time.sleep(2)
  path = RAW / name
  path.write_bytes(ui.adb("exec-out", "screencap", "-p").stdout)
  print(f"Captured {path.relative_to(ROOT)}", flush=True)


def set_time(hour, minute):
  now = datetime.now().replace(hour=hour, minute=minute, second=0, microsecond=0)
  adb("shell", "cmd", "alarm", "set-time", int(now.timestamp() * 1000))
  demo_mode("clock", "-e", "hhmm", f"{hour:02d}{minute:02d}")


def demo_mode(command, *extras):
  adb("shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", command, *extras)


def set_up_device():
  subprocess.run([ROOT / "gradlew", ":app:installFreeDebug", "-q"], cwd=ROOT, check=True)
  adb("shell", "rm", "-rf", "/sdcard/Audiobooks")
  adb("push", LIBRARY / "Audiobooks", "/sdcard/Audiobooks")
  adb("shell", "pm", "clear", ui.PACKAGE)
  adb("shell", "pm", "grant", ui.PACKAGE, "android.permission.POST_NOTIFICATIONS")
  adb("shell", "cmd", "time_detector", "set_auto_detection_enabled", "false")
  # Animations off keep the sleep timer's digits from rolling mid capture. The waves still show.
  for scale in ["animator_duration_scale", "window_animation_scale", "transition_animation_scale"]:
    adb("shell", "settings", "put", "global", scale, 0)
  adb("shell", "cmd", "uimode", "night", "no")
  adb("shell", "settings", "put", "global", "sysui_demo_allowed", 1)
  demo_mode("enter")
  demo_mode("battery", "-e", "level", 100, "-e", "plugged", "false")
  demo_mode("network", "-e", "wifi", "show", "-e", "level", 4, "-e", "fully", "true")
  demo_mode("network", "-e", "mobile", "hide")
  demo_mode("notifications", "-e", "visible", "false")


def add_library():
  adb("shell", "am", "start", "-W", "-n", ui.ACTIVITY)
  for query in ["Let's go", "Next", "Choose a folder"]:
    tap(query)
  # the picker opens where the base AVD added its test library, so this goes back to the storage root
  wait("breadcrumb_text")
  storage = next(node for node in ui.dump() if node.resource_id == "breadcrumb_text")
  adb("shell", "input", "tap", *storage.center)
  wait("Alarms")
  tap("Audiobooks")
  wait(ECHOES.title)
  tap("Use this folder")
  tap("Allow")
  wait("We found 3 books")
  capture("phone/8.png")
  tap("Add 3 books")
  tap("Start listening")


def seek(fraction):
  slider = next(node for node in ui.dump() if node.cls == "SeekBar")
  left, _, right, _ = slider.bounds
  adb("shell", "input", "tap", int(left + (right - left) * fraction), slider.center[1])
  time.sleep(1)


def open_chapter(book, chapter):
  open_screen("BookOverview", {"type": "Playback", "bookId": book.id})
  tap("Chapters")
  for _ in range(5):
    if ui.find(ui.dump(), chapter, exact=True):
      break
    adb("shell", "input", "swipe", 540, 2000, 540, 1000, 1000)
  tap(chapter)


def scroll_to(query, y):
  for _ in range(3):
    distance = wait(query).center[1] - y
    if abs(distance) < 40:
      return
    adb("shell", "input", "swipe", 540, 1800, 540, 1800 - distance, 1500)
    time.sleep(1)


def add_widget(name):
  long_press(540, 1300)
  tap("Widgets")
  tap("Search")
  adb("shell", "input", "text", "Voice")
  tap(APP_LABEL)
  widget = f"{name} widget, 4 wide by 2 high"
  for _ in range(5):
    if ui.find(ui.dump(), widget, exact=False):
      break
    adb("shell", "input", "swipe", 540, 2000, 540, 1000, 1000)
  tap(widget, exact=False)
  tap("Add")
  # dismisses the resize frame
  adb("shell", "input", "keyevent", "KEYCODE_BACK")


def set_up_home_screen():
  adb("shell", "input", "keyevent", "KEYCODE_HOME")
  dock_y = wait("Phone").center[1]
  # The dump only shows the "Item removed" snackbar after a drag, so everything is looked up once.
  nodes = ui.dump()
  # Two 4×2 widgets only fit without the row of apps above the dock.
  icons = [
    node.center for node in nodes
    if node.clickable and node.own_label in ["Play Store", "Gmail", "Photos", "YouTube"] and node.center[1] < dock_y
  ]
  last_in_dock = max(node.center for node in nodes if node.clickable and node.center[1] == dock_y)
  for icon in icons:
    # the Remove target only shows up while dragging
    drag(icon, (540, 300))
  # otherwise the launcher suggests another app in the last spot of the dock
  adb("shell", "input", "swipe", 540, 1700, 540, 600, 300)
  drag(wait(APP_LABEL).center, last_in_dock)
  add_widget("Now playing")
  add_widget("Shelf")
  # the launcher puts a widget that doesn't fit on a new page and shows that one
  wait("Now playing")


def enable_kiosk_mode():
  open_screen("BookOverview", "Settings", "DeveloperSettings")
  tap("kiosk_mode", exact=False)
  wait("Override active", exact=False)


def main():
  for tool in ["ffmpeg", "adb"]:
    if not shutil.which(tool):
      sys.exit(f"{tool} is missing.")
  generate_library()
  start_emulator()
  try:
    for folder in ["phone", "feature"]:
      (RAW / folder).mkdir(parents=True, exist_ok=True)
    set_up_device()
    set_time(9, 30)
    add_library()

    open_chapter(BOOKSHOP, "After Closing")
    seek(0.4)

    open_chapter(HORIZON, "The Compass Turns")
    seek(0.25)
    seek(0.6)
    wait("Back to", exact=False)
    capture("phone/2.png")

    open_screen("BookOverview", "Settings")
    scroll_to("Appearance", 420)
    capture("phone/7.png")

    set_time(22, 41)
    adb("shell", "cmd", "uimode", "night", "yes")
    open_chapter(ECHOES, "The Signal")
    seek(0.55)
    tap("Play")
    tap("Sleep timer")
    tap("30 minutes")
    capture("phone/3.png")

    adb("shell", "cmd", "uimode", "night", "no")
    set_up_home_screen()
    capture("phone/6.png")

    set_time(9, 30)
    enable_kiosk_mode()
    open_screen("BookOverview")
    capture("phone/1.png")
    open_screen("BookOverview", {"type": "Playback", "bookId": KIOSK_BOOK_ID})
    capture("feature/1.png")
    open_screen("BookOverview", {"type": "Playback", "bookId": KIOSK_BOOK_ID}, {"type": "Bookmarks", "bookId": KIOSK_BOOK_ID})
    capture("phone/4.png")
    tap("History")
    capture("phone/5.png")
  finally:
    emulator("stop")
  print("Done. Render the store images with scripts/generate_artworks.sh.")


if __name__ == "__main__":
  main()
