package de.ph1b.audiobook.utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import de.ph1b.audiobook.R;

public class Prefs {

    private static final String PREF_KEY_PLAYBACK_SPEED = "playbackSpeed";
    private static final String PREF_KEY_CURRENT_BOOK = "currentBook";
    private final Context c;
    private final SharedPreferences sp;

    public Prefs(Context c) {
        this.c = c;
        sp = PreferenceManager.getDefaultSharedPreferences(c);
    }

    public long getCurrentBookId() {
        return sp.getLong(PREF_KEY_CURRENT_BOOK, -1);
    }

    public void setCurrentBookId(long bookId) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(PREF_KEY_CURRENT_BOOK, bookId);
        editor.apply();
    }

    public float getPlaybackSpeed() {
        return sp.getFloat(PREF_KEY_PLAYBACK_SPEED, 1);
    }

    public void setPlaybackSpeed(float playbackSpeed) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putFloat(PREF_KEY_PLAYBACK_SPEED, playbackSpeed);
        editor.apply();
    }

    public String getAudiobookFolder() {
        return sp.getString(c.getString(R.string.pref_key_root_folder), null);
    }

    public void setAudiobookFolder(String folder) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(c.getString(R.string.pref_key_root_folder), folder);
        editor.apply();
    }

    public boolean stopAfterCurrentTrack() {
        return sp.getBoolean(c.getString(R.string.pref_key_track_to_end), false);
    }

    public int getSleepTime() {
        return sp.getInt(c.getString(R.string.pref_key_sleep_time), 20);
    }

    public void setSleepTime(int time){
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(c.getString(R.string.pref_key_sleep_time), time);
        editor.apply();
    }

    public int getSeekTime() {
        return sp.getInt(c.getString(R.string.pref_key_seek_time), 20);
    }

    public void setSeekTime(int time) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(c.getString(R.string.pref_key_seek_time), time);
        editor.apply();
    }

    public boolean resumeOnReplug() {
        return sp.getBoolean(c.getString(R.string.pref_key_resume_on_replug), true);
    }

    public boolean mobileConnectionAllowed(){
        return sp.getBoolean(c.getString(R.string.pref_key_cover_on_internet), false);
    }
}
