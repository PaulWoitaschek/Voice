package de.ph1b.audiobook.utils;


import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;

public class MediaPlayer {

    private enum InternState {
        PREPARED,
        PAUSED,
        STARTED,
        DEAD,
        COMPLETED,
    }

    public interface OnCompletionListener {
        public void onCompletion();
    }

    private float playBackSpeed = 1;

    private static final String TAG = "MediaPlayer";

    private OnCompletionListener onCompletionListener = null;

    private LibVLC vlc;

    private final PowerManager.WakeLock wakeLock;

    private long cachedTime = 0;

    private InternState state = InternState.DEAD;

    private String currentMrl = null;

    public void setOnCompletionListener(OnCompletionListener listener) {
        this.onCompletionListener = listener;
    }

    public MediaPlayer(Context c) {
        Log.d(TAG, "new instance");
        try {
            vlc = LibVLC.getInstance();
            vlc.setTimeStretching(true);
            vlc.setHardwareAcceleration(LibVLC.HW_ACCELERATION_DECODING);
            vlc.init(c);
        } catch (LibVlcException e) {
            e.printStackTrace();
        }

        EventHandler eventHandler = EventHandler.getInstance();
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                switch (msg.getData().getInt("event")) {
                    case EventHandler.MediaPlayerEndReached:
                        Log.d(TAG, "end reached");
                        state = InternState.COMPLETED;
                        if (onCompletionListener != null) {
                            onCompletionListener.onCompletion();
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        eventHandler.addHandler(handler);

        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    }

    public void prepare(String path) {
        Log.d(TAG, "preparing");
        currentMrl = LibVLC.PathToURI(path);
        state = InternState.PREPARED;
    }

    public void setPlayBackSpeed(float playBackSpeed) {
        this.playBackSpeed = playBackSpeed;
        vlc.setRate(playBackSpeed);
    }

    public float getPlayBackSpeed() {
        return playBackSpeed;
    }

    public void reset() {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    public void release() {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        state = InternState.DEAD;
    }

    public int getCurrentPosition() {
        if (state == InternState.PREPARED) {
            return (int) cachedTime;
        } else {
            long vlcTime = vlc.getTime();
            return (int) (vlcTime == 0 ? cachedTime : vlcTime); // because vlc is buggy, we have to return the cached time
        }
    }

    public void seekTo(long position) {
        vlc.setTime(position);
        cachedTime = position;
    }


    public void start() {
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        if (state == InternState.PREPARED) {
            vlc.playMRL(currentMrl);
        } else {
            vlc.play();
        }
        vlc.setRate(playBackSpeed);
        vlc.setTime(cachedTime);

        state = InternState.STARTED;
    }


    public void pause() {
        vlc.pause();
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        state = InternState.PAUSED;
    }
}
