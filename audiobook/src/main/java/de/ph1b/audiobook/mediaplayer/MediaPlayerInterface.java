package de.ph1b.audiobook.mediaplayer;


import android.content.Context;
import android.media.MediaPlayer;

import java.io.IOException;

interface MediaPlayerInterface {

    public void start();

    public void reset();

    public void release();

    public void prepare() throws IOException;

    public void seekTo(int ms);

    public int getCurrentPosition();

    public void pause();

    public void setPlaybackSpeed(float speed);

    @SuppressWarnings("RedundantThrows")
    public void setDataSource(String source) throws IOException;

    public void setOnErrorListener(MediaPlayer.OnErrorListener onErrorListener);

    public void setOnCompletionListener(OnCompletionListener onCompletionListener);

    @SuppressWarnings("UnusedParameters")
    public void setWakeMode(Context context, int mode);

    public int getDuration();

    public interface OnCompletionListener {
        public void onCompletion();
    }
}
