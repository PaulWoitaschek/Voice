package de.ph1b.audiobook.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import de.ph1b.audiobook.R;

public class PrefsManager {

    private static final String PREF_KEY_CURRENT_BOOK = "currentBook";
    private static final String PREF_KEY_COLLECTION_FOLDERS = "folders";
    private static final String PREF_KEY_SINGLE_BOOK_FOLDERS = "singleBookFolders";
    private final Context c;
    private final SharedPreferences sp;

    public PrefsManager(Context c) {
        this.c = c;
        sp = PreferenceManager.getDefaultSharedPreferences(c);
    }

    public long getCurrentBookId() {
        return sp.getLong(PREF_KEY_CURRENT_BOOK, -1);
    }

    public void setCurrentBookId(long bookId) {
        sp.edit().putLong(PREF_KEY_CURRENT_BOOK, bookId)
                .apply();
    }

    @NonNull
    public ArrayList<String> getCollectionFolders() {
        Set<String> set = sp.getStringSet(PREF_KEY_COLLECTION_FOLDERS, new HashSet<String>());
        return new ArrayList<>(set);
    }

    public void setCollectionFolders(@NonNull ArrayList<String> folders) {
        Set<String> set = new HashSet<>();
        set.addAll(folders);
        SharedPreferences.Editor editor = sp.edit();
        editor.putStringSet(PREF_KEY_COLLECTION_FOLDERS, set);
        editor.apply();
    }

    @NonNull
    public ArrayList<String> getSingleBookFolders() {
        Set<String> set = sp.getStringSet(PREF_KEY_SINGLE_BOOK_FOLDERS, new HashSet<String>());
        return new ArrayList<>(set);
    }

    public void setSingleBookFolders(@NonNull ArrayList<String> folders) {
        Set<String> set = new HashSet<>();
        set.addAll(folders);
        SharedPreferences.Editor editor = sp.edit();
        editor.putStringSet(PREF_KEY_SINGLE_BOOK_FOLDERS, set);
        editor.apply();
    }

    public boolean stopAfterCurrentTrack() {
        return sp.getBoolean(c.getString(R.string.pref_key_track_to_end), false);
    }

    public int getSleepTime() {
        return sp.getInt(c.getString(R.string.pref_key_sleep_time), 20);
    }

    public void setSleepTime(int time) {
        sp.edit().putInt(c.getString(R.string.pref_key_sleep_time), time)
                .apply();
    }

    public int getSeekTime() {
        return sp.getInt(c.getString(R.string.pref_key_seek_time), 20);
    }

    public void setSeekTime(int time) {
        sp.edit().putInt(c.getString(R.string.pref_key_seek_time), time)
                .apply();
    }

    public boolean resumeOnReplug() {
        return sp.getBoolean(c.getString(R.string.pref_key_resume_on_replug), true);
    }

    public boolean pauseOnTempFocusLoss() {
        return sp.getBoolean(c.getString(R.string.pref_key_pause_on_can_duck), false);
    }

    public int getAutoRewindAmount() {
        return sp.getInt(c.getString(R.string.pref_key_auto_rewind), 2);
    }

    public void setAutoRewindAmount(int autoRewindAmount) {
        sp.edit().putInt(c.getString(R.string.pref_key_auto_rewind), autoRewindAmount)
                .apply();
    }
}
