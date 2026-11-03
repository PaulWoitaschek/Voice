#!/usr/bin/env python3
"""Shows Voice from the emulator in ANDROID_SERIAL on the Android Auto Desktop Head Unit (DHU)."""

import argparse
import json
import os
import shlex
import shutil
import subprocess
import sys
import tempfile
import time
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "emulator" / "scripts"))
import ui  # noqa: E402

GEARHEAD = "com.google.android.projection.gearhead"
NOTIFICATION_LISTENER = f"{GEARHEAD}/com.google.android.gearhead.notifications.SharedNotificationListenerManager$ListenerService"
HEAD_UNIT_PORT = 5277
APK_DIR = Path.home() / ".android" / "voice-agent" / "android-auto"
STATE_DIR = Path.home() / ".android" / "voice-agent" / "dhu"


def dhu_binary():
  roots = []
  properties = Path(__file__).resolve().parents[4] / "local.properties"
  if properties.is_file():
    roots += [line.removeprefix("sdk.dir=") for line in properties.read_text().splitlines() if line.startswith("sdk.dir=")]
  roots += [os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT"), Path.home() / "Library" / "Android" / "sdk"]
  emulator = shutil.which("emulator")
  if emulator:
    roots.append(Path(emulator).resolve().parent.parent)
  for root in filter(None, roots):
    binary = Path(root) / "extras" / "google" / "auto" / "desktop-head-unit"
    if binary.is_file():
      return binary
  sys.exit("The DHU is missing. Install it with: sdkmanager 'extras;google;auto'")


def instance_dir():
  return STATE_DIR / ui.serial()


def read_state():
  state_file = instance_dir() / "state.json"
  if not state_file.is_file():
    return None
  state = json.loads(state_file.read_text())
  command = subprocess.run(["ps", "-o", "command=", "-p", str(state["pid"])], capture_output=True, text=True).stdout
  if "desktop-head-unit" in command and f"--adb={state['port']}" in command:
    return state
  shutil.rmtree(instance_dir(), ignore_errors=True)
  return None


def shell(command):
  return ui.adb("shell", command, check=False).stdout.decode(errors="replace")


def head_unit_server_running():
  for line in shell("netstat -ltn").splitlines():
    columns = line.split()
    if "LISTEN" in columns and columns[3].endswith(f":{HEAD_UNIT_PORT}"):
      return True
  return False


def wait_for(query, timeout=15):
  deadline = time.monotonic() + timeout
  while time.monotonic() < deadline:
    matches = ui.find(ui.dump(), query, exact=True)
    if matches:
      return matches[0]
    time.sleep(0.5)
  sys.exit(f'"{query}" didn\'t show up while setting up Android Auto. Take a screenshot with ui.py to see where it got stuck.')


def tap(node):
  x, y = node.center
  ui.adb("shell", "input", "tap", str(x), str(y))
  time.sleep(0.5)


def scroll_to(query):
  for _ in range(10):
    matches = ui.find(ui.dump(), query, exact=True)
    if matches:
      return matches[0]
    ui.command_scroll(argparse.Namespace(direction="down"))
  sys.exit(f'"{query}" isn\'t in the Android Auto settings.')


def open_menu():
  tap(wait_for("More options"))
  wait_for("Help & feedback")
  return {node.label for node in ui.dump()}


def install_android_auto():
  apks = sorted(APK_DIR.glob("*.apk"))
  if not (APK_DIR / "base.apk").is_file():
    sys.exit(
      f"No Android Auto APKs in {APK_DIR}. The emulator only has a stub, and Play needs a Google account.\n"
      "Ask the user to connect a phone with Android Auto from Play, then, with their OK, run: car.py pull <phone serial>",
    )
  print("Installing Android Auto", flush=True)
  ui.adb("install-multiple", "-r", *map(str, apks))
  # Voice granted its cover uris to the stub, and the car shows no covers until Voice grants them again.
  ui.adb("shell", "am", "force-stop", ui.PACKAGE)


def set_up():
  if "/data/app/" not in shell(f"pm path {GEARHEAD}"):
    install_android_auto()
  # Play updates Play services a few minutes after boot, which ends the projection.
  shell("pm disable-user --user 0 com.android.vending")
  shell(f"cmd notification allow_listener {shlex.quote(NOTIFICATION_LISTENER)}")
  print("Turning on Android Auto's developer settings and head unit server", flush=True)
  ui.adb("shell", "am", "start", "-W", "-a", f"{GEARHEAD}.SETTINGS")
  if "Developer settings" not in open_menu():
    ui.adb("shell", "input", "keyevent", "KEYCODE_BACK")
    x, y = scroll_to("Version").center
    for _ in range(10):
      ui.adb("shell", "input", "tap", str(x), str(y))
    tap(wait_for("OK"))
    open_menu()
  tap(wait_for("Developer settings"))
  unknown_sources = scroll_to("Unknown sources")
  checkboxes = [node for node in ui.dump() if node.resource_id == "checkbox"]
  checkbox = min(checkboxes, key=lambda node: abs(node.center[1] - unknown_sources.center[1]))
  if not checkbox.checked:
    tap(unknown_sources)
  ui.adb("shell", "input", "keyevent", "KEYCODE_BACK")
  open_menu()
  tap(wait_for("Start head unit server"))
  deadline = time.monotonic() + 10
  while not head_unit_server_running():
    if time.monotonic() > deadline:
      sys.exit("The head unit server didn't start.")
    time.sleep(0.5)
  ui.adb("shell", "input", "keyevent", "KEYCODE_HOME")


def launch_dhu(port, args):
  directory = instance_dir()
  directory.mkdir(parents=True, exist_ok=True)
  console = directory / "console"
  console.unlink(missing_ok=True)
  os.mkfifo(console)
  command = [str(dhu_binary()), f"--adb={port}"]
  if not args.window:
    command.append("--headless")
  for config in args.config:
    bundled = dhu_binary().parent / "config" / f"{config}.ini"
    command += ["-c", str(bundled if bundled.is_file() else Path(config).resolve())]
  # Holding both ends of the pipe means the DHU never reads an end of file between commands.
  console_fd = os.open(console, os.O_RDWR)
  with open(directory / "dhu.log", "w") as log:
    process = subprocess.Popen(command, stdin=console_fd, stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
  os.close(console_fd)
  (directory / "state.json").write_text(json.dumps({"pid": process.pid, "port": port}))
  return process


def wait_until_projecting(process):
  deadline = time.monotonic() + 60
  while time.monotonic() < deadline:
    if process.poll() is not None:
      log = (instance_dir() / "dhu.log").read_text(errors="replace")
      sys.exit(f"The DHU quit:\n{log[-1500:]}")
    # The car's bottom bar gets a display of its own once Android Auto's home screen is up.
    if '"GhFacetBar"' in shell("dumpsys display"):
      return
    # Android Auto asks to accept its terms on the phone the first time a car connects.
    welcome = [node for node in ui.dump() if node.resource_id == "continue_button" and node.package == GEARHEAD]
    if welcome:
      tap(welcome[0])
    time.sleep(1)
  sys.exit("Android Auto didn't connect to Voice within 60 seconds. Look with car.py screenshot and ui.py screenshot.")


def command_start(args):
  if read_state():
    print("The DHU is already running for this emulator.")
    return
  dhu_binary()
  if not head_unit_server_running():
    set_up()
  port = ui.adb("forward", "tcp:0", f"tcp:{HEAD_UNIT_PORT}").stdout.decode().strip()
  process = launch_dhu(port, args)
  wait_until_projecting(process)
  print("Android Auto runs on the DHU. car.py screenshot shows the car screen.")


def send(command):
  if not read_state():
    sys.exit("No DHU runs for this emulator. Start it with car.py start.")
  console = os.open(instance_dir() / "console", os.O_WRONLY | os.O_NONBLOCK)
  os.write(console, f"{command}\n".encode())
  os.close(console)


def command_send(args):
  send(" ".join(args.command))


def command_tap(args):
  send(f"tap {args.x} {args.y}")
  print(f"tapped {args.x},{args.y}")


def command_screenshot(args):
  path = Path(args.path or tempfile.mkstemp(prefix="car-", suffix=".png")[1]).resolve()
  path.unlink(missing_ok=True)
  send(f"screenshot {path}")
  deadline = time.monotonic() + 10
  size = -1
  while time.monotonic() < deadline:
    if path.is_file() and path.stat().st_size == size and size > 0:
      print(path)
      return
    size = path.stat().st_size if path.is_file() else -1
    time.sleep(0.3)
  sys.exit(f"The DHU didn't save a screenshot. Its log: {instance_dir() / 'dhu.log'}")


def command_stop(args):
  state = read_state()
  if not state:
    print("No DHU runs for this emulator.")
    return
  send("quit")
  deadline = time.monotonic() + 5
  while read_state() and time.monotonic() < deadline:
    time.sleep(0.3)
  if read_state():
    os.kill(state["pid"], 9)
  ui.adb("forward", "--remove", f"tcp:{state['port']}", check=False)
  shutil.rmtree(instance_dir(), ignore_errors=True)
  print("Stopped the DHU.")


def command_pull(args):
  def from_phone(*command):
    return subprocess.run(["adb", "-s", args.serial, *command], capture_output=True, text=True, check=True).stdout

  paths = [line.removeprefix("package:") for line in from_phone("shell", "pm", "path", GEARHEAD).split()]
  if not paths:
    sys.exit(f"Android Auto isn't installed on {args.serial}.")
  with tempfile.TemporaryDirectory() as directory:
    for path in paths:
      from_phone("pull", path, directory)
    shutil.rmtree(APK_DIR, ignore_errors=True)
    shutil.copytree(directory, APK_DIR)
  version = next(line for line in from_phone("shell", "dumpsys", "package", GEARHEAD).splitlines() if "versionName=" in line)
  print(f"Saved Android Auto {version.split('=')[1]} ({len(paths)} APKs) to {APK_DIR}.")


def main():
  parser = argparse.ArgumentParser(description=__doc__)
  subparsers = parser.add_subparsers(required=True)

  start_parser = subparsers.add_parser("start", help="set up Android Auto on the emulator and connect a DHU to it")
  start_parser.add_argument("--window", action="store_true", help="show the DHU window, e.g. so the user can watch")
  start_parser.add_argument(
    "--config",
    action="append",
    default=[],
    help="a car from the DHU's config folder, like default_1080p or rotary, or an .ini path. Repeat to combine.",
  )
  start_parser.set_defaults(func=command_start)

  tap_parser = subparsers.add_parser("tap", help="tap the car screen at screenshot pixels")
  tap_parser.add_argument("x", type=int)
  tap_parser.add_argument("y", type=int)
  tap_parser.set_defaults(func=command_tap)

  screenshot_parser = subparsers.add_parser("screenshot", help="save the car screen and print its path")
  screenshot_parser.add_argument("path", nargs="?")
  screenshot_parser.set_defaults(func=command_screenshot)

  send_parser = subparsers.add_parser("send", help="run a DHU console command: send keycode media, send night")
  send_parser.add_argument("command", nargs="+")
  send_parser.set_defaults(func=command_send)

  subparsers.add_parser("stop", help="quit the DHU").set_defaults(func=command_stop)

  pull_parser = subparsers.add_parser("pull", help="copy Android Auto's APKs from a phone, only with the user's OK")
  pull_parser.add_argument("serial", help="the phone's adb serial")
  pull_parser.set_defaults(func=command_pull)

  args = parser.parse_args()
  if args.func is not command_pull:
    ui.mark_used()
  args.func(args)


if __name__ == "__main__":
  main()
