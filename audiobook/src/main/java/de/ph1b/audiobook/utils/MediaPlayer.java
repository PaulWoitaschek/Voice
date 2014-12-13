package de.ph1b.audiobook.utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;

import de.ph1b.audiobook.R;


/**
 * Wrapper trying to mimic the behaviour of the default Android MediaPlayer. Sets wake-lock
 * automatically to android.os.PowerManager.PARTIAL_WAKE_LOCK.
 */
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

    private long cachedTime = -1;

    private InternState state = InternState.DEAD;

    private String currentMrl = null;

    private Context c;

    public void setOnCompletionListener(OnCompletionListener listener) {
        this.onCompletionListener = listener;
    }

    public MediaPlayer(final Context c) {
        Log.d(TAG, "new instance");
        this.c = c;

        try {
            vlc = LibVLC.getInstance();
            vlc.setTimeStretching(true);

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
            int hwAcceleration = Integer.valueOf(sp.getString(c.getString(R.string.pref_key_hardware_acceleration), String.valueOf(LibVLC.HW_ACCELERATION_FULL)));
            vlc.setHardwareAcceleration(hwAcceleration);
            vlc.setAout(LibVLC.AOUT_OPENSLES);

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
                    case EventHandler.HardwareAccelerationError:
                        int newHardwareAcceleration;

                        switch (vlc.getHardwareAcceleration()) {
                            case LibVLC.HW_ACCELERATION_FULL:
                                newHardwareAcceleration = LibVLC.HW_ACCELERATION_DECODING;
                                break;
                            case LibVLC.HW_ACCELERATION_DECODING:
                                newHardwareAcceleration = LibVLC.HW_ACCELERATION_AUTOMATIC;
                                break;
                            default:
                                newHardwareAcceleration = LibVLC.HW_ACCELERATION_DISABLED;
                                break;
                        }

                        vlc.setHardwareAcceleration(newHardwareAcceleration);

                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putInt(c.getString(R.string.pref_key_hardware_acceleration), newHardwareAcceleration);
                        editor.apply();

                        vlc.play();
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
        state = InternState.DEAD;
    }

    public void release() {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        state = InternState.DEAD;
    }

    public int getCurrentPosition() {
        if (state == InternState.STARTED || state == InternState.PAUSED) {
            return (int) vlc.getTime();
        } else {
            return (int) cachedTime;
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
        if (cachedTime != -1) {
            vlc.setTime(cachedTime);
            cachedTime = -1;
        }

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
