/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.playback

import android.content.Context
import android.media.*
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by ph1b on 04/01/16.
 */
class FalseChannelDetector
@Inject
constructor(private val context: Context) {

    private fun formatByChannelCount(channelCount: Int) = when (channelCount) {
        1 -> AudioFormat.CHANNEL_OUT_MONO
        2 -> AudioFormat.CHANNEL_OUT_STEREO
        else -> AudioFormat.CHANNEL_INVALID
    }

    fun track(numChannels: Int, sampleRate: Int): AudioTrack? {
        val format = formatByChannelCount(numChannels);
        val minSize = AudioTrack.getMinBufferSize(sampleRate, format,
                AudioFormat.ENCODING_PCM_16BIT);

        if (minSize == AudioTrack.ERROR || minSize == AudioTrack.ERROR_BAD_VALUE) {
            return null
        } else {
            return AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, format,
                    AudioFormat.ENCODING_PCM_16BIT, minSize * 4, AudioTrack.MODE_STREAM);
        }
    }

    fun channelCountMatches(): Boolean {
        val extractor = MediaExtractor()
        val fd = context.assets.openFd("joined.mp3")
        extractor.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
        val format = extractor.getTrackFormat(0)

        // when one value i missing, we can't use our implementation!
        val containsChannelCount = format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
        val containsSampleRate = format.containsKey(MediaFormat.KEY_SAMPLE_RATE)
        val containsMime = format.containsKey(MediaFormat.KEY_MIME)
        val containsDuration = format.containsKey(MediaFormat.KEY_DURATION)
        if (!containsChannelCount || !containsSampleRate || !containsMime || !containsDuration) {
            Timber.d("containsChannelCount=$containsChannelCount and containsSampelRate=$containsSampleRate and containsMime=$containsMime and containsDuration=$containsDuration. Returning false now")
            return false
        }
        val extractorChannelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val mime = format.getString(MediaFormat.KEY_MIME)
        val duration = format.getLong(MediaFormat.KEY_DURATION)
        Timber.d("extractorChannelCount=$extractorChannelCount, sampleRate=$sampleRate, mime=$mime, duration=$duration")

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)

        codec.start()
        val inputBuffers = codec.inputBuffers
        var outputBuffers = codec.outputBuffers
        var sawInputEOS = false
        var sawOutputEOS = false
        var codecSampleRate = 0
        var codecChannelCount = 0
        var bytesCount = 0
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
                        flags);
                if (!sawInputEOS) {
                    extractor.advance()
                }
            }

            var info = MediaCodec.BufferInfo()
            var res: Int
            do {
                res = codec.dequeueOutputBuffer(info, 200);
                if (res >= 0) {
                    val chunk = ByteArray(info.size)
                    outputBuffers[res].get(chunk);
                    outputBuffers[res].clear();
                    if (chunk.size > 0) {
                        bytesCount += chunk.size;
                    }
                    codec.releaseOutputBuffer(res, false);
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true;
                    }
                } else //noinspection deprecation
                    if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        //noinspection deprecation
                        outputBuffers = codec.outputBuffers;
                    } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                        val oFormat = codec
                                .outputFormat;
                        codecSampleRate = oFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                        codecChannelCount = oFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                        Timber.d("Codec output format changed");
                        Timber.d("Codec output sample rate = " + codecSampleRate);
                        Timber.d("Codec output channel count = " + codecChannelCount);
                        //noinspection deprecation
                        outputBuffers = codec.outputBuffers;

                    }
            } while (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED || res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED);
        }

        val calculatedCount = (duration * codecSampleRate * codecChannelCount * 2)
        Timber.i("calculatedCount =$calculatedCount, measuredCount=$bytesCount")

        return true
    }

}