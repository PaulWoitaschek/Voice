#!/bin/bash

set -euo pipefail

ffmpeg \
  -y \
  -f lavfi -i sine=frequency=440:sample_rate=44100 \
  -codec:a aac \
  -b:a 32k \
  -t 30 \
  adts.aac
