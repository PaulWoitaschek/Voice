package de.ph1b.audiobook.mediaplayer;


import android.content.Context;
import android.os.Build;

import java.io.IOException;

public class MediaPlayerCompat {

    private final boolean useCustomMediaPlayer;
    private android.media.MediaPlayer androidMediaPlayer;
    private MediaPlayer customMediaPlayer;

    public MediaPlayerCompat(Context c) {
        useCustomMediaPlayer = Build.VERSION.SDK_INT >= 16;

        if (useCustomMediaPlayer) {
            customMediaPlayer = new MediaPlayer(c);
        } else {
            androidMediaPlayer = new android.media.MediaPlayer();
        }
    }

    public void setPlaybackSpeed(float speed) {
        if (useCustomMediaPlayer) {
            customMediaPlayer.setPlaybackSpeed(speed);
        }
    }

    public void reset() {
        if (useCustomMediaPlayer) {
            customMediaPlayer.reset();
        } else {
            androidMediaPlayer.reset();
        }
    }

    public int getDuration() {
        return (useCustomMediaPlayer ? customMediaPlayer.getDuration() : androidMediaPlayer.getDuration());
    }

    public void release() {
        if (useCustomMediaPlayer) {
            customMediaPlayer.release();
        } else {
            androidMediaPlayer.release();
        }
    }

    public void prepare() {
        if (useCustomMediaPlayer) {
            customMediaPlayer.prepare();
        } else {
            try {
                androidMediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setDataSource(String path) {
        if (useCustomMediaPlayer) {
            customMediaPlayer.setDataSource(path);
        } else {
            try {
                androidMediaPlayer.setDataSource(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void seekTo(int position) {
        if (useCustomMediaPlayer) {
            customMediaPlayer.seekTo(position);
        } else {
            androidMediaPlayer.seekTo(position);
        }
    }

    public void setOnCompletionListener(final OnCompletionListener listener) {
        if (useCustomMediaPlayer) {
            customMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion() {
                    listener.onCompletion();
                }
            });
        } else {
            androidMediaPlayer.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(android.media.MediaPlayer mp) {
                    listener.onCompletion();
                }
            });
        }
    }

    public int getCurrentPosition() {
        return (useCustomMediaPlayer ? customMediaPlayer.getCurrentPosition() : androidMediaPlayer.getCurrentPosition());
    }

    public void start() {
        if (useCustomMediaPlayer) {
            customMediaPlayer.start();
        } else {
            androidMediaPlayer.start();
        }
    }

    public void pause() {
        if (useCustomMediaPlayer) {
            customMediaPlayer.pause();
        } else {
            androidMediaPlayer.pause();
        }
    }

    /**
     * Sets wakeMode. Custom Mediaplayer handles wake mode on its own.
     *
     * @param c    Context
     * @param mode PowerManager flags
     */
    public void setWakeMode(Context c, int mode) {
        if (!useCustomMediaPlayer) {
            androidMediaPlayer.setWakeMode(c, mode);
        }
    }


    public interface OnCompletionListener {
        public void onCompletion();
    }
}
