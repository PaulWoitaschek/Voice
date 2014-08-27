package de.ph1b.audiobook.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.utils.CommonTasks;


public class FilesAdd extends ActionBarActivity {

    private Bundle extras;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.extras = getIntent().getExtras();
        setContentView(R.layout.activity_files_add);
    }

    public Bundle getExtras() {
        return extras;
    }

    @Override
    public void onResume() {
        CommonTasks.checkExternalStorage(this);
        super.onResume();
    }
}
