package de.ph1b.audiobook.utils;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

import de.ph1b.audiobook.R;

public class CustomBackupAgent extends BackupAgentHelper {

    private static final String BACKUP_KEY = "BACKUP_KEY";

    @Override
    public void onCreate() {

        String resumeOnReplug = getString(R.string.pref_key_resume_on_replug);
        String seekTime = getString(R.string.pref_key_seek_time);
        String sleepTime = getString(R.string.pref_key_sleep_time);
        String trackToEnd = getString(R.string.pref_key_track_to_end);
        String theme = getString(R.string.pref_key_theme);
        String pauseOnCanDuck = getString(R.string.pref_key_pause_on_can_duck);

        SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this,
                resumeOnReplug, seekTime, sleepTime, trackToEnd, theme, pauseOnCanDuck);
        addHelper(BACKUP_KEY, helper);
    }
}
