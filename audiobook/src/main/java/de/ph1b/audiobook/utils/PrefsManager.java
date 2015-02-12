package de.ph1b.audiobook.utils;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import de.ph1b.audiobook.R;

public class PrefsManager {

    private static final String PREF_KEY_CURRENT_BOOK = "currentBook";
    private final Context c;
    private final SharedPreferences sp;

    public PrefsManager(Context c) {
        this.c = c;
        sp = PreferenceManager.getDefaultSharedPreferences(c);
    }

    public long getCurrentBookId() {
        return sp.getLong(PREF_KEY_CURRENT_BOOK, -1);
    }

    @SuppressLint("CommitPrefEdits")
    public void setCurrentBookId(long bookId) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(PREF_KEY_CURRENT_BOOK, bookId);
        /**
         * We do a commit instead of apply, because when we set the prefs, we directly start a new
         * activity when {@link de.ph1b.audiobook.fragment.BookShelfFragment} ->
         * {@link de.ph1b.audiobook.fragment.BookPlayFragment}
         * directly.
         */
        editor.commit();
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

    public void setSleepTime(int time) {
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

    public boolean mobileConnectionAllowed() {
        return sp.getBoolean(c.getString(R.string.pref_key_cover_on_internet), false);
    }

    public boolean pauseOnTransientAudioFocusLoss() {
        return sp.getBoolean(c.getString(R.string.pref_key_pause_on_transient_interrupt), false);
    }
}
