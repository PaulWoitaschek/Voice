package de.ph1b.audiobook.mediaplayer;


import android.content.Context;
import android.media.MediaPlayer;

import java.io.IOException;

interface MediaPlayerInterface {

    void release();

    void start();

    void reset();

    void prepare() throws IOException;

    void seekTo(int ms);

    int getCurrentPosition();

    void pause();

    void setPlaybackSpeed(float speed);

    @SuppressWarnings("RedundantThrows")
    void setDataSource(String source) throws IOException;

    void setOnErrorListener(MediaPlayer.OnErrorListener onErrorListener);

    void setOnCompletionListener(OnCompletionListener onCompletionListener);

    @SuppressWarnings("UnusedParameters")
    void setWakeMode(Context context, int mode);

    int getDuration();

    interface OnCompletionListener {
        void onCompletion();
    }
}
