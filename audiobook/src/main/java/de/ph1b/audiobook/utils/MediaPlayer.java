package de.ph1b.audiobook.utils;//Copyright 2012 James Falcon
//Edited by Paul Woitaschek
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;

import org.vinuxproject.sonic.Sonic;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

@TargetApi(16)
class MediaPlayer {
    private AudioTrack track;

    private Sonic sonic;
    private MediaExtractor extractor;
    private MediaCodec codec;
    private Thread decoderThread;
    private String path;
    private final Uri uri;
    private final ReentrantLock lock;
    private final Object mDecoderLock;
    private boolean mContinue;
    private boolean mIsDecoding;
    private long mDuration;
    private float mCurrentSpeed;
    private final float mCurrentPitch;
    private int mCurrentState;
    private final Context mContext;
    private final static int TRACK_NUM = 0;
    private final static int STATE_IDLE = 0;
    private final static int STATE_INITIALIZED = 1;
    private final static int STATE_PREPARED = 3;
    private final static int STATE_STARTED = 4;
    private final static int STATE_PAUSED = 5;
    private final static int STATE_STOPPED = 6;
    private final static int STATE_PLAYBACK_COMPLETED = 7;
    private final static int STATE_END = 8;
    private final static int STATE_ERROR = 9;

    private static final String TAG = "MediaPlayer";
    private final PowerManager.WakeLock wakeLock;


    public interface OnCompletionListener {
        public void onCompletion();
    }

    private OnCompletionListener onCompletionListener;

    public void setOnCompletionListener(OnCompletionListener listener) {
        this.onCompletionListener = listener;
    }


    public MediaPlayer(Context context) {
        mCurrentState = STATE_IDLE;
        mCurrentSpeed = (float) 1.0;
        mCurrentPitch = (float) 1.0;
        mContinue = false;
        mIsDecoding = false;
        mContext = context;
        path = null;
        uri = null;
        lock = new ReentrantLock();
        mDecoderLock = new Object();

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, MediaPlayer.class.getName());
        wakeLock.setReferenceCounted(false);
    }


    public int getCurrentPosition() {
        switch (mCurrentState) {
            case STATE_ERROR:
                error();
                break;
            default:
                return (int) (extractor.getSampleTime() / 1000);
        }
        return 0;
    }

    public void setPlaybackSpeed(float speed) {
        this.mCurrentSpeed = speed;
    }

    public float getPlaybackSpeed() {
        return mCurrentSpeed;
    }


    public void pause() {
        switch (mCurrentState) {
            case STATE_STARTED:
            case STATE_PAUSED:
                track.pause();
                mCurrentState = STATE_PAUSED;
                Log.d(TAG, "State changed to STATE_PAUSED");
                stayAwake(false);
                break;
            default:
                error();
        }
    }

    public void prepare() {
        switch (mCurrentState) {
            case STATE_INITIALIZED:
            case STATE_STOPPED:
                try {
                    initStream();
                } catch (IOException e) {
                    Log.e(TAG, "Failed setting data source!", e);
                    error();
                    return;
                }
                mCurrentState = STATE_PREPARED;
                Log.d(TAG, "State changed to STATE_PREPARED");
                break;
            default:
                error();
        }
    }


    public void start() {
        Log.d(TAG, "start called");
        switch (mCurrentState) {
            case STATE_PREPARED:
            case STATE_PLAYBACK_COMPLETED:
                mCurrentState = STATE_STARTED;
                mContinue = true;
                track.play();
                decode();
                stayAwake(true);
            case STATE_STARTED:
                break;
            case STATE_PAUSED:
                mCurrentState = STATE_STARTED;
                synchronized (mDecoderLock) {
                    mDecoderLock.notify();
                }
                track.play();
                stayAwake(true);
                break;
            default:
                mCurrentState = STATE_ERROR;
                if (track != null) {
                    error();
                } else {
                    Log.d("start",
                            "Attempting to start while in idle after construction. Not allowed by no callbacks called");
                }
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

    public void release() {
        reset(); //reset will release wakelock
        onCompletionListener = null;
        mCurrentState = STATE_END;
    }

    public void reset() {
        stayAwake(false);
        lock.lock();
        mContinue = false;
        try {
            if (decoderThread != null
                    && mCurrentState != STATE_PLAYBACK_COMPLETED) {
                while (mIsDecoding) {
                    synchronized (mDecoderLock) {
                        mDecoderLock.notify();
                        mDecoderLock.wait();
                    }
                }
            }
        } catch (InterruptedException e) {
            Log.e(TAG,
                    "Interrupted in reset while waiting for decoder thread to stop.",
                    e);
        }
        if (codec != null) {
            codec.release();
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
        mCurrentState = STATE_IDLE;
        Log.d(TAG, "State changed to STATE_IDLE");
        lock.unlock();
    }

    public void seekTo(final int ms) {
        switch (mCurrentState) {
            case STATE_PREPARED:
            case STATE_STARTED:
            case STATE_PAUSED:
            case STATE_PLAYBACK_COMPLETED:
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        lock.lock();
                        if (track == null) {
                            return;
                        }
                        track.flush();
                        extractor.seekTo(((long) ms * 1000),
                                MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                        lock.unlock();
                    }
                });
                t.setDaemon(true);
                t.start();
                break;
            default:
                error();
        }
    }

    public void setDataSource(String path) {
        Log.d(TAG, "setDataSource: " + path);
        switch (mCurrentState) {
            case STATE_IDLE:
                this.path = path;
                mCurrentState = STATE_INITIALIZED;
                Log.d(TAG, "Moving state to STATE_INITIALIZED");
                break;
            default:
                error();
        }
    }


    private void error() {
        Log.e(TAG, "Moved to error state!");
        stayAwake(false);
        mCurrentState = STATE_ERROR;
    }


    private int findFormatFromChannels(int numChannels) {
        switch (numChannels) {
            case 1:
                return AudioFormat.CHANNEL_OUT_MONO;
            case 2:
                return AudioFormat.CHANNEL_OUT_STEREO;
            default:
                return -1; // Error
        }
    }

    private void initStream() throws IOException {
        lock.lock();
        extractor = new MediaExtractor();
        if (path != null) {
            extractor.setDataSource(path);
        } else if (uri != null) {
            extractor.setDataSource(mContext, uri, null);
        } else {
            throw new IOException();
        }
        final MediaFormat oFormat = extractor.getTrackFormat(TRACK_NUM);
        int sampleRate = oFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = oFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        final String mime = oFormat.getString(MediaFormat.KEY_MIME);
        mDuration = oFormat.getLong(MediaFormat.KEY_DURATION);
        Log.v(TAG, "Sample rate: " + sampleRate);
        Log.v(TAG, "Mime type: " + mime);
        initDevice(sampleRate, channelCount);
        extractor.selectTrack(TRACK_NUM);
        codec = MediaCodec.createDecoderByType(mime);
        codec.configure(oFormat, null, null, 0);
        lock.unlock();
    }

    private void initDevice(int sampleRate, int numChannels) {
        lock.lock();
        final int format = findFormatFromChannels(numChannels);
        final int minSize = AudioTrack.getMinBufferSize(sampleRate, format,
                AudioFormat.ENCODING_PCM_16BIT);
        track = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, format,
                AudioFormat.ENCODING_PCM_16BIT, minSize * 4,
                AudioTrack.MODE_STREAM);
        sonic = new Sonic(sampleRate, numChannels);
        lock.unlock();
    }

    private void decode() {
        decoderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mIsDecoding = true;
                codec.start();
                ByteBuffer[] inputBuffers = codec.getInputBuffers();
                ByteBuffer[] outputBuffers = codec.getOutputBuffers();
                boolean sawInputEOS = false;
                boolean sawOutputEOS = false;
                while (!sawInputEOS && !sawOutputEOS && mContinue) {
                    if (mCurrentState == STATE_PAUSED) {
                        System.out.println("Decoder changed to PAUSED");
                        try {
                            synchronized (mDecoderLock) {
                                mDecoderLock.wait();
                                System.out.println("Done with wait");
                            }
                        } catch (InterruptedException e) {
                            // Purposely not doing anything here
                        }
                        continue;
                    }
                    if (null != sonic) {
                        sonic.setSpeed(mCurrentSpeed);
                        sonic.setPitch(mCurrentPitch);
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
                                sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                        : 0);
                        if (!sawInputEOS) {
                            extractor.advance();
                        }
                    }
                    final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    byte[] modifiedSamples = new byte[info.size];
                    int res;
                    do {
                        res = codec.dequeueOutputBuffer(info, 200);
                        if (res >= 0) {
                            final byte[] chunk = new byte[info.size];
                            outputBuffers[res].get(chunk);
                            outputBuffers[res].clear();
                            if (chunk.length > 0) {
                                sonic.putBytes(chunk, chunk.length);
                            } else {
                                sonic.flush();
                            }
                            int available = sonic.availableBytes();
                            if (available > 0) {
                                if (modifiedSamples.length < available) {
                                    modifiedSamples = new byte[available];
                                }
                                sonic.receiveBytes(modifiedSamples, available);
                                track.write(modifiedSamples, 0, available);
                            }
                            codec.releaseOutputBuffer(res, false);
                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                sawOutputEOS = true;
                            }
                        } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            outputBuffers = codec.getOutputBuffers();
                            Log.d("PCM", "Output buffers changed");
                        } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            track.stop();
                            lock.lock();
                            track.release();
                            final MediaFormat oformat = codec
                                    .getOutputFormat();
                            Log.d("PCM", "Output format has changed to"
                                    + oformat);
                            initDevice(
                                    oformat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                                    oformat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
                            outputBuffers = codec.getOutputBuffers();
                            track.play();
                            lock.unlock();
                        }
                    } while (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED
                            || res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED);
                }
                Log.d(TAG,
                        "Decoding loop exited. Stopping codec and track");
                Log.d(TAG, "Duration: " + (int) (mDuration / 1000));
                Log.d(TAG,
                        "Current position: "
                                + (int) (extractor.getSampleTime() / 1000));
                codec.stop();
                track.stop();
                Log.d(TAG, "Stopped codec and track");
                Log.d(TAG,
                        "Current position: "
                                + (int) (extractor.getSampleTime() / 1000));
                mIsDecoding = false;
                if (mContinue && (sawInputEOS || sawOutputEOS)) {
                    mCurrentState = STATE_PLAYBACK_COMPLETED;
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            onCompletionListener.onCompletion();
                            stayAwake(false);
                        }
                    });
                    t.setDaemon(true);
                    t.start();
                } else {
                    Log.d(TAG,
                            "Loop ended before saw input eos or output eos");
                    Log.d(TAG, "sawInputEOS: " + sawInputEOS);
                    Log.d(TAG, "sawOutputEOS: " + sawOutputEOS);
                }
                synchronized (mDecoderLock) {
                    mDecoderLock.notifyAll();
                }
            }
        }

        );
        decoderThread.setDaemon(true);
        decoderThread.start();
    }
}