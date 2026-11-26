package voice.core.playback.player

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.MediaSource.MediaPeriodId
import androidx.media3.exoplayer.source.SinglePeriodTimeline
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.runner.RunWith

private const val STREAM = "abs://track/item/1"

@RunWith(AndroidJUnit4::class)
class StreamBufferTest {

  private val loadControl = streamBuffer().also { it.onPrepared(PlayerId.UNSET) }

  @Test
  fun `a stream loads up to about 10 minutes ahead, then stops`() {
    assertEquals(
      listOf(true, true, false),
      listOf(0.seconds, 9.minutes, 10.minutes).map { loads(STREAM, buffered = it) },
    )
  }

  @Test
  fun `a stream loads again only when little is left`() {
    assertEquals(
      listOf(false, false, true),
      listOf(10.minutes, 5.minutes, 59.seconds).map { loads(STREAM, buffered = it) },
    )
  }

  @Test
  fun `a file on the device keeps the small buffer`() {
    assertEquals(
      listOf(false, true),
      listOf("file:///book/1.mp3", STREAM).map { loads(it, buffered = 55.seconds) },
    )
  }

  private fun loads(
    uri: String,
    buffered: Duration,
  ): Boolean {
    val timeline =
      SinglePeriodTimeline(
        60.minutes.inWholeMicroseconds,
        true,
        false,
        false,
        null,
        MediaItem.fromUri(uri),
      )
    return loadControl.shouldContinueLoading(
      LoadControl.Parameters(
        PlayerId.UNSET,
        timeline,
        MediaPeriodId(timeline.getUidOfPeriod(0)),
        0,
        buffered.inWholeMicroseconds,
        1F,
        true,
        false,
        C.TIME_UNSET,
        C.TIME_UNSET,
      ),
    )
  }
}
