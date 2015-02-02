package de.ph1b.audiobook.utils;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;

import java.io.IOException;

import de.ph1b.audiobook.R;


public class BAgent extends BackupAgentHelper {

    private static final String BACKUP_KEY = "BACKUP_KEY";

    @Override
    public void onCreate() {

        String rootFolder = getString(R.string.pref_key_root_folder);
        String resumeOnReplug = getString(R.string.pref_key_resume_on_replug);
        String coverOnInternet = getString(R.string.pref_key_cover_on_internet);
        String seekTime = getString(R.string.pref_key_seek_time);
        String sleepTime = getString(R.string.pref_key_sleep_time);
        String trackToEnd = getString(R.string.pref_key_track_to_end);
        String pauseOnTransientInterrupt = getString(R.string.pref_key_pause_on_transient_interrupt);
        String playbackSpeed = Prefs.PREF_KEY_PLAYBACK_SPEED;

        SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, rootFolder,
                resumeOnReplug, coverOnInternet, seekTime, sleepTime, trackToEnd,
                pauseOnTransientInterrupt, playbackSpeed);
        addHelper(BACKUP_KEY, helper);
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
        super.onBackup(oldState, data, newState);
        L.d("bagent", "onBackup called");
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
        super.onRestore(data, appVersionCode, newState);
        L.d("bagent", "onrestore called");
    }
}
