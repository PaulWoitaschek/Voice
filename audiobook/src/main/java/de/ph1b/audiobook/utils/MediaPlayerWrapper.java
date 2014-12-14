package de.ph1b.audiobook.utils;


import android.content.Context;
import android.os.Build;

import java.io.IOException;

public class MediaPlayerWrapper {

    private android.media.MediaPlayer androidMediaPlayer;
    private MediaPlayer customMediaPlayer;

    private final boolean useCustomMediaPlayer;

    public interface OnCompletionListener {
        public void onCompletion();
    }

    public MediaPlayerWrapper(Context c) {
        useCustomMediaPlayer = Build.VERSION.SDK_INT >= 16;

        if (useCustomMediaPlayer) {
            customMediaPlayer = new MediaPlayer(c);
        } else {
            androidMediaPlayer = new android.media.MediaPlayer();
        }
    }

    public float getPlaybackSpeed() {
        if (useCustomMediaPlayer) {
            return customMediaPlayer.getPlaybackSpeed();
        } else {
            return 1;
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
        if (useCustomMediaPlayer) {
            return customMediaPlayer.getCurrentPosition();
        } else {
            return androidMediaPlayer.getCurrentPosition();
        }
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
     * @param c Context
     * @param mode PowerManager flags
     */
    public void setWakeMode(Context c, int mode) {
        if (!useCustomMediaPlayer) {
            androidMediaPlayer.setWakeMode(c, mode);
        }
    }
}
