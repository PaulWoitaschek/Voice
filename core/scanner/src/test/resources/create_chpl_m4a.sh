#!/bin/bash

set -euo pipefail

ffmpeg \
  -y \
  -f lavfi -i anullsrc=r=44100:cl=stereo \
  -f ffmetadata -i chapters.txt \
  -map 0:a \
  -map_metadata 0 \
  -codec:a aac  \
  -t 30 \
  test.m4a
