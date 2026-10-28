#!/bin/bash

set -euo pipefail

# ffmpeg writes the grouping into the ©grp atom, like Audiobookshelf when it embeds a book's series
ffmpeg \
  -y \
  -f lavfi -i anullsrc=r=22050:cl=mono \
  -t 1 \
  -codec:a aac -b:a 32k \
  -metadata title="The Way of Kings" \
  -metadata grouping="The Stormlight Archive #1; Cosmere #4" \
  grouping.m4b
