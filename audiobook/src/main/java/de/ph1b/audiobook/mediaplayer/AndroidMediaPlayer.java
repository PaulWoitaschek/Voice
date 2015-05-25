package de.ph1b.audiobook.mediaplayer;

import android.media.MediaPlayer;


public class AndroidMediaPlayer extends MediaPlayer implements MediaPlayerInterface {
    @Override
    public float getPlaybackSpeed() {
        return 1.0F;
    }

    @Override
    public void setPlaybackSpeed(float speed) {
        // ignore since android mediaPlayer is not able to do this
    }

    @Override
    public void setOnCompletionListener(final MediaPlayerInterface.OnCompletionListener listener) {
        setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                listener.onCompletion();
            }
        });
    }
}
