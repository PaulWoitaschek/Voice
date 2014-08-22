package de.ph1b.audiobook.activity;


import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import de.ph1b.audiobook.fragment.BookChoose;
import de.ph1b.audiobook.utils.CommonTasks;

public class MediaView extends ActionBarActivity{

    private static final String TAG = "de.ph1b.audiobook.activity.MediaView";
    public static final String PLAY_BOOK = TAG + ".PLAY_BOOK";
    public static final String SHARED_PREFS = TAG + ".SHARED_PREFS";
    public static final String SHARED_PREFS_CURRENT = TAG + ".SHARED_PREFS_CURRENT";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .add(android.R.id.content, new BookChoose())
                .addToBackStack(BookChoose.TAG)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();

        //checking if external storage is available
        new CommonTasks().checkExternalStorage(this);
    }

}
