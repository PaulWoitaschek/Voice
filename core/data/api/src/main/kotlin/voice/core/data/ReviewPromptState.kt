package voice.core.data

import kotlinx.serialization.Serializable

/**
 * What happened with the prompt asking for a rating so far.
 */
@Serializable
public data class ReviewPromptState(
  /**
   * How often the prompt was shown.
   */
  val asks: Int = 0,
  val lastAskedAtMillis: Long? = null,
  /**
   * The total listening time when the prompt was last shown.
   */
  val listenedAtLastAskMillis: Long = 0,
  /**
   * The highest listening milestone, in hours, that was celebrated with the prompt.
   */
  val celebratedMilestoneHours: Int = 0,
  /**
   * The prompt waits until then.
   */
  val snoozedUntilMillis: Long? = null,
  /**
   * The listener went to rate, so the prompt never shows again.
   */
  val done: Boolean = false,
)
