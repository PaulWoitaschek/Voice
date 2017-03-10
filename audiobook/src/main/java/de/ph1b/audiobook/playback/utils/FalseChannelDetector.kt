package de.ph1b.audiobook.playback.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import d
import i
import javax.inject.Inject

/**
 * Detects if there is a bug on the device that leads to a playback bug.
 *
 * @see [https://github.com/PaulWoitaschek/MaterialAudiobookPlayer/issues/129]
 *
 * @author igormisha
 * @author Paul Woitaschek
 */
class FalseChannelDetector
@Inject
constructor(private val context: Context) {

  @Suppress("DEPRECATION")
  fun channelCountMatches(): Boolean {
    val monoFile = "mono.mp3"
    val stereoFile = "stereo.mp3"

    var monoOutputChunkSize = 0
    var stereoOutputChunkSize = 0
    var monoChannelCount = 1
    var stereoChannelCount = 2
    arrayOf(monoFile, stereoFile)
        .forEach foreachMark@ { file ->
          i { "Checking $file" }
          val fd = context.assets.openFd(file)
          val extractor = MediaExtractor()
          extractor.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
          val format = extractor.getTrackFormat(0)
          extractor.selectTrack(0)
          // when one value is missing, we can't use our implementation!
          val containsChannelCount = format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
          val containsSampleRate = format.containsKey(MediaFormat.KEY_SAMPLE_RATE)
          val containsMime = format.containsKey(MediaFormat.KEY_MIME)
          val containsDuration = format.containsKey(MediaFormat.KEY_DURATION)
          if (!containsChannelCount || !containsSampleRate || !containsMime || !containsDuration) {
            d { "containsChannelCount=$containsChannelCount and containsSampleRate=$containsSampleRate and containsMime=$containsMime and containsDuration=$containsDuration" }
            return false
          }
          val extractorChannelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
          val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
          val mime = format.getString(MediaFormat.KEY_MIME)
          val duration = format.getLong(MediaFormat.KEY_DURATION)
          d { "extractorChannelCount=$extractorChannelCount, sampleRate=$sampleRate, mime=$mime, duration=$duration" }

          MediaCodec.createDecoderByType(mime).let { codec ->
            codec.configure(format, null, null, 0)
            codec.start()

            val inputBuffers = codec.inputBuffers
            var outputBuffers = codec.outputBuffers
            var sawInputEOS = false
            var sawOutputEOS = false
            var codecSampleRate: Int
            var codecChannelCount: Int
            var firstNotEmptyChunk = false
            while (!sawInputEOS && !sawOutputEOS) {
              val inputBufIndex = codec.dequeueInputBuffer(200)
              if (inputBufIndex >= 0) {
                val dstBuf = inputBuffers[inputBufIndex]
                var sampleSize = extractor.readSampleData(dstBuf, 0)
                val presentationTimeUs =
                    if (sampleSize < 0) {
                      sawInputEOS = true
                      sampleSize = 0
                      0
                    } else {
                      extractor.sampleTime
                    }
                val flags = if (sawInputEOS) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                codec.queueInputBuffer(
                    inputBufIndex,
                    0,
                    sampleSize,
                    presentationTimeUs,
                    flags)
                if (!sawInputEOS) {
                  extractor.advance()
                }
              }

              val info = MediaCodec.BufferInfo()
              var res: Int
              do {
                res = codec.dequeueOutputBuffer(info, 200)
                if (res >= 0) {
                  val chunk = ByteArray(info.size)
                  outputBuffers[res].get(chunk)
                  outputBuffers[res].clear()
                  if (chunk.isNotEmpty()) {
                    // first not empty chunk's size is not stable, so save the second chunk size
                    if (firstNotEmptyChunk) {
                      if (file == monoFile) {
                        monoOutputChunkSize = chunk.size
                      } else {
                        stereoOutputChunkSize = chunk.size
                      }
                      return@foreachMark
                    } else {
                      firstNotEmptyChunk = true
                    }
                  }
                  codec.releaseOutputBuffer(res, false)
                  if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true
                  }
                } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                  outputBuffers = codec.outputBuffers
                } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                  val oFormat = codec
                      .outputFormat
                  codecSampleRate = oFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                  codecChannelCount = oFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                  d { "Codec output format changed" }
                  d { "Codec output sample rate = " + codecSampleRate }
                  d { "Codec output channel count = " + codecChannelCount }

                  outputBuffers = codec.outputBuffers
                  if (file == monoFile) {
                    monoChannelCount = codecChannelCount
                  } else {
                    stereoChannelCount = codecChannelCount
                  }
                }
              } while (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED || res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
            }
            codec.release()
          }
          extractor.release()
        }
    d { "monoOutputChunkSize=$monoOutputChunkSize, stereoOutputChunkSize=$stereoOutputChunkSize" }
    if (monoChannelCount == stereoChannelCount && monoOutputChunkSize != stereoOutputChunkSize) {
      d { "Device channel count is false" }
      return false
    }
    return true
  }
}