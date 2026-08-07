package voice.core.playback.mediasource

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.DataReader
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.MediaFormatUtil
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.ProgressiveMediaExtractor
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.SeekPoint
import androidx.media3.extractor.TrackOutput
import voice.core.logging.api.Logger
import java.nio.ByteBuffer

/**
 * A [ProgressiveMediaExtractor] backed by the platform's [MediaExtractor] rather than by media3's
 * own Mp4Extractor.
 */
internal class PlatformMediaExtractor(private val context: Context) : ProgressiveMediaExtractor {

  private var extractor: MediaExtractor? = null
  private var trackOutputs: Array<TrackOutput?> = arrayOf()

  private var bytesRead = 0L

  private var sampleBytes = ByteArray(0)
  private var sampleBuffer: ByteBuffer = ByteBuffer.wrap(sampleBytes)
  private var parsableBuffer = ParsableByteArray(sampleBytes)

  override fun init(
    dataReader: DataReader,
    uri: Uri,
    responseHeaders: Map<String, List<String>>,
    position: Long,
    length: Long,
    output: ExtractorOutput,
  ) {
    bytesRead = position

    if (extractor != null) return

    val mediaExtractor = MediaExtractor()
    try {
      mediaExtractor.setDataSource(context, uri, null)
    } catch (e: Throwable) {
      mediaExtractor.release()
      throw e
    }
    extractor = mediaExtractor

    trackOutputs = arrayOfNulls(mediaExtractor.trackCount)
    var durationUs = C.TIME_UNSET
    var maxInputSize = 0
    repeat(mediaExtractor.trackCount) { index ->
      val mediaFormat = mediaExtractor.getTrackFormat(index)
      if (mediaFormat.containsKey(MediaFormat.KEY_DURATION)) {
        durationUs = maxOf(durationUs, mediaFormat.getLong(MediaFormat.KEY_DURATION))
      }
      if (mediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
        maxInputSize = maxOf(maxInputSize, mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
      }
      val format = MediaFormatUtil.createFormatFromMediaFormat(mediaFormat)
      if (MimeTypes.getTrackType(format.sampleMimeType) == C.TRACK_TYPE_AUDIO) {
        val trackOutput = output.track(index, C.TRACK_TYPE_AUDIO)
        trackOutput.format(
          format.buildUpon()
            .setEncoderDelay(mediaFormat.intOrZero(KEY_ENCODER_DELAY))
            .setEncoderPadding(mediaFormat.intOrZero(KEY_ENCODER_PADDING))
            .build(),
        )
        trackOutputs[index] = trackOutput
        mediaExtractor.selectTrack(index)
      }
    }
    sampleBytes = ByteArray(if (maxInputSize > 0) maxInputSize else FALLBACK_BUFFER_SIZE)
    sampleBuffer = ByteBuffer.wrap(sampleBytes)
    parsableBuffer = ParsableByteArray(sampleBytes)

    output.endTracks()
    output.seekMap(TimeBasedSeekMap(durationUs))
    Logger.d("PlatformMediaExtractor prepared $uri, durationUs=$durationUs")
  }

  override fun read(positionHolder: PositionHolder): Int {
    val mediaExtractor = extractor ?: return Extractor.RESULT_END_OF_INPUT
    val trackIndex = mediaExtractor.sampleTrackIndex
    if (trackIndex < 0) return Extractor.RESULT_END_OF_INPUT

    val size = mediaExtractor.readSampleData(sampleBuffer, 0)
    if (size < 0) return Extractor.RESULT_END_OF_INPUT
    bytesRead += size

    val trackOutput = trackOutputs.getOrNull(trackIndex)
    if (trackOutput != null) {
      val timeUs = mediaExtractor.sampleTime
      parsableBuffer.reset(sampleBytes, size)
      trackOutput.sampleData(parsableBuffer, size)
      val flags = if (mediaExtractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
        C.BUFFER_FLAG_KEY_FRAME
      } else {
        0
      }
      trackOutput.sampleMetadata(timeUs, flags, size, 0, null)
    }
    return if (mediaExtractor.advance()) Extractor.RESULT_CONTINUE else Extractor.RESULT_END_OF_INPUT
  }

  override fun seek(
    position: Long,
    timeUs: Long,
  ) {
    extractor?.seekTo(timeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
  }

  override fun getCurrentInputPosition(): Long = bytesRead

  override fun disableSeekingOnMp3Streams() = Unit

  override fun release() {
    extractor?.release()
    extractor = null
    trackOutputs = arrayOf()
    bytesRead = 0
  }

  private class TimeBasedSeekMap(private val durationUs: Long) : SeekMap {
    override fun isSeekable(): Boolean = true

    override fun getDurationUs(): Long = durationUs

    override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints {
      return SeekMap.SeekPoints(SeekPoint(timeUs, 0))
    }
  }

  class Factory(private val context: Context) : ProgressiveMediaExtractor.Factory {
    override fun createProgressiveMediaExtractor(playerId: PlayerId): ProgressiveMediaExtractor {
      return PlatformMediaExtractor(context)
    }
  }

  private companion object {
    const val FALLBACK_BUFFER_SIZE = 64 * 1024

    const val KEY_ENCODER_DELAY = "encoder-delay"
    const val KEY_ENCODER_PADDING = "encoder-padding"

    fun MediaFormat.intOrZero(key: String): Int = if (containsKey(key)) getInteger(key) else 0
  }
}
