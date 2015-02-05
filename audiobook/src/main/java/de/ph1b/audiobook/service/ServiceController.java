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

    public static final String CONTROL_CHANGE_BOOK_POSITION = "CONTROL_CHANGE_BOOK_POSITION";
    public static final String CONTROL_CHANGE_BOOK_POSITION_EXTRA_MEDIA_POSITION = "CONTROL_CHANGE_BOOK_POSITION_EXTRA_MEDIA_POSITION";
    public static final String CONTROL_CHANGE_BOOK_POSITION_EXTRA_MEDIA_TIME = "CONTROL_CHANGE_BOOK_POSITION_EXTRA_MEDIA_TIME";
    public static final String CONTROL_TOGGLE_SLEEP_SAND = "CONTROL_TOGGLE_SLEEP_SAND";
    public static final String CONTROL_CHANGE_TIME = "CONTROL_CHANGE_TIME";
    public static final String CONTROL_CHANGE_TIME_EXTRA = "CONTROL_CHANGE_TIME_EXTRA";
    private final Context c;

    public ServiceController(Context c) {
        this.c = c;
    }

    public static Intent getPlayPauseIntent(Context c) {
        Intent intent = new Intent(c, AudioPlayerService.class);
        intent.setAction(Intent.ACTION_MEDIA_BUTTON);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        return intent;
    }

    public static Intent getFastForwardIntent(Context c) {
        Intent intent = new Intent(c, AudioPlayerService.class);
        intent.setAction(Intent.ACTION_MEDIA_BUTTON);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
        return intent;
    }

    public static Intent getRewindIntent(Context c) {
        Intent intent = new Intent(c, AudioPlayerService.class);
        intent.setAction(Intent.ACTION_MEDIA_BUTTON);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.KEYCODE_MEDIA_REWIND);
        return intent;
    }

    private static Intent getNextIntent(Context c) {
        Intent intent = new Intent(c, AudioPlayerService.class);
        intent.setAction(Intent.ACTION_MEDIA_BUTTON);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.KEYCODE_MEDIA_NEXT);
        return intent;
    }

    public static Intent getStopIntent(Context c) {
        Intent intent = new Intent(c, AudioPlayerService.class);
        intent.setAction(Intent.ACTION_MEDIA_BUTTON);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.KEYCODE_MEDIA_STOP);
        return intent;
    }

    public void setPlaybackSpeed(float speed) {
        Intent i = new Intent(c, AudioPlayerService.class);
        i.setAction(CONTROL_SET_PLAYBACK_SPEED);
        i.putExtra(CONTROL_SET_PLAYBACK_SPEED_EXTRA_SPEED, speed);
        c.startService(i);
    }

    public void changeBookPosition(int position, int time) {
        Intent intent = new Intent(c, AudioPlayerService.class);
        intent.setAction(CONTROL_CHANGE_BOOK_POSITION);
        intent.putExtra(CONTROL_CHANGE_BOOK_POSITION_EXTRA_MEDIA_POSITION, position);
        intent.putExtra(CONTROL_CHANGE_BOOK_POSITION_EXTRA_MEDIA_TIME, time);
        c.startService(intent);
    }

    public void changeTime(int time) {
        Intent intent = new Intent(c, AudioPlayerService.class);
        intent.setAction(CONTROL_CHANGE_TIME);
        intent.putExtra(CONTROL_CHANGE_TIME_EXTRA, time);
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
        c.startService(getNextIntent(c));
    }

    public void previous() {
        Intent intent = new Intent(c, AudioPlayerService.class);
        intent.setAction(Intent.ACTION_MEDIA_BUTTON);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        c.startService(intent);
    }

    public void toggleSleepSand() {
        Intent intent = new Intent(c, AudioPlayerService.class);
        intent.setAction(CONTROL_TOGGLE_SLEEP_SAND);
        c.startService(intent);
    }
}
