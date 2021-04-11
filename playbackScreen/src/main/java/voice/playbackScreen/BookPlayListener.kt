package voice.playbackScreen

internal interface BookPlayListener {

  fun close()
  fun fastForward()
  fun rewind()
  fun playPause()
  fun seekTo(milliseconds: Long)
}
