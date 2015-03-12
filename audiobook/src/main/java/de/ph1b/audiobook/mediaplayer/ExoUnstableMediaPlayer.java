package de.ph1b.audiobook.mediaplayer;


import android.content.Context;
import android.media.MediaPlayer;

import java.io.IOException;

public class ExoUnstableMediaPlayer implements MediaPlayerInterface {

    private State state;
    private OnCompletionListener onCompletionListener = null;

    private MediaPlayer.OnErrorListener onErrorListener = null;

    public ExoUnstableMediaPlayer() {
        state = State.IDLE;

        // Uri uri = Uri.fromFile(new File("/storage/sdcard1/test.mp3"));
        // DefaultSampleSource sampleSource = new DefaultSampleSource(new FrameworkSampleExtractor(BookShelfActivity.this, uri, null), 2);
        //  TrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource);
        // ExoPlayer exoPlayer = ExoPlayer.Factory.newInstance(1);
        // exoPlayer.prepare(audioRenderer);
        //  exoPlayer.setPlayWhenReady(true);
    }

    @Override
    public void start() {
        switch (state) {
            case PREPARED:
            case STARTED:
            case PAUSED:
            case PLAYBACK_COMPLETED:
                state = State.STARTED;
                break;
            default:
                state = State.ERROR;
                throw new IllegalStateException("start must not be called in state=" + state);
        }
    }

    @Override
    public void reset() {
        state = State.IDLE;
    }

    @Override
    public void release() {
        state = State.DEAD;
    }

    @Override
    public void prepare() throws IOException {
        switch (state) {
            case INITIALIZED:
            case STOPPED:
                state = State.PREPARED;
                break;
            default:
                state = State.ERROR;
                throw new IllegalStateException("prepare must not be called in state=" + state);
        }
    }

    @Override
    public void seekTo(int ms) {
        switch (state) {
            case PREPARED:
            case STARTED:
            case PAUSED:
            case PLAYBACK_COMPLETED:
                break;
            default:
                state = State.ERROR;
                throw new IllegalStateException("seekTo must not be called in state=" + state);
        }
    }

    @Override
    public int getCurrentPosition() {
        switch (state) {
            case IDLE:
            case INITIALIZED:
            case PREPARED:
            case STARTED:
            case PAUSED:
            case STOPPED:
            case PLAYBACK_COMPLETED:
                return 0;
            default:
                state = State.ERROR;
                throw new IllegalStateException("getCurrentPosition must not be called in state=" + state);
        }
    }

    @Override
    public void pause() {
        switch (state) {
            case STARTED:
            case PAUSED:
            case PLAYBACK_COMPLETED:
                state = State.PAUSED;
                break;
            default:
                state = State.ERROR;
                throw new IllegalStateException("pause must not be called in state=" + state);
        }
    }

    @Override
    public void setPlaybackSpeed(float speed) {

    }

    @Override
    public void setDataSource(String source) throws IOException {
        switch (state) {
            case IDLE:
                state = State.INITIALIZED;
                break;
            default:
                state = State.ERROR;
                throw new IllegalStateException("setDataSource must not be called in state=" + state);
        }
    }

    @Override
    public void setOnErrorListener(MediaPlayer.OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
    }

    @Override
    public void setOnCompletionListener(OnCompletionListener onCompletionListener) {
        this.onCompletionListener = onCompletionListener;
    }

    @Override
    public void setWakeMode(Context context, int mode) {

    }

    private enum State {
        IDLE,
        INITIALIZED,
        STARTED,
        PAUSED,
        PREPARED,
        PLAYBACK_COMPLETED,
        ERROR,
        STOPPED,
        DEAD
    }
}
