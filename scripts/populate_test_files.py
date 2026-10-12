#!/usr/bin/env python3
"""Populates realistic audiobook test files: MP3 folders and single files, an M4B with chapters, and Opus."""

import argparse
import re
import shutil
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Optional

ROOT = Path(__file__).resolve().parent.parent
TEST_FILES = ROOT / "build" / "testfiles"
WORK_DIR = ROOT / "build" / "testfiles-work"
SAMPLES = ROOT / "core" / "scanner" / "src" / "test" / "resources"
ARTWORK = ROOT / "artwork" / "demo-data"


@dataclass(frozen=True)
class Chapter:
  title: str
  loop_count: int


@dataclass(frozen=True)
class Book:
  title: str
  author: str
  artwork: str
  extension: str
  chapters: list[Chapter]

  @property
  def cover(self):
    return ARTWORK / self.artwork

  def chapter_file_name(self, index):
    if len(self.chapters) == 1:
      return f"{self.title}.{self.extension}"
    return f"{index + 1:02d} - {self.chapters[index].title}.{self.extension}"

  def metadata(self, title, track: Optional[int]):
    tags = {
      "title": title,
      "album": self.title,
      "album_artist": self.author,
      "artist": self.author,
      "track": f"{track}/{len(self.chapters)}" if track else None,
      "genre": "Audiobook",
      "comment": "Generated for Voice test library",
      "date": "2024",
    }
    return [argument for key, value in tags.items() if value for argument in ["-metadata", f"{key}={value}"]]


BOOKS = [
  Book(
    title="Dream in a Boat",
    author="Amelia Winters",
    artwork="horizon.webp",
    extension="mp3",
    chapters=[
      Chapter("Gliding into Dawn", loop_count=4),
      Chapter("Harbor Whispers", loop_count=5),
      Chapter("Nightfall Reflections", loop_count=6),
    ],
  ),
  Book(
    title="El Guitarrero",
    author="Lucia Marquez",
    artwork="bookshop.webp",
    extension="mp3",
    chapters=[
      Chapter("Strings Awake", loop_count=4),
      Chapter("Courtyard Echoes", loop_count=5),
      Chapter("Flamenco Midnight", loop_count=6),
    ],
  ),
  Book(
    title="The Fabulous Geysha",
    author="Lucia Marquez",
    artwork="echoes.webp",
    extension="m4b",
    chapters=[
      Chapter("Lantern Lights", loop_count=4),
      Chapter("Winding Garden", loop_count=4),
      Chapter("Moonlit Stage", loop_count=5),
    ],
  ),
  Book(
    title="The Boy and the Cave",
    author="June Porter",
    artwork="horizon.webp",
    extension="opus",
    chapters=[
      Chapter("Footsteps Below", loop_count=3),
      Chapter("Hidden Paintings", loop_count=4),
      Chapter("Echoes of Fire", loop_count=5),
    ],
  ),
  Book(
    title="Treeman",
    author="Amelia Winters",
    artwork="bookshop.webp",
    extension="mp3",
    chapters=[Chapter("Treeman", loop_count=8)],
  ),
  Book(
    title="Snakey",
    author="Harlan Pike",
    artwork="echoes.webp",
    extension="mp3",
    chapters=[Chapter("Snakey", loop_count=7)],
  ),
]


def ffmpeg(*arguments):
  subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", *map(str, arguments)], check=True)


def audio_duration_seconds(file):
  command = ["ffprobe", "-v", "error", "-select_streams", "a:0", "-show_entries", "stream=duration", "-of", "csv=p=0", str(file)]
  return float(subprocess.run(command, capture_output=True, text=True, check=True).stdout)


def encode_mp3_sample():
  sample = WORK_DIR / "sample.mp3"
  ffmpeg("-i", SAMPLES / "auphonic_chapters_demo.mp3", "-map", "0:a", "-ac", "1", "-c:a", "libmp3lame", "-b:a", "64k", sample)
  return sample


def escape_ffmetadata(value):
  return re.sub(r"([=;#\\\n])", r"\\\1", value)


def generate(book, sample):
  output_dir = WORK_DIR / book.title
  output_dir.mkdir(parents=True)
  if book.extension == "mp3":
    files = []
    for index, chapter in enumerate(book.chapters):
      output = output_dir / book.chapter_file_name(index)
      ffmpeg(
        "-stream_loop", chapter.loop_count,
        "-i", sample,
        "-i", book.cover,
        "-map", "0:a",
        "-map", "1:0",
        "-c:a", "copy",
        "-c:v", "mjpeg",
        "-disposition:v", "attached_pic",
        "-id3v2_version", "3",
        "-metadata:s:v", "title=Cover",
        "-metadata:s:v", "comment=Cover (front)",
        "-map_metadata", "-1",
        *book.metadata(chapter.title, track=index + 1),
        output,
      )
      files.append(output)
    return files
  if book.extension == "opus":
    files = []
    for index, chapter in enumerate(book.chapters):
      output = output_dir / book.chapter_file_name(index)
      ffmpeg(
        "-stream_loop", chapter.loop_count,
        "-i", sample,
        "-map", "0:a",
        "-map_chapters", "-1",
        "-map_metadata", "-1",
        "-c:a", "copy",
        *book.metadata(chapter.title, track=index + 1),
        output,
      )
      files.append(output)
    # tests covers that are found next to the files
    cover = output_dir / "cover.jpg"
    ffmpeg("-i", book.cover, cover)
    return files + [cover]
  if book.extension == "m4b":
    sample_ms = int(audio_duration_seconds(sample) * 1000)
    lines = [";FFMETADATA1"]
    start_ms = 0
    for chapter in book.chapters:
      end_ms = start_ms + (chapter.loop_count + 1) * sample_ms
      lines += ["[CHAPTER]", "TIMEBASE=1/1000", f"START={start_ms}", f"END={end_ms}", f"title={escape_ffmetadata(chapter.title)}"]
      start_ms = end_ms
    chapters_file = WORK_DIR / f"{book.title}.chapters.txt"
    chapters_file.write_text("\n".join(lines) + "\n")
    output = output_dir / f"{book.title}.{book.extension}"
    ffmpeg(
      "-stream_loop", sum(chapter.loop_count + 1 for chapter in book.chapters) - 1,
      "-i", sample,
      "-i", book.cover,
      "-i", chapters_file,
      "-map", "0:a",
      "-map", "1:0",
      # the chapter titles come along with the metadata of the chapters file
      "-map_metadata", "2",
      "-map_chapters", "2",
      "-c:a", "copy",
      "-c:v", "mjpeg",
      "-disposition:v", "attached_pic",
      *book.metadata(book.title, track=None),
      output,
    )
    return [output]
  raise ValueError(f"Unknown format {book.extension}")


def copy_file(source, target):
  target.parent.mkdir(parents=True, exist_ok=True)
  shutil.copyfile(source, target)


def copy_into(book, files, parent):
  if len(files) == 1:
    copy_file(files[0], parent / f"{book.title}{files[0].suffix}")
  else:
    for file in files:
      copy_file(file, parent / book.title / file.name)


def main():
  parser = argparse.ArgumentParser(description=__doc__)
  parser.add_argument("--skip-adb", action="store_true", help="only generate into build/testfiles, don't push")
  args = parser.parse_args()

  for tool in ["ffmpeg", "ffprobe"]:
    if not shutil.which(tool):
      sys.exit(f"{tool} is missing. Install ffmpeg, e.g. with `brew install ffmpeg`.")
  for book in BOOKS:
    if not book.cover.exists():
      sys.exit(f"Missing demo artwork for {book.title}: {book.cover}")

  shutil.rmtree(TEST_FILES, ignore_errors=True)
  shutil.rmtree(WORK_DIR, ignore_errors=True)
  WORK_DIR.mkdir(parents=True)

  # Chapters loop these with stream copy instead of re-encoding, which keeps the generation instant.
  samples = {
    "mp3": encode_mp3_sample(),
    "m4b": SAMPLES / "auphonic_chapters_demo.m4a",
    "opus": SAMPLES / "auphonic_chapters_demo.opus",
  }
  print(f"Generating {len(BOOKS)} audiobooks")
  generated = []
  for book in BOOKS:
    print(f"- {book.title} by {book.author} ({book.extension})")
    generated.append((book, generate(book, samples[book.extension])))

  print("Copying books into /Audiobooks, /SingleAudiobook and /Authors")
  for book, files in generated:
    copy_into(book, files, TEST_FILES / "Audiobooks")
    copy_into(book, files, TEST_FILES / "Authors" / book.author)
  _, reference_files = generated[0]
  for file in reference_files:
    copy_file(file, TEST_FILES / "SingleAudiobook" / file.name)
  shutil.rmtree(WORK_DIR)

  if args.skip_adb:
    print("Skipping adb push (run without --skip-adb to push to a device).")
  else:
    print("Pushing generated audiobooks to adb")
    subprocess.run(["adb", "shell", "rm", "-rf", "/sdcard/testfiles"], check=True)
    subprocess.run(["adb", "push", str(TEST_FILES), "/sdcard/testfiles"], check=True)


if __name__ == "__main__":
  main()
