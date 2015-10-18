package de.ph1b.audiobook.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;

import javax.inject.Inject;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.service.BookReaderService;
import de.ph1b.audiobook.utils.App;

/**
 * Base class for all Activities which checks in onResume, if the storage
 * is mounted. Shuts down service if not.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Inject PrefsManager prefsManager;

    public static boolean storageMounted() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        App.getComponent().inject(this);
        setTheme(prefsManager.getTheme().getThemeId());
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!storageMounted()) {
            Intent serviceIntent = new Intent(this, BookReaderService.class);
            stopService(serviceIntent);

            Intent i = new Intent(this, NoExternalStorageActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(new Intent(i));
            return;
        }
        recreateIfThemeChanged();
    }

    public void recreateIfThemeChanged() {
        TypedValue outValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.theme_name, outValue, true);
        String oldThemeName = String.valueOf(outValue.string);
        String newName = getString(prefsManager.getTheme().getNameId());

        if (!newName.equals(oldThemeName)) {
            recreate();
        }
    }
}
