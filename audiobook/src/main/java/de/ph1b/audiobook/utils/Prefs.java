package de.ph1b.audiobook.utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Prefs {

    private static final String PREF_KEY_AUDIO_DIRS = "audioDirs";

    public static ArrayList<String> getAudiobookDirs(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        Set<String> dirs = sp.getStringSet(PREF_KEY_AUDIO_DIRS, new HashSet<String>());
        return new ArrayList<>(dirs);
    }

    public static void saveAudiobookDirs(Context c, ArrayList<String> dirs) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = sp.edit();
        Set<String> save = new HashSet<>(dirs);
        editor.putStringSet(PREF_KEY_AUDIO_DIRS, save);
        editor.apply();
    }
}
