package voice.playback.session.search

data class VoiceSearch(
  val query: String? = null,
  val mediaFocus: String? = null,
  val album: String? = null,
  val artist: String? = null,
)
