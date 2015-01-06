package de.ph1b.audiobook.utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BookChoose;

public class Prefs {

    private static final String PREF_KEY_PLAYBACK_SPEED = "playbackSpeed";

    private static SharedPreferences getSP(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c);
    }

    private static SharedPreferences.Editor getED(Context c) {
        return getSP(c).edit();
    }

    public static int getCurrentBookId(Context c) {
        return getSP(c).getInt(BookChoose.SHARED_PREFS_CURRENT, -1);
    }

    public static void setCurrentBookId(int bookId, Context c) {
        SharedPreferences.Editor editor = getED(c);
        editor.putInt(BookChoose.SHARED_PREFS_CURRENT, bookId);
        editor.apply();
    }

    public static float getPlaybackSpeed(Context c) {
        return getSP(c).getFloat(PREF_KEY_PLAYBACK_SPEED, 1);
    }

    public static void setPlaybackSpeed(float playbackSpeed, Context c) {
        SharedPreferences.Editor editor = getED(c);
        editor.putFloat(PREF_KEY_PLAYBACK_SPEED, playbackSpeed);
        editor.apply();
    }

    public static String getAudiobookFolder(Context c) {
        return getSP(c).getString(c.getString(R.string.pref_key_root_folder), null);
    }

    public static void setAudiobookFolder(String folder, Context c) {
        SharedPreferences.Editor editor = getED(c);
        editor.putString(c.getString(R.string.pref_key_root_folder), folder);
        editor.apply();
    }
}
