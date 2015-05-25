package de.ph1b.audiobook.service;

import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

/**
 * @author <a href="mailto:woitaschek@posteo.de">Paul Woitaschek</a>
 * @link {http://www.paul-woitaschek.de}
 * @see <a href="http://www.paul-woitaschek.de">http://www.paul-woitaschek.de</a>
 */
public class ServiceController {

    public static final String CONTROL_SET_PLAYBACK_SPEED = "CONTROL_SET_PLAYBACK_SPEED";
    public static final String CONTROL_SET_PLAYBACK_SPEED_EXTRA_SPEED = "CONTROL_SET_PLAYBACK_SPEED_EXTRA_SPEED";

    public static final String CONTROL_TOGGLE_SLEEP_SAND = "CONTROL_TOGGLE_SLEEP_SAND";
    public static final String CONTROL_CHANGE_POSITION = "CONTROL_CHANGE_POSITION";
    public static final String CONTROL_CHANGE_POSITION_EXTRA_TIME = "CONTROL_CHANGE_POSITION_EXTRA_TIME";
    public static final String CONTROL_CHANGE_POSITION_EXTRA_PATH_RELATIVE = "CONTROL_CHANGE_POSITION_EXTRA_PATH_RELATIVE";
    public static final String CONTROL_NEXT = "CONTROL_NEXT";
    public static final String CONTROL_PREVIOUS = "CONTROL_PREVIOUS";
    private final Context c;

    public ServiceController(Context c) {
        this.c = c;
    }

    public static Intent getStopIntent(Context c) {
        Intent intent = new Intent(c, AudioService.class);
        intent.setAction(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STOP);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
        return intent;
    }

    public static Intent getPlayPauseIntent(Context c) {
        Intent intent = new Intent(c, AudioService.class);
        intent.setAction(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
        return intent;
    }

    public static Intent getFastForwardIntent(Context c) {
        Intent intent = new Intent(c, AudioService.class);
        intent.setAction(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
        return intent;
    }

    public static Intent getRewindIntent(Context c) {
        Intent intent = new Intent(c, AudioService.class);
        intent.setAction(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_REWIND);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
        return intent;
    }

    public void setPlaybackSpeed(float speed) {
        Intent i = new Intent(c, AudioService.class);
        i.setAction(CONTROL_SET_PLAYBACK_SPEED);
        i.putExtra(CONTROL_SET_PLAYBACK_SPEED_EXTRA_SPEED, speed);
        c.startService(i);
    }

    public void changeTime(int time, String relativePath) {
        Intent intent = new Intent(c, AudioService.class);
        intent.setAction(CONTROL_CHANGE_POSITION);
        intent.putExtra(CONTROL_CHANGE_POSITION_EXTRA_TIME, time);
        intent.putExtra(CONTROL_CHANGE_POSITION_EXTRA_PATH_RELATIVE, relativePath);
        c.startService(intent);
    }

    public void playPause() {
        c.startService(getPlayPauseIntent(c));
    }

    public void fastForward() {
        c.startService(getFastForwardIntent(c));
    }

    public void rewind() {
        c.startService(getRewindIntent(c));
    }

    public void next() {
        Intent intent = new Intent(c, AudioService.class);
        intent.setAction(CONTROL_NEXT);
        c.startService(intent);
    }

    public void previous() {
        Intent intent = new Intent(c, AudioService.class);
        intent.setAction(CONTROL_PREVIOUS);
        c.startService(intent);
    }

    public void toggleSleepSand() {
        Intent intent = new Intent(c, AudioService.class);
        intent.setAction(CONTROL_TOGGLE_SLEEP_SAND);
        c.startService(intent);
    }
}
