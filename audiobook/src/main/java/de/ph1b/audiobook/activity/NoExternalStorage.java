package de.ph1b.audiobook.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.helper.CommonTasks;


public class NoExternalStorage extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_external_storage);
    }

    @Override
    public void onResume(){
        super.onResume();
        new CommonTasks().startMediaViewIfExternalStorageAvailable(this);
    }
}
