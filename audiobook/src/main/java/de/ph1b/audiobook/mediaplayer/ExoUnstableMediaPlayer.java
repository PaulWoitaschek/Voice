package de.ph1b.audiobook.mediaplayer;


import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.source.DefaultSampleSource;
import com.google.android.exoplayer.source.FrameworkSampleExtractor;

import java.io.File;
import java.io.IOException;

public class ExoUnstableMediaPlayer implements MediaPlayerInterface, ExoPlayer.Listener {

    private final ExoPlayer exoPlayer = ExoPlayer.Factory.newInstance(1);
    private State state;
    private OnCompletionListener onCompletionListener = null;
    private MediaPlayer.OnErrorListener onErrorListener = null;
    private String currentPath = null;
    private Context c;

    public ExoUnstableMediaPlayer(Context c) {
        state = State.IDLE;
        this.c = c;

        exoPlayer.addListener(this);
    }

    @Override
    public void start() {
        switch (state) {
            case PREPARED:
            case STARTED:
            case PAUSED:
            case PLAYBACK_COMPLETED:
                exoPlayer.setPlayWhenReady(true);
                state = State.STARTED;
                break;
            default:
                state = State.ERROR;
                throw new IllegalStateException("start must not be called in state=" + state);
        }
    }

    @Override
    public void reset() {
        exoPlayer.setPlayWhenReady(false);
        state = State.IDLE;
    }

    @Override
    public void release() {
        exoPlayer.removeListener(this);
        exoPlayer.release();
        state = State.DEAD;
    }

    @Override
    public void prepare() throws IOException {
        switch (state) {
            case INITIALIZED:
            case STOPPED:
                DefaultSampleSource sampleSource = new DefaultSampleSource(new FrameworkSampleExtractor(c, Uri.fromFile(new File(currentPath)), null), 2);
                TrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource);
                exoPlayer.prepare(audioRenderer);
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
                long seekPosition = exoPlayer.getDuration() == ExoPlayer.UNKNOWN_TIME ? 0
                        : Math.min(Math.max(0, ms), getDuration());
                exoPlayer.seekTo(seekPosition);
                break;
            default:
                state = State.ERROR;
                throw new IllegalStateException("seekTo must not be called in state=" + state);
        }
    }

    @Override
    public int getDuration() {
        return exoPlayer.getDuration() == ExoPlayer.UNKNOWN_TIME ? 0 : (int) exoPlayer.getDuration();
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
                return exoPlayer.getCurrentPosition() == ExoPlayer.UNKNOWN_TIME ? 0 : (int) exoPlayer.getCurrentPosition();
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
                exoPlayer.setPlayWhenReady(false);
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
                currentPath = source;
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

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == ExoPlayer.STATE_ENDED && onCompletionListener != null) {
            onCompletionListener.onCompletion(this);
        }
    }

    @Override
    public void onPlayWhenReadyCommitted() {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
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
