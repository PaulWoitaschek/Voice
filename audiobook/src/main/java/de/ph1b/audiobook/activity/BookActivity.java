package de.ph1b.audiobook.activity;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.FrameLayout;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.fragment.BookPlayFragment;
import de.ph1b.audiobook.fragment.BookShelfFragment;
import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.utils.L;

public class BookActivity extends ActionBarActivity {

    public static final String TARGET_FRAGMENT = "targetFragment";
    private static final String TAG = BookActivity.class.getSimpleName();

    public static Intent bookScreenIntent(Context c) {
        Intent intent = new Intent(c, BookActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean addPlayFragment = false;
        if (getIntent().hasExtra(TARGET_FRAGMENT)) {
            String fragmentTag = getIntent().getStringExtra(TARGET_FRAGMENT);
            if (fragmentTag.equals(BookPlayFragment.TAG)) {
                addPlayFragment = true;
            } else {
                throw new AssertionError("Fragment tag not implemented:" + fragmentTag);
            }
        }

        View view = new FrameLayout(this);
        view.setId(R.id.content);
        setContentView(view);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (addPlayFragment) {
            ft.replace(R.id.content, new BookPlayFragment(), BookPlayFragment.TAG);
        } else {
            Fragment bookShelfFragment = new BookShelfFragment();

            if (getIntent().hasExtra(MediaPlayerController.MALFORMED_FILE)) {
                Bundle args = new Bundle();
                String malformedFile = getIntent().getStringExtra(MediaPlayerController.MALFORMED_FILE);
                args.putString(MediaPlayerController.MALFORMED_FILE, malformedFile);
                bookShelfFragment.setArguments(args);
            }

            ft.replace(R.id.content, bookShelfFragment, BookShelfFragment.TAG);
        }
        ft.commit();
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        L.d(TAG, "onBackPressed with backStackEntryCount=" + fm.getBackStackEntryCount());
        Fragment bookPlayFragment = fm.findFragmentByTag(BookPlayFragment.TAG);
        if (bookPlayFragment != null && bookPlayFragment.isVisible()) {
            fm.beginTransaction().replace(R.id.content, new BookShelfFragment(), BookShelfFragment.TAG).commit();
        } else {
            super.onBackPressed();
        }
    }
}
