package voice.features.review

/**
 * What the rating prompt shows: the win it celebrates and a few numbers about the listener's library.
 */
internal data class ReviewPrompt(
  val headline: Headline,
  val stats: Stats,
  /**
   * Shown through the feature flag, so it doesn't count as an ask.
   */
  val forced: Boolean,
) {

  sealed interface Headline {
    data class BookFinished(
      val name: String,
      val cover: String?,
    ) : Headline

    data object Milestone : Headline
  }

  data class Stats(
    val listenedHours: Int,
    val finishedBooks: Int,
    val authors: Int,
  )
}
