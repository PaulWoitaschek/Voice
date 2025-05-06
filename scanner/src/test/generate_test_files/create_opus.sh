#!/bin/bash

set -euo pipefail

RESOURCES_FOLDER="../resources"

ffmpeg \
  -y \
  -f lavfi -i anullsrc=r=44100:cl=stereo \
  -f ffmetadata -i chapters.txt \
  -map 0:a \
  -map_metadata 1 \
  -c:a libopus \
  -b:a 96k \
  -t 30 \
  "$RESOURCES_FOLDER/test.opus"

ffprobe "$RESOURCES_FOLDER/test.opus"
