RESOURCES_FOLDER=../scanner/src/test/resources

ffmpeg \
  -y \
  -f lavfi -i anullsrc=r=44100:cl=stereo \
  -f ffmetadata -i chapters.txt \
  -map 0:a \
  -map_metadata 1 \
  -codec:a libmp3lame \
  -t 30 \
  $RESOURCES_FOLDER/test.mp3
