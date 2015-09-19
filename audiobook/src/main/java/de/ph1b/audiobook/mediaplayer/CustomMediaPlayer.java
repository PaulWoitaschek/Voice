package de.ph1b.audiobook.mediaplayer;


import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.support.annotation.Nullable;

import org.vinuxproject.sonic.Sonic;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import de.ph1b.audiobook.utils.L;

/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author James Falcon
 * @author Paul Woitaschek
 */
@TargetApi(16)
public class CustomMediaPlayer implements MediaPlayerInterface {
    private static final String TAG = CustomMediaPlayer.class.getSimpleName();
    private final ReentrantLock lock = new ReentrantLock();
    private final Object decoderLock = new Object();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private PowerManager.WakeLock wakeLock = null;
    private AudioTrack track;
    private Sonic sonic;
    private MediaExtractor extractor;
    private MediaCodec codec;
    @Nullable
    private String path = null;
    private volatile boolean continuing = false;
    private volatile boolean isDecoding = false;
    private volatile boolean flushCodec = false;
    private float speed = 1.0F;
    @Nullable
    private MediaPlayerInterface.OnCompletionListener onCompletionListener;
    private volatile State state = State.IDLE;
    private final Runnable decoderRunnable = new Runnable() {
        @Override
        public void run() {
            isDecoding = true;
            codec.start();
            @SuppressWarnings("deprecation") ByteBuffer[] inputBuffers = codec.getInputBuffers();
            @SuppressWarnings("deprecation") ByteBuffer[] outputBuffers = codec.getOutputBuffers();
            boolean sawInputEOS = false;
            boolean sawOutputEOS = false;
            while (!sawInputEOS && !sawOutputEOS && continuing) {
                if (state == State.PAUSED) {
                    try {
                        synchronized (decoderLock) {
                            decoderLock.wait();
                        }
                    } catch (InterruptedException e) {
                        // Purposely not doing anything here
                    }
                    continue;
                }
                if (sonic != null) {
                    sonic.setSpeed(speed);
                    sonic.setPitch(1);
                }

                int inputBufIndex = codec.dequeueInputBuffer(200);
                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = inputBuffers[inputBufIndex];
                    int sampleSize = extractor.readSampleData(dstBuf, 0);
                    long presentationTimeUs = 0;
                    if (sampleSize < 0) {
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {
                        presentationTimeUs = extractor.getSampleTime();
                    }
                    codec.queueInputBuffer(
                            inputBufIndex,
                            0,
                            sampleSize,
                            presentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    if (flushCodec) {
                        codec.flush();
                        flushCodec = false;
                    }
                    if (!sawInputEOS) {
                        extractor.advance();
                    }
                }
                final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                byte[] modifiedSamples = new byte[info.size];
                int res;
                //noinspection deprecation
                do {
                    res = codec.dequeueOutputBuffer(info, 200);
                    if (res >= 0) {
                        final byte[] chunk = new byte[info.size];
                        outputBuffers[res].get(chunk);
                        outputBuffers[res].clear();
                        if (chunk.length > 0) {
                            sonic.writeBytesToStream(chunk, chunk.length);
                        } else {
                            sonic.flushStream();
                        }
                        int available = sonic.availableBytes();
                        if (available > 0) {
                            if (modifiedSamples.length < available) {
                                modifiedSamples = new byte[available];
                            }
                            sonic.readBytesFromStream(modifiedSamples, available);
                            track.write(modifiedSamples, 0, available);
                        }
                        codec.releaseOutputBuffer(res, false);
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            sawOutputEOS = true;
                        }
                    } else //noinspection deprecation
                        if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            //noinspection deprecation
                            outputBuffers = codec.getOutputBuffers();
                        } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            track.stop();
                            lock.lock();
                            try {
                                track.release();
                                final MediaFormat oFormat = codec
                                        .getOutputFormat();

                                initDevice(
                                        oFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                                        oFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
                                //noinspection deprecation
                                outputBuffers = codec.getOutputBuffers();
                                track.play();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                lock.unlock();
                            }
                        }
                } while (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED
                        || res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED);
            }

            codec.stop();
            track.stop();
            isDecoding = false;
            if (continuing && (sawInputEOS || sawOutputEOS)) {
                state = State.PLAYBACK_COMPLETED;
                L.d(TAG, "State changed to: " + state);
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (onCompletionListener != null) {
                            onCompletionListener.onCompletion();
                        }
                        stayAwake(false);
                    }
                });
                t.setDaemon(true);
                t.start();
            }
            synchronized (decoderLock) {
                decoderLock.notifyAll();
            }
        }
    };
    @Nullable
    private MediaPlayer.OnErrorListener onErrorListener;
    private long duration;

    private static int findFormatFromChannels(int numChannels) {
        switch (numChannels) {
            case 1:
                return AudioFormat.CHANNEL_OUT_MONO;
            case 2:
                return AudioFormat.CHANNEL_OUT_STEREO;
            default:
                return -1; // Error
        }
    }

    @Override
    public void start() {
        L.v(TAG, "start called in state:" + state);
        switch (state) {
            case PLAYBACK_COMPLETED:
                try {
                    initStream();
                } catch (IOException e) {
                    e.printStackTrace();
                    error("start");
                    break;
                }
            case PREPARED:
                state = State.STARTED;
                L.d(TAG, "State changed to: " + state);
                continuing = true;
                track.play();
                decode();
                stayAwake(true);
                break;
            case STARTED:
                break;
            case PAUSED:
                state = State.STARTED;
                L.d(TAG, "State changed to: " + state + " with path=" + path);
                synchronized (decoderLock) {
                    decoderLock.notify();
                }
                track.play();
                stayAwake(true);
                break;
            default:
                error("start");
                break;
        }
    }

    @Override
    public void reset() {
        L.v(TAG, "reset called in state: " + state);
        stayAwake(false);
        lock.lock();
        try {
            continuing = false;
            try {
                if (state != State.PLAYBACK_COMPLETED) {
                    while (isDecoding) {
                        synchronized (decoderLock) {
                            decoderLock.notify();
                            decoderLock.wait();
                        }
                    }
                }
            } catch (InterruptedException e) {
                L.e(TAG, "Interrupted in reset while waiting for decoder thread to stop.", e);
            }
            if (codec != null) {
                codec.release();
                L.d(TAG, "releasing codec");
                codec = null;
            }
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
            if (track != null) {
                track.release();
                track = null;
            }
            state = State.IDLE;
            L.d(TAG, "State changed to: " + state);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void prepare() throws IOException {
        L.v(TAG, "prepare called in state: " + state);
        switch (state) {
            case INITIALIZED:
            case STOPPED:
                initStream();
                state = State.PREPARED;
                L.d(TAG, "State changed to: " + state);
                break;
            default:
                error("prepare");
        }
    }

    @Override
    public void seekTo(final int ms) {
        switch (state) {
            case PREPARED:
            case STARTED:
            case PAUSED:
            case PLAYBACK_COMPLETED:
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        lock.lock();
                        try {
                            if (track != null) {
                                track.flush();
                                flushCodec = true;
                                long to = ((long) ms * 1000);
                                extractor.seekTo(to, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                            }
                        } finally {
                            lock.unlock();
                        }
                    }
                });
                t.setDaemon(true);
                t.start();
                break;
            default:
                error("seekTo");
        }
    }

    @Override
    public int getCurrentPosition() {
        switch (state) {
            case ERROR:
                error("getCurrentPosition");
                if (onErrorListener != null) {
                    onErrorListener.onError(null, 0, 0);
                }
                return 0;
            case IDLE:
                return 0;
            default:
                return (int) (extractor.getSampleTime() / 1000);
        }
    }

    @Override
    public void pause() {
        L.v(TAG, "pause called");
        switch (state) {
            case PLAYBACK_COMPLETED:
                state = State.PAUSED;
                L.d(TAG, "State changed to: " + state);
                stayAwake(false);
                break;
            case STARTED:
            case PAUSED:
                track.pause();
                state = State.PAUSED;
                L.d(TAG, "State changed to: " + state);
                stayAwake(false);
                break;
            default:
                error("pause");
        }
    }

    @Override
    public float getPlaybackSpeed() {
        return speed;
    }

    @Override
    public void setPlaybackSpeed(float speed) {
        this.speed = speed;
    }

    @Override
    public void setDataSource(String source) {
        L.d(TAG, "setDataSource: " + source);
        switch (state) {
            case IDLE:
                this.path = source;
                state = State.INITIALIZED;
                L.d(TAG, "State changed to: " + state);
                break;
            default:
                error("setDataSource");
                break;
        }
    }

    @Override
    public void setOnErrorListener(@Nullable MediaPlayer.OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
    }

    @Override
    public void setOnCompletionListener(@Nullable MediaPlayerInterface.OnCompletionListener onCompletionListener) {
        this.onCompletionListener = onCompletionListener;
    }

    @Override
    public void setWakeMode(Context context, int mode) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(mode, TAG);
        wakeLock.setReferenceCounted(false);
    }

    @Override
    public int getDuration() {
        return (int) (duration / 1000);
    }

    private void initStream() throws IOException, IllegalArgumentException {
        L.v(TAG, "initStream called in state=" + state);
        lock.lock();
        try {
            extractor = new MediaExtractor();
            if (path != null) {
                extractor.setDataSource(path);
            } else {
                error("initStream");
                throw new IOException();
            }
            int trackNum = 0;
            final MediaFormat oFormat = extractor.getTrackFormat(trackNum);

            if (!oFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                error("initStream");
                throw new IOException("No KEY_SAMPLE_RATE");
            }
            int sampleRate = oFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);

            if (!oFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                error("initStream");
                throw new IOException("No KEY_CHANNEL_COUNT");
            }
            int channelCount = oFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

            if (!oFormat.containsKey(MediaFormat.KEY_MIME)) {
                error("initStream");
                throw new IOException("No KEY_MIME");
            }
            final String mime = oFormat.getString(MediaFormat.KEY_MIME);

            if (!oFormat.containsKey(MediaFormat.KEY_DURATION)) {
                error("initStream");
                throw new IOException("No KEY_DURATION");
            }
            duration = oFormat.getLong(MediaFormat.KEY_DURATION);

            L.v(TAG, "Sample rate: " + sampleRate);
            L.v(TAG, "Mime type: " + mime);
            initDevice(sampleRate, channelCount);
            extractor.selectTrack(trackNum);
            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(oFormat, null, null, 0);
        } finally {
            lock.unlock();
        }
    }

    private void error(String methodName) {
        L.e(TAG, "Error in " + methodName + " at state=" + state);
        state = State.ERROR;
        stayAwake(false);
    }


    /**
     * Initializes the basic audio track to be able to playback.
     *
     * @param sampleRate  The sample rate of the track
     * @param numChannels The number of channels available in the track.
     */
    private void initDevice(int sampleRate, int numChannels) throws IOException {
        L.d(TAG, "initDevice called in state:" + state);
        lock.lock();
        try {
            final int format = findFormatFromChannels(numChannels);
            final int minSize = AudioTrack.getMinBufferSize(sampleRate, format,
                    AudioFormat.ENCODING_PCM_16BIT);

            if (minSize == AudioTrack.ERROR || minSize == AudioTrack.ERROR_BAD_VALUE) {
                L.e(TAG, "minSize=" + minSize);
                throw new IOException("getMinBufferSize returned " + minSize);
            }
            track = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, format,
                    AudioFormat.ENCODING_PCM_16BIT, minSize * 4,
                    AudioTrack.MODE_STREAM);
            sonic = new Sonic(sampleRate, numChannels);
        } finally {
            lock.unlock();
        }
    }

    private void stayAwake(boolean awake) {
        if (wakeLock != null) {
            if (awake && !wakeLock.isHeld()) {
                wakeLock.acquire();
            } else if (!awake && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    @Override
    public void release() {
        reset();
        state = State.END;
    }

    private void decode() {
        L.d(TAG, "decode called ins state=" + state);
        executor.execute(decoderRunnable);
    }

    private enum State {
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