package voice.features.review

import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import voice.core.datastore.DurationInHoursSerializer
import voice.core.datastore.DurationInMillisecondsSerializer
import voice.core.datastore.EpochMillisecondsSerializer

/** What happened with the prompt asking for a rating so far. */
@Serializable
data class ReviewPromptState(
  /** How often the prompt was shown. */
  val asks: Int = 0,
  @SerialName("lastAskedAtMillis")
  @Serializable(with = EpochMillisecondsSerializer::class)
  val lastAskedAt: Instant? = null,
  /** The total listening time when the prompt was last shown. */
  @SerialName("listenedAtLastAskMillis")
  @Serializable(with = DurationInMillisecondsSerializer::class)
  val listenedAtLastAsk: Duration = Duration.ZERO,
  /** The highest listening milestone that was celebrated with the prompt. */
  @SerialName("celebratedMilestoneHours")
  @Serializable(with = DurationInHoursSerializer::class)
  val celebratedMilestone: Duration = Duration.ZERO,
  /** The prompt waits until then. */
  @SerialName("snoozedUntilMillis")
  @Serializable(with = EpochMillisecondsSerializer::class)
  val snoozedUntil: Instant? = null,
  /** The listener went to rate, so the prompt never shows again. */
  val done: Boolean = false,
)
