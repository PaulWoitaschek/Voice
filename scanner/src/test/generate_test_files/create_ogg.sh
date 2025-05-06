#!/bin/bash

set -euo pipefail

RESOURCES_FOLDER="../resources"
ffmpeg \
  -y \
  -f lavfi \
  -i anullsrc=channel_layout=stereo:sample_rate=44100 \
  -f ffmetadata -i chapters.txt \
  -map 0:a \
  -map_metadata 1 \
  -c:a libvorbis \
  -t 30 \
  "$RESOURCES_FOLDER/test.ogg"

vorbiscomment --append "$RESOURCES_FOLDER/test.ogg" \
  -t "CHAPTER000=00:00:00.000" -t "CHAPTER000NAME=Introduction" \
  -t "CHAPTER001=00:00:10.000" -t "CHAPTER001NAME=Chapter 1" \
  -t "CHAPTER002=00:00:20.000" -t "CHAPTER002NAME=Chapter 2"

ffprobe "$RESOURCES_FOLDER/test.ogg"
