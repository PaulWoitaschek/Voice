package voice.playback.session.search

/**
 * The parsed book search
 */
data class BookSearch(
  val query: String? = null,
  val mediaFocus: String? = null,
  val album: String? = null,
  val artist: String? = null,
)
