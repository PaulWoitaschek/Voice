#!/usr/bin/env python3
"""Leases every agent its own emulator from simlock, a copy of the voice baseline with the app and a real test library."""

import argparse
import json
import os
import shlex
import shutil
import signal
import sqlite3
import subprocess
import sys
import tempfile
import time
from pathlib import Path

BASELINE = "voice"
DEVICE = "Pixel 9"
API_LEVEL = "36"
# simlock's longest lease. `start` again renews it.
TTL = "4h"
PACKAGE = "de.ph1b.audiobook"
ACTIVITY = f"{PACKAGE}/voice.app.MainActivity"
DESTINATION_PREFIX = "voice.navigation.Destination."
SMOKE_SNAPSHOT = "voice-smoke"
INSTALL_HINT = "Install it with: npm install -g simlock agent-device"


def repo_root():
  return Path(subprocess.run(["git", "rev-parse", "--show-toplevel"], capture_output=True, text=True, check=True).stdout.strip())


def require_tools():
  for tool in ["simlock", "agent-device"]:
    if not shutil.which(tool):
      sys.exit(f"{tool} is missing. {INSTALL_HINT}")


class SimlockError(Exception):
  def __init__(self, code, message):
    super().__init__(f"{code}: {message}")
    self.code = code


def simlock(*args):
  result = subprocess.run(["simlock", *args], capture_output=True, text=True)
  if result.returncode != 0:
    # Every failure writes one JSON line on stderr.
    for line in reversed(result.stderr.strip().splitlines()):
      try:
        error = json.loads(line)["error"]
      except (ValueError, KeyError, TypeError):
        continue
      raise SimlockError(error.get("code", "UNKNOWN"), error.get("message", ""))
    raise SimlockError("UNKNOWN", result.stderr.strip() or f"simlock {' '.join(args)} failed")
  return result.stdout


def simlock_json(*args):
  return json.loads(simlock(*args, "--json"))


def adb_server_port():
  try:
    return str(json.loads(simlock("config", "get", "drivers.android.adbServerPort")))
  except (SimlockError, ValueError):
    return "5038"


def own_lease():
  """The lease this agent holds, with the serial of its emulator, or None."""
  principal = simlock_json("whoami")["principal"]
  status = simlock_json("status")
  # status leaves out the serials, list has them
  addresses = {device["id"]: device.get("address") for device in json.loads(simlock("list"))}
  for lease in status["leases"]:
    if lease.get("ownerId") == principal:
      return {"id": lease["id"], "serial": addresses.get(lease["deviceId"])}
  return None


def session_name(lease):
  # One agent-device session per lease, so a later lease on the same serial starts clean.
  return f"voice-{lease['id'].removeprefix('lse_')[:8]}"


def device_env(lease):
  return {
    "ANDROID_SERIAL": lease["serial"],
    "ANDROID_ADB_SERVER_PORT": adb_server_port(),
    "AGENT_DEVICE_ANDROID_DEVICE_ALLOWLIST": lease["serial"],
    "AGENT_DEVICE_SESSION": session_name(lease),
    "AGENT_DEVICE_SCREENSHOT_SCALE": "0.5",
  }


def lease_device(baseline=None):
  args = ["lease", "--platform", "android", "--device", DEVICE, "--os", API_LEVEL, "--detach", "--ttl", TTL]
  if baseline:
    args += ["--baseline", baseline]
  grant = json.loads(simlock(*args))
  return {"id": grant["lease"]["id"], "serial": grant["device"]["address"]}


# Everything below works on the emulator in ANDROID_SERIAL, through simlock's adb server.


def serial():
  value = os.environ.get("ANDROID_SERIAL")
  if not value:
    sys.exit("Run the export line that emulator.py start printed first. It sets ANDROID_SERIAL and simlock's adb port.")
  return value


def adb(*args, check=True):
  result = subprocess.run(["adb", "-s", serial(), *args], capture_output=True)
  if check and result.returncode != 0:
    sys.exit(f"adb {' '.join(args)} failed: {result.stderr.decode(errors='replace').strip()}")
  return result


def agent(*args, check=True):
  """Runs agent-device on the emulator in ANDROID_SERIAL and returns its output."""
  result = subprocess.run(["agent-device", *args], capture_output=True, text=True)
  if check and result.returncode != 0:
    sys.exit(f"agent-device {shlex.join(args)} failed:\n{(result.stdout + result.stderr).strip()}")
  return result


def require_trailblaze():
  if not shutil.which("trailblaze"):
    sys.exit("trailblaze is missing. Install it with: brew install block/tap/trailblaze")


def trailblaze_sees_emulator(work):
  devices = subprocess.run(["trailblaze", "device", "list"], cwd=work, capture_output=True, text=True).stdout
  return f"android/{serial()}" in devices


def trailblaze_work_dir(root):
  # Trailblaze writes its settings into the working directory.
  work = root / "build" / "smoke"
  work.mkdir(parents=True, exist_ok=True)
  # The one Trailblaze daemon per machine keeps the adb server it started with.
  if not trailblaze_sees_emulator(work):
    print("Trailblaze doesn't see your emulator. Starting its daemon again on this adb server.", flush=True)
    subprocess.run(["trailblaze", "app", "--stop"], cwd=work, capture_output=True)
    subprocess.run(["trailblaze", "app", "--headless"], cwd=work, capture_output=True)
    if not trailblaze_sees_emulator(work):
      sys.exit(f"Trailblaze still doesn't see {serial()}. Check trailblaze device list.")
  return work


def trail_path(root, name):
  path = root / "smoke-tests" / f"{name}.trail.yaml"
  if not path.exists():
    sys.exit(f"There is no trail {path.relative_to(root)}.")
  return path


def run_trail(work, path):
  print(f"Trail {path.name.removesuffix('.trail.yaml')}", flush=True)
  command = ["trailblaze", "run", str(path), "-d", f"android/{serial()}", "--no-save-recording"]
  passed = subprocess.run(command, cwd=work).returncode == 0
  # agent-device can't read the screen while the Trailblaze app runs.
  adb("shell", "am", "force-stop", "xyz.block.trailblaze.runner", check=False)
  return passed


def run_trails_in_order(root, names):
  work = trailblaze_work_dir(root)
  for name in names:
    if not run_trail(work, trail_path(root, name)):
      sys.exit(f"The trail {name} failed.")


def install_library(root):
  # Right after boot, the shared storage refuses new files for a while.
  deadline = time.monotonic() + 60
  while adb("shell", "touch /sdcard/.voice-probe && rm /sdcard/.voice-probe", check=False).returncode != 0:
    if time.monotonic() > deadline:
      sys.exit("The emulator's shared storage didn't become writable.")
    time.sleep(1)
  adb("push", str(root / "build" / "testfiles"), "/sdcard/testfiles")
  adb("install", "-r", str(root / "app/build/outputs/apk/foss/debug/app-foss-debug.apk"))


def prepare(args=None):
  require_trailblaze()
  root = repo_root()
  if own_lease():
    sys.exit("You hold an emulator. prepare needs a lease of its own: run emulator.py stop first.")
  print("Leasing a plain emulator and building the app and test library meanwhile", flush=True)
  lease_process = subprocess.Popen(
    ["simlock", "lease", "--platform", "android", "--device", DEVICE, "--os", API_LEVEL, "--detach", "--ttl", "1h"],
    stdout=subprocess.PIPE,
    stderr=subprocess.PIPE,
    text=True,
  )
  build = [
    [sys.executable, "scripts/populate_test_files.py", "--skip-adb"],
    [str(root / "gradlew"), ":app:assembleFossDebug", "-q"],
  ]
  for command in build:
    result = subprocess.run(command, cwd=root, capture_output=True, text=True)
    if result.returncode != 0:
      lease_process.kill()
      sys.exit(f"{result.stdout}{result.stderr}\n{shlex.join(command)} failed.")
  stdout, stderr = lease_process.communicate()
  if lease_process.returncode != 0:
    sys.exit(f"simlock couldn't lease an emulator: {stderr.strip().splitlines()[-1] if stderr.strip() else 'no output'}")
  grant = json.loads(stdout)
  lease = {"id": grant["lease"]["id"], "serial": grant["device"]["address"]}
  os.environ.update(device_env(lease))
  saved = False
  try:
    install_library(root)
    run_trails_in_order(root, ["first-start", "listen"])
    commit = subprocess.run(["git", "log", "-1", "--format=%h %s"], cwd=root, capture_output=True, text=True).stdout.strip()
    if subprocess.run(["git", "status", "--porcelain"], cwd=root, capture_output=True, text=True).stdout.strip():
      commit += " with local changes"
    print(simlock("baseline", "save", BASELINE, "--lease", lease["id"], "--note", f"app from {commit}"[:200]).strip())
    saved = True
  except SimlockError as error:
    if error.code == "FORBIDDEN":
      sys.exit("Saving a baseline needs simlock's admin role. Ask the user to run emulator.py prepare.")
    sys.exit(f"simlock failed: {error}")
  finally:
    if not saved:
      try:
        simlock("release", lease["id"])
      except SimlockError:
        pass


def print_env(lease):
  exports = " ".join(f"{key}={value}" for key, value in device_env(lease).items())
  print(f"export {exports}")


def command_start(args):
  require_tools()
  lease = own_lease()
  if lease:
    simlock("lease", "renew", lease["id"], "--ttl", TTL)
    print(f"Reusing your emulator, renewed for {TTL}.")
    print_env(lease)
    return
  try:
    lease = lease_device(BASELINE)
  except SimlockError as error:
    if error.code not in ("UNKNOWN_BASELINE", "BASELINE_STALE"):
      sys.exit(f"simlock couldn't lease an emulator: {error}")
    print(f"The {BASELINE} baseline is missing or stale, preparing it first. That takes about 5 minutes.", flush=True)
    prepare()
    lease = lease_device(BASELINE)
  os.environ.update(device_env(lease))
  agent("open", PACKAGE, "--platform", "android")
  print_env(lease)


def command_stop(args):
  lease = own_lease()
  if not lease:
    print("You hold no emulator.")
    return
  os.environ.update(device_env(lease))
  agent("close", check=False)
  simlock("release", lease["id"])
  print(f"Released {lease['serial']}.")


def command_prepare(args):
  require_tools()
  prepare(args)


def command_setup(args):
  require_trailblaze()
  root = repo_root()
  install_library(root)
  run_trails_in_order(root, ["first-start"])


def destination_json(entry):
  destination = json.loads(entry) if entry.startswith("{") else {"type": entry}
  if "." not in destination["type"]:
    destination["type"] = DESTINATION_PREFIX + destination["type"]
  return destination


def command_open(args):
  back_stack = json.dumps([destination_json(entry) for entry in args.destinations], separators=(",", ":"))
  adb("logcat", "-b", "crash", "-c")
  command = ["am", "start", "-W", "-S", "-n", ACTIVITY, "--es", "debugBackStack", back_stack]
  adb("shell", shlex.join(command))
  time.sleep(1)
  crash = adb("logcat", "-b", "crash", "-d").stdout.decode(errors="replace")
  if crash.strip():
    reason = next((line for line in crash.splitlines() if "Exception" in line and "FATAL" not in line), crash)
    sys.exit(f"The app crashed on start: {reason.split('AndroidRuntime: ', 1)[-1]}")
  print(f"Opened {back_stack}")


def command_books(args):
  with tempfile.TemporaryDirectory() as directory:
    for suffix in ["", "-wal", "-shm"]:
      name = f"autoBookDB{suffix}"
      result = adb("exec-out", "run-as", PACKAGE, "cat", f"databases/{name}", check=False)
      if result.returncode == 0 and result.stdout:
        Path(directory, name).write_bytes(result.stdout)
    database = Path(directory, "autoBookDB")
    if not database.exists():
      sys.exit("No database yet. Is the debug app installed and was it started once?")
    connection = sqlite3.connect(database)
    columns = {column[1] for column in connection.execute("PRAGMA table_info(content2)")}
    # databases from before the listener's name was stored apart have no customName
    name = "coalesce(customName, name)" if "customName" in columns else "name"
    rows = connection.execute(f"SELECT id, {name}, author, isActive FROM content2 ORDER BY lastPlayedAt DESC").fetchall()
    connection.close()
  for book_id, name, author, active in rows:
    if active or args.all:
      print(f"{name} | {author or '-'} | {book_id}")


def emulator_snapshot(action):
  output = adb("emu", "avd", "snapshot", action, SMOKE_SNAPSHOT).stdout.decode(errors="replace")
  if "KO" in output:
    sys.exit(f"The emulator couldn't {action} the snapshot {SMOKE_SNAPSHOT}: {output.strip()}")
  # adb loses the emulator for a moment after a load, and a load can also crash the emulator.
  deadline = time.monotonic() + 60
  while not package_manager_answers():
    if time.monotonic() > deadline:
      state = subprocess.run(["adb", "-s", serial(), "get-state"], capture_output=True, text=True)
      sys.exit(f"The emulator didn't come back after the {action} of the snapshot {SMOKE_SNAPSHOT}. adb: {(state.stdout + state.stderr).strip()}")
    time.sleep(1)


def package_manager_answers():
  try:
    result = subprocess.run(["adb", "-s", serial(), "shell", "pm", "path", "android"], capture_output=True, text=True, timeout=10)
  except subprocess.TimeoutExpired:
    return False
  return "package:" in result.stdout


def command_smoke(args):
  require_trailblaze()
  root = repo_root()
  paths = [trail_path(root, name) for name in args.trails] or sorted((root / "smoke-tests").glob("*.trail.yaml"))
  work = trailblaze_work_dir(root)
  adb("shell", "am", "force-stop", "xyz.block.trailblaze.runner", check=False)
  # Without the last load, the next run would save the state that a trail left behind.
  signal.signal(signal.SIGTERM, lambda *_: sys.exit("smoke was stopped."))
  # Every trail starts from this state and may change what it likes. The last load gives the emulator back as it was.
  emulator_snapshot("save")
  failed = []
  try:
    for path in paths:
      emulator_snapshot("load")
      if not run_trail(work, path):
        failed.append(path.name.removesuffix(".trail.yaml"))
  finally:
    emulator_snapshot("load")
  if failed:
    sys.exit(f"Failed trails: {', '.join(failed)}")
  print(f"All {len(paths)} trails passed.")


def main():
  parser = argparse.ArgumentParser(description=__doc__)
  subparsers = parser.add_subparsers(required=True)

  subparsers.add_parser("start", help="lease or reuse your emulator and print the export line for it").set_defaults(func=command_start)
  subparsers.add_parser("stop", help="give your emulator back").set_defaults(func=command_stop)
  subparsers.add_parser("prepare", help=f"(re)build the {BASELINE} baseline from your worktree's app").set_defaults(func=command_prepare)

  setup_help = "put the built app and the generated test library on a plain emulator and run first-start, as CI does"
  subparsers.add_parser("setup", help=setup_help).set_defaults(func=command_setup)

  open_parser = subparsers.add_parser("open", help="restart the debug app on a back stack: open BookOverview Settings")
  open_parser.add_argument(
    "destinations",
    nargs="+",
    help='Destination names, or JSON for one with arguments: \'{"type":"Playback","bookId":"…"}\'',
  )
  open_parser.set_defaults(func=command_open)

  books_parser = subparsers.add_parser("books", help="list the library with book ids, last played first")
  books_parser.add_argument("--all", action="store_true", help="also books whose files are gone")
  books_parser.set_defaults(func=command_books)

  smoke_parser = subparsers.add_parser("smoke", help="run the smoke test trails on your emulator, each from the same state")
  smoke_parser.add_argument("trails", nargs="*", help="names of the trails to run, like listen. Default: all")
  smoke_parser.set_defaults(func=command_smoke)

  args = parser.parse_args()
  try:
    args.func(args)
  except SimlockError as error:
    sys.exit(f"simlock failed: {error}")


if __name__ == "__main__":
  main()
