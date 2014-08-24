package de.ph1b.audiobook.activity;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import de.ph1b.audiobook.fragment.BookChoose;
import de.ph1b.audiobook.fragment.BookPlay;
import de.ph1b.audiobook.service.AudioPlayerService;
import de.ph1b.audiobook.utils.CommonTasks;

public class MediaView extends ActionBarActivity {

    private static final String TAG = "de.ph1b.audiobook.activity.MediaView";
    public static final String PLAY_BOOK = TAG + ".PLAY_BOOK";
    public static final String SHARED_PREFS = TAG + ".SHARED_PREFS";
    public static final String SHARED_PREFS_CURRENT = TAG + ".SHARED_PREFS_CURRENT";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // if got action from notification directly go to playing book
        Intent i = getIntent();
        if (i.getAction().equals(BookPlay.TAG)) {
            int bookId = i.getIntExtra(AudioPlayerService.BOOK_ID, -1);
            if (bookId != -1) {
                Bundle bundle = new Bundle();
                bundle.putInt(MediaView.PLAY_BOOK, bookId);
                BookPlay fragment = new BookPlay();
                fragment.setArguments(bundle);
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, fragment)
                        .addToBackStack(BookPlay.TAG)
                        .commit();
            }
        } else {
            getFragmentManager().beginTransaction()
                    .add(android.R.id.content, new BookChoose(), BookChoose.TAG)
                    .commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        //checking if external storage is available
        new CommonTasks().checkExternalStorage(this);
    }

    @Override
    public void onBackPressed() {
        int size = getFragmentManager().getBackStackEntryCount();
        if (size > 0) {
            getFragmentManager().popBackStackImmediate();
        } else if (getFragmentManager().findFragmentByTag(BookChoose.TAG) == null) {
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new BookChoose())
                    .commit();
        } else {
            super.onBackPressed();
        }

    }


}
