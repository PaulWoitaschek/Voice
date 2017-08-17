package de.ph1b.audiobook.playback.utils.audioFocus

interface AudioFocusRequester {

  fun request(): Int
  fun abandon()
}
