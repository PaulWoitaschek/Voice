package de.ph1b.audiobook.playback


import android.media.MediaCodec
import android.media.MediaFormat
import android.media.PlaybackParams
import android.os.Build
import com.google.android.exoplayer.ExoPlaybackException
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer
import com.google.android.exoplayer.SampleSource
import sonic.Sonic
import java.nio.ByteBuffer

/**
 *
 * Copyright [2015] [Paul Woitaschek]
 * Inspired by jedhoffmann and ojw28
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * Track renderer that support variable playback speed.
 */
class SpeedRenderer(source: SampleSource) : MediaCodecAudioTrackRenderer(source) {

    private lateinit var sonic: Sonic
    private lateinit var sonicInBuffer: ByteArray
    private lateinit var sonicOutBuffer: ByteArray

    private var lastBufferIndex = -1
    private lateinit var lastModifiedBuffer: ByteBuffer
    var playbackSpeed = 1F
        set(value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val params = PlaybackParams()
                params.setSpeed(value)
                handleMessage(MSG_SET_PLAYBACK_PARAMS, params)
            }
            field = value
        }

    @Throws(ExoPlaybackException::class)
    override fun processOutputBuffer(positionUs: Long, elapsedRealtimeUs: Long, codec: MediaCodec, buffer: ByteBuffer,
                                     bufferInfo: MediaCodec.BufferInfo, bufferIndex: Int, shouldSkip: Boolean): Boolean {

        // if we are on marshmallow use playback params directly.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, buffer, bufferInfo, bufferIndex, shouldSkip)
        }

        // buffer not completely processed yet
        if (bufferIndex == lastBufferIndex) {
            return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, lastModifiedBuffer, bufferInfo, bufferIndex, shouldSkip)
        }

        lastBufferIndex = bufferIndex

        val bytesToRead =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    buffer.remaining()
                } else {
                    buffer.position(0)
                    bufferInfo.size
                }

        sonic.speed = playbackSpeed
        buffer.get(sonicInBuffer, 0, bytesToRead)
        sonic.writeBytesToStream(sonicInBuffer, bytesToRead)
        val readBytes = sonic.readBytesFromStream(sonicOutBuffer, sonicOutBuffer.size)

        bufferInfo.offset = 0
        lastModifiedBuffer.position(0)
        bufferInfo.size = readBytes
        lastModifiedBuffer.limit(readBytes)

        return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, lastModifiedBuffer, bufferInfo, bufferIndex, shouldSkip)
    }

    override fun onOutputFormatChanged(format: MediaFormat) {
        super.onOutputFormatChanged(format)

        // this is only necessary below marshmallow. Else we use playback params directly.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            // Two samples per frame * 2 to support narration speeds down to 0.5
            val bufferSizeBytes = SAMPLES_PER_CODEC_FRAME * 2 * 2 * channelCount

            sonicInBuffer = ByteArray(bufferSizeBytes)
            sonicOutBuffer = ByteArray(bufferSizeBytes)
            sonic = Sonic(sampleRate, channelCount)
            lastModifiedBuffer = ByteBuffer.wrap(sonicOutBuffer, 0, 0)
        }
    }

    companion object {

        private val SAMPLES_PER_CODEC_FRAME = 1024
    }
}