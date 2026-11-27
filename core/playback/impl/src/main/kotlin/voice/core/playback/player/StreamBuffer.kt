package voice.core.playback.player

import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import kotlin.time.Duration.Companion.minutes

// a stream loads in long bursts, so the radio can sleep between them
internal fun streamBuffer(): LoadControl =
  DefaultLoadControl.Builder()
    .setBufferDurationsMsForStreaming(
      1.minutes.inWholeMilliseconds.toInt(),
      10.minutes.inWholeMilliseconds.toInt(),
      DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
      DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
    )
    .build()
