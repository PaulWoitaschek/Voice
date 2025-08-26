#!/bin/bash

ffmpeg -f lavfi -i color=c=darkblue:size=500x500:duration=0.1 \
  -vf "drawtext=text='ALBUM COVER':fontsize=40:fontcolor=white:x=(w-text_w)/2:y=(h-text_h)/2" \
  -frames:v 1 -y cover.jpg

ffmpeg \
  -f lavfi -i anullsrc=channel_layout=stereo:sample_rate=48000 \
  -t 1 \
  -c:a aac -b:a 64k \
  -y dummy_audio.mka

mkvmerge \
  -o mka_with_cover.mka \
  --attachment-mime-type image/jpeg \
  --attachment-name "cover.jpg" \
  --attachment-description "Album Cover" \
  --attach-file cover.jpg \
  dummy_audio.mka

rm dummy_audio.mka cover.jpg
