#!/usr/bin/env python3
"""Gives every agent its own emulator, cloned read-only from one prepared AVD with a real test library."""

import argparse
import fcntl
import json
import os
import platform
import shutil
import socket
import subprocess
import sys
import time
from contextlib import contextmanager
from datetime import datetime
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).parent))
import ui  # noqa: E402

AVD_NAME = "voice-agent"
API_LEVEL = 36
DEVICE = "pixel_9"
PORTS = range(5560, 5586, 2)
STATE_DIR = Path.home() / ".android" / "voice-agent"
INSTANCES_DIR = STATE_DIR / "instances"
PREPARED_FILE = STATE_DIR / "prepared.json"
MAX_INSTANCES = int(os.environ.get("VOICE_EMULATOR_MAX", "4"))
IDLE_HOURS = float(os.environ.get("VOICE_EMULATOR_IDLE_HOURS", "3"))
# The software renderer that -no-window picks on its own reboots the guest after a snapshot restore.
EMULATOR_FLAGS = ["-gpu", "host", "-no-audio", "-no-boot-anim"]


def repo_root():
  return Path(subprocess.run(["git", "rev-parse", "--show-toplevel"], capture_output=True, text=True, check=True).stdout.strip())


def sdk_root():
  emulator = shutil.which("emulator")
  if emulator:
    return Path(emulator).resolve().parent.parent
  for variable in ["ANDROID_HOME", "ANDROID_SDK_ROOT"]:
    if os.environ.get(variable):
      return Path(os.environ[variable])
  properties = repo_root() / "local.properties"
  if properties.is_file():
    for line in properties.read_text().splitlines():
      if line.startswith("sdk.dir="):
        return Path(line.removeprefix("sdk.dir="))
  sys.exit("No Android SDK found. Put the emulator on the PATH or set ANDROID_HOME.")


def sdk_tool(*relative_paths):
  for relative_path in relative_paths:
    tool = sdk_root() / relative_path
    if tool.exists():
      return str(tool)
  sys.exit(f"{relative_paths[0]} is missing in the SDK at {sdk_root()}.")


def sdk_env():
  return {**os.environ, "ANDROID_HOME": str(sdk_root()), "ANDROID_SDK_ROOT": str(sdk_root())}


def system_image():
  abi = "arm64-v8a" if platform.machine() in ("arm64", "aarch64") else "x86_64"
  return f"system-images;android-{API_LEVEL};google_apis_playstore;{abi}"


@contextmanager
def lock():
  STATE_DIR.mkdir(parents=True, exist_ok=True)
  with open(STATE_DIR / "lock", "w") as file:
    try:
      fcntl.flock(file, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except BlockingIOError:
      print("Waiting for another emulator.py run, e.g. a prepare, which takes about 90 seconds.", flush=True)
      fcntl.flock(file, fcntl.LOCK_EX)
    yield


def alive(instance):
  # After a reboot, the pid of a stale instance can belong to an unrelated process.
  command = subprocess.run(["ps", "-o", "command=", "-p", str(instance["pid"])], capture_output=True, text=True).stdout
  return f"-avd {AVD_NAME}" in command and f"-port {instance['port']}" in command


def instances():
  INSTANCES_DIR.mkdir(parents=True, exist_ok=True)
  result = []
  for file in sorted(INSTANCES_DIR.glob("*.json")):
    instance = json.loads(file.read_text())
    instance["file"] = file
    instance["idle_hours"] = (time.time() - file.stat().st_mtime) / 3600
    if alive(instance):
      result.append(instance)
    else:
      file.unlink()
  return result


def owner_key(args):
  return args.name or str(repo_root())


def port_free(port):
  for candidate in [port, port + 1]:
    with socket.socket() as probe:
      if probe.connect_ex(("127.0.0.1", candidate)) == 0:
        return False
  return True


def kill(instance, reason=None):
  serial = instance["serial"]
  if reason:
    print(f"Stopping {serial} of {instance['owner']}: {reason}")
  subprocess.run(["adb", "-s", serial, "emu", "kill"], capture_output=True)
  deadline = time.monotonic() + 20
  while alive(instance) and time.monotonic() < deadline:
    time.sleep(0.5)
  if alive(instance):
    os.kill(instance["pid"], 9)
  instance["file"].unlink(missing_ok=True)


def reap():
  for instance in instances():
    if not Path(instance["owner"]).exists() and instance["owner"].startswith("/"):
      kill(instance, "its worktree is gone")
    elif instance["idle_hours"] > IDLE_HOURS:
      kill(instance, f"unused for {instance['idle_hours']:.1f} hours")


def adb_ok(serial, *args):
  result = subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True)
  return result.returncode == 0, result.stdout.strip()


def wait_for_boot(serial, process, timeout=240):
  deadline = time.monotonic() + timeout
  stable_since = None
  while time.monotonic() < deadline:
    if process.poll() is not None:
      return False
    ok, value = adb_ok(serial, "shell", "getprop", "sys.boot_completed")
    if ok and value == "1":
      stable_since = stable_since or time.monotonic()
      # adb reconnects once right after a snapshot restore
      if time.monotonic() - stable_since > 2:
        return True
    else:
      stable_since = None
    time.sleep(0.5)
  return False


def launch(port, extra_flags, log_file):
  emulator = sdk_tool("emulator/emulator")
  command = [emulator, "-avd", AVD_NAME, "-port", str(port), *EMULATOR_FLAGS, *extra_flags]
  with open(log_file, "w") as log:
    return subprocess.Popen(command, stdout=log, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL, env=sdk_env(), start_new_session=True)


def command_start(args):
  owner = owner_key(args)
  if not PREPARED_FILE.is_file():
    print(f"The {AVD_NAME} AVD isn't prepared yet, preparing it first.")
    prepare(args, only_if_missing=True)
  with lock():
    reap()
    running = instances()
    own = next((instance for instance in running if instance["owner"] == owner), None)
    if own:
      own["file"].touch()
      print(f"Reusing your running emulator.\nANDROID_SERIAL={own['serial']}")
      return
    if len(running) >= MAX_INSTANCES:
      listing = "\n".join(f"  {instance['serial']}  {instance['owner']}" for instance in running)
      sys.exit(
        f"All {MAX_INSTANCES} agent emulators are taken:\n{listing}\n"
        "Wait for one to stop, or raise VOICE_EMULATOR_MAX if the machine has memory to spare.",
      )
    taken = {instance["port"] for instance in running}
    port = next((port for port in PORTS if port not in taken and port_free(port)), None)
    if port is None:
      sys.exit("No free emulator port left.")
    flags = ["-read-only"]
    if not args.window:
      flags.append("-no-window")
    log_file = INSTANCES_DIR / f"{port}.log"
    process = launch(port, flags, log_file)
    serial = f"emulator-{port}"
    state_file = INSTANCES_DIR / f"{port}.json"
    started = datetime.now().isoformat(timespec="seconds")
    state_file.write_text(json.dumps({"serial": serial, "port": port, "pid": process.pid, "owner": owner, "started": started}))
  if not wait_for_boot(serial, process):
    subprocess.run(["adb", "-s", serial, "emu", "kill"], capture_output=True)
    process.kill()
    state_file.unlink(missing_ok=True)
    sys.exit(f"{serial} didn't boot. Log: {log_file}")
  print(f"Booted with the test library and the app from {json.loads(PREPARED_FILE.read_text())['commit']}.\nANDROID_SERIAL={serial}")


def command_stop(args):
  with lock():
    running = instances()
    if args.all:
      targets = running
    else:
      owner = owner_key(args)
      targets = [instance for instance in running if instance["owner"] == owner]
    if not targets:
      print("No agent emulator to stop.")
    for instance in targets:
      kill(instance)
      print(f"Stopped {instance['serial']}.")


def command_list(args):
  running = instances()
  if not running:
    print("No agent emulators are running.")
  for instance in running:
    print(f"{instance['serial']}  started {instance['started']}  idle {instance['idle_hours']:.1f}h  {instance['owner']}")
  if PREPARED_FILE.is_file():
    prepared = json.loads(PREPARED_FILE.read_text())
    print(f"Base AVD {AVD_NAME} prepared {prepared['date']} from {prepared['commit']}.")
  else:
    print(f"Base AVD {AVD_NAME} isn't prepared yet.")


def run(command, **kwargs):
  if not shutil.which(command[0]):
    sys.exit(f"{command[0]} is missing. Install it and run prepare again.")
  print("$ " + " ".join(command), flush=True)
  result = subprocess.run(command, capture_output=True, text=True, **kwargs)
  if result.returncode != 0:
    sys.exit(f"{result.stdout}{result.stderr}\n{command[0]} failed.")


def create_avd():
  image = system_image()
  sdkmanager = sdk_tool("cmdline-tools/latest/bin/sdkmanager", "tools/bin/sdkmanager")
  avdmanager = sdk_tool("cmdline-tools/latest/bin/avdmanager", "tools/bin/avdmanager")
  if not (sdk_root() / image.replace(";", "/")).is_dir():
    run([sdkmanager, image], env=sdk_env())
  run([avdmanager, "create", "avd", "--force", "-n", AVD_NAME, "-k", image, "-d", DEVICE], input="no\n", env=sdk_env())
  avd_home = Path(os.environ.get("ANDROID_AVD_HOME", Path.home() / ".android" / "avd"))
  config = avd_home / f"{AVD_NAME}.avd" / "config.ini"
  overrides = {"hw.keyboard": "yes", "hw.gpu.enabled": "yes", "hw.gpu.mode": "host"}
  lines = [line for line in config.read_text().splitlines() if line.split("=", 1)[0] not in overrides]
  lines += [f"{key}={value}" for key, value in overrides.items()]
  config.write_text("\n".join(lines) + "\n")


def set_up_library():
  for query in ["Let's go", "Next", "Choose a folder", "testfiles"]:
    tap_when_visible(query)
  # The picker lags behind the taps, and the folders above have an Audiobooks folder and the button as well.
  wait_until_visible("SingleAudiobook")
  tap_when_visible("Audiobooks")
  wait_until_visible("Dream in a Boat")
  for query in ["Use this folder", "Allow", "Add 6 books", "Start listening", "Dream in a Boat", "Play"]:
    tap_when_visible(query)
  time.sleep(2)
  tap_when_visible("Pause")
  ui.adb("shell", "input", "keyevent", "KEYCODE_BACK")
  time.sleep(1)


def wait_until_visible(query, timeout=30):
  deadline = time.monotonic() + timeout
  while time.monotonic() < deadline:
    matches = ui.find(ui.dump(), query, exact=True)
    if matches:
      return matches[0]
    time.sleep(0.5)
  sys.exit(f'"{query}" didn\'t show up while setting up the library. Take a screenshot with ui.py to see where it got stuck.')


def tap_when_visible(query):
  x, y = wait_until_visible(query).center
  print(f"tap {query}", flush=True)
  ui.adb("shell", "input", "tap", str(x), str(y))
  time.sleep(0.3)


def prepare(args, only_if_missing=False):
  root = repo_root()
  with lock():
    if only_if_missing and PREPARED_FILE.is_file():
      return
    running = instances()
    if running:
      sys.exit(f"{len(running)} agent emulator(s) still run from the old base. Stop them first (emulator.py stop --all).")
    PREPARED_FILE.unlink(missing_ok=True)
    create_avd()
    port = next(port for port in PORTS if port_free(port))
    serial = f"emulator-{port}"
    os.environ["ANDROID_SERIAL"] = serial
    STATE_DIR.mkdir(parents=True, exist_ok=True)
    process = launch(port, ["-no-window", "-no-snapshot-load", "-wipe-data"], STATE_DIR / "prepare.log")
    try:
      # builds while the emulator boots
      run([sys.executable, "scripts/populate_test_files.py", "--skip-adb"], cwd=root)
      run([str(root / "gradlew"), ":app:assembleFreeDebug", "-q"], cwd=root)
      if not wait_for_boot(serial, process):
        sys.exit(f"The base AVD didn't boot. Log: {STATE_DIR / 'prepare.log'}")
      run(["adb", "-s", serial, "push", "build/testfiles", "/sdcard/testfiles"], cwd=root)
      run(["adb", "-s", serial, "install", "-r", "app/build/outputs/apk/free/debug/app-free-debug.apk"], cwd=root)
      ui.adb("shell", "am", "start", "-W", "-n", ui.ACTIVITY)
      set_up_library()
    except BaseException:
      subprocess.run(["adb", "-s", serial, "emu", "kill"], capture_output=True)
      raise
    commit = subprocess.run(["git", "log", "-1", "--format=%h %s"], cwd=root, capture_output=True, text=True).stdout.strip()
    if subprocess.run(["git", "status", "--porcelain"], cwd=root, capture_output=True, text=True).stdout.strip():
      commit += " with local changes"
    # killing it saves the quick boot snapshot that every agent emulator starts from
    subprocess.run(["adb", "-s", serial, "emu", "kill"], capture_output=True)
    process.wait()
    PREPARED_FILE.write_text(json.dumps({"commit": commit, "date": datetime.now().isoformat(timespec="seconds")}))
  print(f"Prepared {AVD_NAME} with the app from {commit}.")


def main():
  parser = argparse.ArgumentParser(description=__doc__)
  owner = argparse.ArgumentParser(add_help=False)
  owner.add_argument("--name", help="who owns the emulator, defaults to the git worktree")
  subparsers = parser.add_subparsers(required=True)

  start_parser = subparsers.add_parser("start", parents=[owner], help="start or reuse your emulator and print its ANDROID_SERIAL")
  start_parser.add_argument("--window", action="store_true", help="show the emulator window, e.g. so the user can watch")
  start_parser.set_defaults(func=command_start)

  stop_parser = subparsers.add_parser("stop", parents=[owner], help="stop your emulator")
  stop_parser.add_argument("--all", action="store_true", help="stop every agent emulator")
  stop_parser.set_defaults(func=command_stop)

  subparsers.add_parser("list", help="show the running agent emulators").set_defaults(func=command_list)
  subparsers.add_parser("prepare", help=f"(re)build the {AVD_NAME} AVD that every agent emulator is cloned from").set_defaults(func=prepare)

  args = parser.parse_args()
  args.func(args)


if __name__ == "__main__":
  main()
