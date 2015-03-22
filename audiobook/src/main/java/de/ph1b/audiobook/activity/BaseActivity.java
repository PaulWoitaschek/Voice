package de.ph1b.audiobook.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.TypedValue;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.service.AudioService;
import de.ph1b.audiobook.uitools.ThemeUtil;

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
        recreateIfThemeChanged();
    }

    public void recreateIfThemeChanged() {
        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.theme_name, outValue, true);
        String themeName = String.valueOf(outValue.string);
        String darkThemeName = getString(R.string.pref_theme_dark);
        String lightThemeName = getString(R.string.pref_theme_light);
        int settingsTheme = ThemeUtil.getTheme(this);
        if ((settingsTheme == R.style.DarkTheme && themeName.equals(lightThemeName)) || (settingsTheme == R.style.LightTheme && themeName.equals(darkThemeName))) {
            // themes have changed. recreate
            recreate();
        }
    }
}
