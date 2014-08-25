package de.ph1b.audiobook.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.utils.CommonTasks;


public class FilesAdd extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files_add);
    }

    @Override
    public void onResume() {
        super.onResume();
        //checking if external storage is available
        new CommonTasks().checkExternalStorage(this);
    }
}
