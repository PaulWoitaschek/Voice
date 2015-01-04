package de.ph1b.audiobook.utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import de.ph1b.audiobook.activity.BookChoose;

public class Prefs {

    private static final String PREF_KEY_AUDIO_DIRS = "audioDirs";

    private static SharedPreferences getSP(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c);
    }

    private static SharedPreferences.Editor getED(Context c) {
        return getSP(c).edit();
    }

    public static ArrayList<String> getAudiobookDirs(Context c) {
        SharedPreferences sp = getSP(c);
        Set<String> dirs = sp.getStringSet(PREF_KEY_AUDIO_DIRS, new HashSet<String>());
        return new ArrayList<>(dirs);
    }

    public static void saveAudiobookDirs(Context c, ArrayList<String> dirs) {
        SharedPreferences.Editor editor = getED(c);
        Set<String> save = new HashSet<>(dirs);
        editor.putStringSet(PREF_KEY_AUDIO_DIRS, save);
        editor.apply();
    }

    public static int getCurrentBookId(Context c) {
        return getSP(c).getInt(BookChoose.SHARED_PREFS_CURRENT, -1);
    }

    public static void setCurrentBookId(int bookId, Context c) {
        SharedPreferences.Editor editor = getED(c);
        editor.putInt(BookChoose.SHARED_PREFS_CURRENT, bookId);
        editor.apply();
    }
}
