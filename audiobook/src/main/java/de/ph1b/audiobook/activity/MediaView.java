package de.ph1b.audiobook.activity;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.fragment.MediaChooserFragment;
import de.ph1b.audiobook.fragment.SettingsFragment;
import de.ph1b.audiobook.helper.CommonTasks;

public class MediaView extends ActionBarActivity {

    private static final String TAG = "de.ph1b.audiobook.activity.MediaView";
    public static final String PLAY_BOOK = TAG + ".PLAY_BOOK";
    public static final String SHARED_PREFS = TAG + ".SHARED_PREFS";
    public static final String SHARED_PREFS_CURRENT = TAG + ".SHARED_PREFS_CURRENT";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        MediaChooserFragment fragment = new MediaChooserFragment();
        fragmentTransaction.add(android.R.id.content, fragment);
        fragmentTransaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_media_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_add:
                startActivity(new Intent(this, MediaAdd.class));
                return true;
            case R.id.action_settings:
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                fragmentTransaction.replace(android.R.id.content, new SettingsFragment());
                fragmentTransaction.commit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        //checking if external storage is available
        new CommonTasks().checkExternalStorage(this);
    }

}
