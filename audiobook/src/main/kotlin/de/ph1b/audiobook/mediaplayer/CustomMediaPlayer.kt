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
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

package de.ph1b.audiobook.mediaplayer


import android.annotation.TargetApi
import android.content.Context
import android.media.*
import android.os.PowerManager
import de.ph1b.audiobook.sonic.Sonic
import rx.Observable
import rx.subjects.PublishSubject
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock

/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.

 * @author James Falcon
 * *
 * @author Paul Woitaschek
 */
@TargetApi(16)
class CustomMediaPlayer : MediaPlayerInterface {
    private val lock = ReentrantLock()
    private val decoderLock = Object()
    private val executor = Executors.newSingleThreadExecutor()
    private var wakeLock: PowerManager.WakeLock? = null
    private var track: AudioTrack? = null
    private var sonic: Sonic? = null
    private var extractor: MediaExtractor? = null
    private var codec: MediaCodec? = null
    private var path: String? = null
    @Volatile private var continuing = false
    @Volatile private var isDecoding = false
    @Volatile private var flushCodec = false
    override var playbackSpeed = 1.0f
    @Volatile private var state = State.IDLE
    private val decoderRunnable = Runnable {
        isDecoding = true
        codec!!.start()
        @Suppress("DEPRECATION")
        val inputBuffers = codec!!.inputBuffers
        @Suppress("DEPRECATION")
        var outputBuffers = codec!!.outputBuffers
        var sawInputEOS = false
        var sawOutputEOS = false
        while (!sawInputEOS && !sawOutputEOS && continuing) {
            if (state == State.PAUSED) {
                try {
                    synchronized (decoderLock) {
                        decoderLock.wait()
                    }
                } catch (e: InterruptedException) {
                    // Purposely not doing anything here
                }

                continue
            }
            if (sonic != null) {
                sonic!!.speed = playbackSpeed
                sonic!!.pitch = 1f
            }

            val inputBufIndex = codec!!.dequeueInputBuffer(200)
            if (inputBufIndex >= 0) {
                val dstBuf = inputBuffers[inputBufIndex]
                var sampleSize = extractor!!.readSampleData(dstBuf, 0)
                var presentationTimeUs: Long = 0
                if (sampleSize < 0) {
                    sawInputEOS = true
                    sampleSize = 0
                } else {
                    presentationTimeUs = extractor!!.sampleTime
                }
                codec!!.queueInputBuffer(
                        inputBufIndex,
                        0,
                        sampleSize,
                        presentationTimeUs,
                        if (sawInputEOS) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                if (flushCodec) {
                    codec!!.flush()
                    flushCodec = false
                }
                if (!sawInputEOS) {
                    extractor!!.advance()
                }
            }
            val info = MediaCodec.BufferInfo()
            var modifiedSamples = ByteArray(info.size)
            var res: Int
            @Suppress("DEPRECATION")
            do {
                res = codec!!.dequeueOutputBuffer(info, 200)
                if (res >= 0) {
                    val chunk = ByteArray(info.size)
                    outputBuffers[res].get(chunk)
                    outputBuffers[res].clear()
                    if (chunk.size > 0) {
                        sonic!!.writeBytesToStream(chunk, chunk.size)
                    } else {
                        sonic!!.flushStream()
                    }
                    val available = sonic!!.availableBytes()
                    if (available > 0) {
                        if (modifiedSamples.size < available) {
                            modifiedSamples = ByteArray(available)
                        }
                        sonic!!.readBytesFromStream(modifiedSamples, available)
                        track!!.write(modifiedSamples, 0, available)
                    }
                    codec!!.releaseOutputBuffer(res, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true
                    }
                } else
                    if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        //noinspection deprecation
                        outputBuffers = codec!!.outputBuffers
                    } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        track!!.stop()
                        lock.lock()
                        try {
                            track!!.release()
                            val oFormat = codec!!.outputFormat

                            initDevice(
                                    oFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                                    oFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
                            //noinspection deprecation
                            outputBuffers = codec!!.outputBuffers
                            track!!.play()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        } finally {
                            lock.unlock()
                        }
                    }
            } while (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED || res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
        }

        codec!!.stop()
        track!!.stop()
        isDecoding = false
        if (continuing && (sawInputEOS || sawOutputEOS)) {
            state = State.PLAYBACK_COMPLETED
            Timber.d("State changed to: " + state)
            val t = Thread(java.lang.Runnable {
                internalCompletionObservable.onNext(Unit)
                stayAwake(false)
            })
            t.isDaemon = true
            t.start()
        }
        synchronized (decoderLock) {
            decoderLock.notifyAll()
        }
    }

    private var internalDuration: Long = 0

    override val duration: Int
        get() = (internalDuration / 1000).toInt()

    private fun findFormatFromChannels(numChannels: Int): Int {
        when (numChannels) {
            1 -> return AudioFormat.CHANNEL_OUT_MONO
            2 -> return AudioFormat.CHANNEL_OUT_STEREO
            else -> return -1 // Error
        }
    }

    override fun start() {
        Timber.v("start called in state:" + state)
        when (state) {
            CustomMediaPlayer.State.PLAYBACK_COMPLETED -> {
                try {
                    initStream()
                } catch (e: IOException) {
                    e.printStackTrace()
                    error("start")
                    return
                }

                state = State.STARTED
                Timber.d("State changed to: " + state)
                continuing = true
                track!!.play()
                decode()
                stayAwake(true)
            }
            CustomMediaPlayer.State.PREPARED -> {
                state = State.STARTED
                Timber.d("State changed to: " + state)
                continuing = true
                track!!.play()
                decode()
                stayAwake(true)
            }
            CustomMediaPlayer.State.STARTED -> {
            }
            CustomMediaPlayer.State.PAUSED -> {
                state = State.STARTED
                Timber.d("State changed to: $state with path=$path")
                synchronized (decoderLock) {
                    decoderLock.notify()
                }
                track!!.play()
                stayAwake(true)
            }
            else -> error("start")
        }
    }

    override fun reset() {
        Timber.v("reset called in state: " + state)
        stayAwake(false)
        lock.lock()
        try {
            continuing = false
            try {
                if (state != State.PLAYBACK_COMPLETED) {
                    while (isDecoding) {
                        synchronized (decoderLock) {
                            decoderLock.notify()
                            decoderLock.wait()
                        }
                    }
                }
            } catch (e: InterruptedException) {
                Timber.e("Interrupted in reset while waiting for decoder thread to stop.", e)
            }

            if (codec != null) {
                codec!!.release()
                Timber.d("releasing codec")
                codec = null
            }
            if (extractor != null) {
                extractor!!.release()
                extractor = null
            }
            if (track != null) {
                track!!.release()
                track = null
            }
            state = State.IDLE
            Timber.d("State changed to: " + state)
        } finally {
            lock.unlock()
        }
    }

    @Throws(IOException::class)
    override fun prepare() {
        Timber.v("prepare called in state: " + state)
        when (state) {
            CustomMediaPlayer.State.INITIALIZED, CustomMediaPlayer.State.STOPPED -> {
                initStream()
                state = State.PREPARED
                Timber.d("State changed to: " + state)
            }
            else -> error("prepare")
        }
    }


    override var currentPosition: Int = 0
        get() {
            when (state) {
                CustomMediaPlayer.State.ERROR -> {
                    error("getCurrentPosition")
                    internalErrorObservable.onNext(Unit)
                    return 0
                }
                CustomMediaPlayer.State.IDLE -> return 0
                else -> return (extractor!!.sampleTime / 1000).toInt()
            }
        }
        set(i) {
            when (state) {
                CustomMediaPlayer.State.PREPARED, CustomMediaPlayer.State.STARTED, CustomMediaPlayer.State.PAUSED, CustomMediaPlayer.State.PLAYBACK_COMPLETED -> {
                    val t = Thread(Runnable {
                        lock.lock()
                        try {
                            if (track != null) {
                                track!!.flush()
                                flushCodec = true
                                val to = (field.toLong() * 1000)
                                extractor!!.seekTo(to, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                            }
                        } finally {
                            lock.unlock()
                        }
                    })
                    t.isDaemon = true
                    t.start()
                }
                else -> error("seekTo")
            }
            field = i
        }

    override fun pause() {
        Timber.v("pause called")
        when (state) {
            CustomMediaPlayer.State.PLAYBACK_COMPLETED -> {
                state = State.PAUSED
                Timber.d("State changed to: " + state)
                stayAwake(false)
            }
            CustomMediaPlayer.State.STARTED, CustomMediaPlayer.State.PAUSED -> {
                track!!.pause()
                state = State.PAUSED
                Timber.d("State changed to: " + state)
                stayAwake(false)
            }
            else -> error("pause")
        }
    }

    override fun setDataSource(source: String) {
        Timber.d("setDataSource: " + source)
        when (state) {
            CustomMediaPlayer.State.IDLE -> {
                this.path = source
                state = State.INITIALIZED
                Timber.d("State changed to: " + state)
            }
            else -> error("setDataSource")
        }
    }

    override fun setWakeMode(context: Context, mode: Int) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(mode, CustomMediaPlayer::class.java.simpleName)
        wakeLock!!.setReferenceCounted(false)
    }

    private fun Sonic.availableBytes(): Int {
        return samplesAvailable() * numChannels * 2
    }

    @Throws(IOException::class, IllegalArgumentException::class)
    private fun initStream() {
        Timber.v("initStream called in state=" + state)
        lock.lock()
        try {
            extractor = MediaExtractor()
            if (path != null) {
                extractor!!.setDataSource(path)
            } else {
                error("initStream")
                throw IOException()
            }
            val trackNum = 0
            val oFormat = extractor!!.getTrackFormat(trackNum)

            if (!oFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                error("initStream")
                throw IOException("No KEY_SAMPLE_RATE")
            }
            val sampleRate = oFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)

            if (!oFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                error("initStream")
                throw IOException("No KEY_CHANNEL_COUNT")
            }
            val channelCount = oFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            if (!oFormat.containsKey(MediaFormat.KEY_MIME)) {
                error("initStream")
                throw IOException("No KEY_MIME")
            }
            val mime = oFormat.getString(MediaFormat.KEY_MIME)

            if (!oFormat.containsKey(MediaFormat.KEY_DURATION)) {
                error("initStream")
                throw IOException("No KEY_DURATION")
            }
            internalDuration = oFormat.getLong(MediaFormat.KEY_DURATION)

            Timber.v("Sample rate: " + sampleRate)
            Timber.v("Mime type: " + mime)
            initDevice(sampleRate, channelCount)
            extractor!!.selectTrack(trackNum)
            codec = MediaCodec.createDecoderByType(mime)
            codec!!.configure(oFormat, null, null, 0)
        } finally {
            lock.unlock()
        }
    }

    private fun error(methodName: String) {
        Timber.e("Error in $methodName at state=$state")
        state = State.ERROR
        stayAwake(false)
    }


    /**
     * Initializes the basic audio track to be able to playback.

     * @param sampleRate  The sample rate of the track
     * *
     * @param numChannels The number of channels available in the track.
     */
    @Throws(IOException::class)
    private fun initDevice(sampleRate: Int, numChannels: Int) {
        Timber.d("initDevice called in state:" + state)
        lock.lock()
        try {
            val format = findFormatFromChannels(numChannels)
            val minSize = AudioTrack.getMinBufferSize(sampleRate, format,
                    AudioFormat.ENCODING_PCM_16BIT)

            if (minSize == AudioTrack.ERROR || minSize == AudioTrack.ERROR_BAD_VALUE) {
                Timber.e("minSize=" + minSize)
                throw IOException("getMinBufferSize returned " + minSize)
            }
            track = AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, format,
                    AudioFormat.ENCODING_PCM_16BIT, minSize * 4,
                    AudioTrack.MODE_STREAM)
            sonic = Sonic(sampleRate, numChannels)
        } finally {
            lock.unlock()
        }
    }

    private fun stayAwake(awake: Boolean) {
        if (wakeLock != null) {
            if (awake && !wakeLock!!.isHeld) {
                wakeLock!!.acquire()
            } else if (!awake && wakeLock!!.isHeld) {
                wakeLock!!.release()
            }
        }
    }

    override fun release() {
        reset()
        state = State.END
    }

    private fun decode() {
        Timber.d("decode called ins state=" + state)
        executor.execute(decoderRunnable)
    }

    private val internalErrorObservable = PublishSubject.create<Unit>()
    private val internalCompletionObservable = PublishSubject.create<Unit>()

    override val errorObservable: Observable<Unit>
        get() = internalErrorObservable.asObservable()

    override val completionObservable: Observable<Unit>
        get() = internalCompletionObservable.asObservable()

    private enum class State {
        IDLE,
        ERROR,
        INITIALIZED,
        STARTED,
        PAUSED,
        PREPARED,
        STOPPED,
        PLAYBACK_COMPLETED,
        END
    }
}