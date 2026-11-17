#!/usr/bin/env python3
"""Runs a local Audiobookshelf server in Docker with the Voice test books, one server per worktree."""

import argparse
import hashlib
import json
import shutil
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

# Voice needs 2.26 or newer. Pass --version to try another one, e.g. 2.20.0 for a server that is too old.
VERSION = "2.37.2"
IMAGE = "ghcr.io/advplyr/audiobookshelf"
LABEL = "voice.audiobookshelf.worktree"
LIBRARY = "Audiobooks"
# Test accounts of this local server only.
ADMIN = ("root", "voice")
LISTENER = ("listener", "voice")


def repo_root():
  return Path(subprocess.run(["git", "rev-parse", "--show-toplevel"], capture_output=True, text=True, check=True).stdout.strip())


ROOT = repo_root()
NAME = "voice-abs-" + hashlib.sha1(str(ROOT).encode()).hexdigest()[:8]
DATA = ROOT / "build" / "audiobookshelf"


def docker(*args, check=True):
  return subprocess.run(["docker", *args], capture_output=True, text=True, check=check)


def require_docker():
  if not shutil.which("docker"):
    sys.exit("docker is missing. Install Docker Desktop, e.g. with `brew install --cask docker`.")
  if docker("info", check=False).returncode != 0:
    sys.exit("The Docker daemon isn't running. Start Docker Desktop (`open -a Docker`), wait until it's up, and run this again.")


def container_state():
  result = docker("inspect", "--format", "{{.State.Status}}", NAME, check=False)
  return result.stdout.strip() if result.returncode == 0 else None


def port():
  out = docker("port", NAME, "80/tcp").stdout.strip().splitlines()[0]
  return int(out.rsplit(":", 1)[1])


def request(method, path, body=None, token=None, timeout=10):
  data = json.dumps(body).encode() if body is not None else None
  req = urllib.request.Request(f"http://127.0.0.1:{port()}{path}", data=data, method=method)
  if data is not None:
    req.add_header("Content-Type", "application/json")
  if token:
    req.add_header("Authorization", f"Bearer {token}")
  with urllib.request.urlopen(req, timeout=timeout) as response:
    text = response.read().decode()
    try:
      return json.loads(text) if text else None
    except json.JSONDecodeError:
      return text


def wait_until_up():
  deadline = time.time() + 90
  while time.time() < deadline:
    try:
      return request("GET", "/status", timeout=2)
    except (urllib.error.URLError, ConnectionError, OSError):
      time.sleep(1)
  sys.exit(f"The server didn't come up. `docker logs {NAME}` shows why.")


def login(username, password):
  user = request("POST", "/login", {"username": username, "password": password})["user"]
  return user.get("accessToken") or user["token"]


def test_books():
  books = ROOT / "build" / "testfiles" / "Audiobooks"
  if not books.exists():
    print("Generating the test books with scripts/populate_test_files.py")
    subprocess.run([sys.executable, str(ROOT / "scripts" / "populate_test_files.py"), "--skip-adb"], check=True)
  return books


def set_up(token):
  request("POST", "/api/users", {
    "username": LISTENER[0],
    "password": LISTENER[1],
    "type": "user",
    "isActive": True,
    "permissions": {"download": True, "update": False, "delete": False, "upload": False, "accessAllLibraries": True, "accessAllTags": True},
  }, token)
  library = request("POST", "/api/libraries", {
    "name": LIBRARY,
    "folders": [{"fullPath": "/audiobooks"}],
    "mediaType": "book",
  }, token)
  request("POST", f"/api/libraries/{library['id']}/scan", token=token)
  wanted = sum(1 for _ in (DATA / "audiobooks").iterdir())
  deadline = time.time() + 60
  while time.time() < deadline:
    total = request("GET", f"/api/libraries/{library['id']}/items?limit=1", token=token)["total"]
    if total >= wanted:
      return total
    time.sleep(1)
  sys.exit(f"The scan found fewer than {wanted} books. `docker logs {NAME}` shows why.")


def start(args):
  require_docker()
  state = container_state()
  if state and args.version and docker("inspect", "--format", "{{.Config.Image}}", NAME).stdout.strip() != f"{IMAGE}:{args.version}":
    print(f"Replacing the server with Audiobookshelf {args.version}")
    remove()
    state = None
  if state is None:
    version = args.version or VERSION
    shutil.rmtree(DATA, ignore_errors=True)
    shutil.copytree(test_books(), DATA / "audiobooks")
    (DATA / "config").mkdir(parents=True)
    (DATA / "metadata").mkdir(parents=True)
    docker(
      "run", "-d", "--name", NAME, "--label", f"{LABEL}={ROOT}",
      "-p", "127.0.0.1::80",
      "-v", f"{DATA / 'audiobooks'}:/audiobooks",
      "-v", f"{DATA / 'config'}:/config",
      "-v", f"{DATA / 'metadata'}:/metadata",
      f"{IMAGE}:{version}",
    )
  elif state != "running":
    docker("start", NAME)
  status = wait_until_up()
  if not status.get("isInit"):
    request("POST", "/init", {"newRoot": {"username": ADMIN[0], "password": ADMIN[1]}})
    print(f"Set up with {set_up(login(*ADMIN))} books")
  print_status()


def remove():
  docker("rm", "-f", NAME, check=False)
  shutil.rmtree(DATA, ignore_errors=True)


def stop(_args):
  require_docker()
  remove()
  print(f"Removed {NAME} and its data")


def print_status(_args=None):
  require_docker()
  state = container_state()
  if state != "running":
    sys.exit(f"No server runs for this worktree ({state or 'none'}). Run `start`.")
  version = request("GET", "/status").get("serverVersion")
  p = port()
  print(f"Audiobookshelf {version} in {NAME}")
  print(f"  On this Mac:      http://127.0.0.1:{p}")
  print(f"  In the emulator:  http://10.0.2.2:{p}")
  print(f"  Admin:            {ADMIN[0]} / {ADMIN[1]}")
  print(f"  Can't edit books: {LISTENER[0]} / {LISTENER[1]}")


def api(args):
  require_docker()
  body = json.loads(args.body) if args.body else None
  try:
    result = request(args.method.upper(), args.path, body, login(*ADMIN))
  except urllib.error.HTTPError as error:
    sys.exit(f"{error.code} {error.reason}: {error.read().decode()}")
  print(json.dumps(result, indent=2) if not isinstance(result, str) else result)


def logs(_args):
  sys.stdout.write(docker("logs", "--tail", "200", NAME).stdout)


def main():
  parser = argparse.ArgumentParser(description=__doc__)
  commands = parser.add_subparsers(dest="command", required=True)
  start_parser = commands.add_parser("start", help="start the server, set it up on the first start, print its address")
  start_parser.add_argument("--version", help=f"Audiobookshelf version, {VERSION} when left out")
  start_parser.set_defaults(func=start)
  commands.add_parser("status", help="print the address and the accounts").set_defaults(func=print_status)
  commands.add_parser("stop", help="remove the server and its data").set_defaults(func=stop)
  commands.add_parser("logs", help="the last lines of the server log").set_defaults(func=logs)
  api_parser = commands.add_parser("api", help="call the server API as the admin, e.g. api GET /api/libraries")
  api_parser.add_argument("method")
  api_parser.add_argument("path")
  api_parser.add_argument("body", nargs="?", help="JSON body")
  api_parser.set_defaults(func=api)
  args = parser.parse_args()
  args.func(args)


if __name__ == "__main__":
  main()
