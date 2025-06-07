#!/bin/bash

ffmpeg \
  -f lavfi -i anullsrc=channel_layout=stereo:sample_rate=48000 \
  -t 180 \
  -c:a aac -b:a 64k \
  -metadata title="Your Album Title" \
  -metadata artist="Your Artist Name" \
  -metadata album="Your Album Name" \
  dummy_audio.mka

mkvmerge \
  -o mka_simple_chapters.mka \
  --chapters mka_simple_chapters.txt \
  dummy_audio.mka

rm dummy_audio.mka
