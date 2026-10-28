#!/bin/bash

set -euo pipefail

# ffmpeg writes a movement as TXXX, so the MVNM and MVIN frames that Mp3tag and Picard write are put in front by hand
ffmpeg \
  -y \
  -f lavfi -i anullsrc=r=22050:cl=mono \
  -t 1 \
  -codec:a libmp3lame -b:a 32k \
  -id3v2_version 0 \
  audio.mp3

python3 - <<'PYTHON'
def syncsafe(n):
    return bytes([(n >> 21) & 0x7F, (n >> 14) & 0x7F, (n >> 7) & 0x7F, n & 0x7F])

def frame(frame_id, text):
    data = b"\x03" + text.encode()
    return frame_id.encode() + syncsafe(len(data)) + b"\x00\x00" + data

frames = frame("MVNM", "Discworld") + frame("MVIN", "3/41")
audio = open("audio.mp3", "rb").read()
open("movement.mp3", "wb").write(b"ID3\x04\x00\x00" + syncsafe(len(frames)) + frames + audio)
PYTHON

rm audio.mp3
