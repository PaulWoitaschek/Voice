package de.ph1b.audiobook.activity;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.FrameLayout;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.fragment.BookPlayFragment;
import de.ph1b.audiobook.fragment.BookShelfFragment;
import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.PrefsManager;

public class BookActivity extends BaseActivity {

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

        View view = new FrameLayout(this);
        view.setId(R.id.content);
        setContentView(view);

        boolean addPlayFragment = false;
        if (getIntent().hasExtra(TARGET_FRAGMENT)) {
            String fragmentTag = getIntent().getStringExtra(TARGET_FRAGMENT);
            if (fragmentTag.equals(BookPlayFragment.TAG)) {
                addPlayFragment = true;
            } else {
                throw new AssertionError("Fragment tag not implemented:" + fragmentTag);
            }
        }

        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            if (addPlayFragment) {
                ft.replace(R.id.content, BookPlayFragment.newInstance(PrefsManager.getInstance(this).getCurrentBookId()), BookPlayFragment.TAG);
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
            ft.addToBackStack(null).commit();
        }
    }

    /**
     * If we are in {@link de.ph1b.audiobook.fragment.BookShelfFragment}, call {@link #finish()}.
     * Else reuse {@link de.ph1b.audiobook.fragment.BookShelfFragment} if available or create a new
     * one and start it.
     * {@inheritDoc}
     */
    @Override
    public void onBackPressed() {
        L.d(TAG, "onBackPressed called");
        FragmentManager fm = getSupportFragmentManager();

        Fragment bookShelfFragment = fm.findFragmentByTag(BookShelfFragment.TAG);

        if (bookShelfFragment != null && bookShelfFragment.isVisible()) {
            finish();
        } else {
            if (bookShelfFragment == null) {
                fm.beginTransaction().replace(R.id.content, new BookShelfFragment(), BookShelfFragment.TAG)
                        .addToBackStack(null).commit();
            } else {
                super.onBackPressed();
            }
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        L.d(TAG, "onSaveInstanceState called");
    }
}
