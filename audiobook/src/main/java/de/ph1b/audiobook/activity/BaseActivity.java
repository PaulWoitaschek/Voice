package de.ph1b.audiobook.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;

import de.ph1b.audiobook.service.AudioService;
import de.ph1b.audiobook.utils.ThemeUtil;

/**
 * Base class for all Activities which extends ActionBarActivity and checks in onResume, if the storage
 * is mounted. Shuts down service if not.
 */
public abstract class BaseActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeUtil.getTheme(this));
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Intent serviceIntent = new Intent(this, AudioService.class);
            stopService(serviceIntent);

            Intent i = new Intent(this, NoExternalStorageActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(new Intent(i));
        }
    }
}
