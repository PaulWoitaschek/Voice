package de.ph1b.audiobook.service;


import android.content.Context;

import java.util.concurrent.locks.ReentrantLock;

import de.ph1b.audiobook.utils.MediaPlayerCompat;

public class MediaPlayerWrapper {

    private final ReentrantLock playerLock = new ReentrantLock();
    private MediaPlayerCompat mediaPlayer;


    public MediaPlayerWrapper(Context c) {
        mediaPlayer = new MediaPlayerCompat(c);
    }

    public void setPlaybackSpeed(final long speed) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                playerLock.lock();
                try {
                    mediaPlayer.setPlaybackSpeed(speed);
                } finally {
                    playerLock.unlock();
                }
            }
        }).start();
    }

    public float getPlaybackSpeed() {
        playerLock.lock();
        float speed = 0;
        try {
            speed = mediaPlayer.getPlaybackSpeed();
        } finally {
            playerLock.unlock();
        }
        return speed;
    }
}
