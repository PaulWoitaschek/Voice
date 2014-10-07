package de.ph1b.audiobook.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.utils.CommonTasks;


public class BookPlay extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_play);
    }

    @Override
    public void onResume() {
        CommonTasks.checkExternalStorage(this);
        super.onResume();
    }
}