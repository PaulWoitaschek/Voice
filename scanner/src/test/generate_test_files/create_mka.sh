#!/bin/bash

set -euo pipefail

RESOURCES_FOLDER="../resources"

ffmpeg \
  -y \
  -f lavfi -i anullsrc=r=44100:cl=stereo \
  -f ffmetadata -i chapters.txt \
  -map_metadata 1 \
  -map_chapters 1 \
  -metadata title='Test title' \
  -movflags use_metadata_tags \
  -c:a aac \
  -b:a 96k \
  -t 30 \
  "$RESOURCES_FOLDER/test.mka"

ffprobe "$RESOURCES_FOLDER/test.mka"
