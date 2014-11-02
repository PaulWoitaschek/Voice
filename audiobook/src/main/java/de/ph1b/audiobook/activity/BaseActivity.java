package de.ph1b.audiobook.activity;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;


public abstract class BaseActivity extends ActionBarActivity {

    @Override
    protected void onResume() {
        super.onResume();
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Intent i = new Intent(this, NoExternalStorage.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(new Intent(i));
        }
    }
}
