package de.ph1b.audiobook.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.base.MoreObjects;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.fragment.BookPlayFragment;
import de.ph1b.audiobook.fragment.BookShelfFragment;
import de.ph1b.audiobook.interfaces.MultiPaneInformer;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.utils.L;

/**
 * Activity that coordinates the book shelf and play screens.
 *
 * @author Paul Woitaschek
 */
public class BookActivity extends BaseActivity implements BookShelfFragment.BookSelectionCallback, MultiPaneInformer {

    private static final String TAG = BookActivity.class.getSimpleName();
    private static final String FM_BOOK_SHELF = TAG + BookShelfFragment.TAG;
    private static final String FM_BOOK_PLAY = TAG + BookPlayFragment.TAG;
    @IdRes
    private static final int BASE_CONTAINER_ID = R.id.base_container;
    @IdRes
    private static final int ADDITIONAL_CONTAINER_ID = R.id.additional_container;
    private static final String NI_MALFORMED_FILE = "malformedFile";
    private static final String NI_GO_TO_BOOK = "niGotoBook";
    private boolean multiPane = false;

    public static Intent malformedFileIntent(Context c, File malformedFile) {
        Intent intent = new Intent(c, BookActivity.class);
        intent.putExtra(NI_MALFORMED_FILE, malformedFile);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static Intent goToBookIntent(Context c, long bookId) {
        Intent intent = new Intent(c, BookActivity.class);
        intent.putExtra(NI_GO_TO_BOOK, bookId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    public boolean isMultiPane() {
        return multiPane;
    }

    /**
     * Makes sure a fragment of the same class is in the container. If it is not, set the instance
     * provided as the new fragment.
     *
     * @param container   The id of the container for the fragment
     * @param fragmentTag The fragment to identify the fragment by
     * @param newInstance A new instance of the fragment if there is none.
     */
    private void makeSureFragmentIsInContainer(@IdRes int container, @NonNull String fragmentTag, @NonNull Fragment newInstance) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment containingFragment = fm.findFragmentById(container);
        if (containingFragment == null || (!(newInstance.getClass().isInstance(containingFragment)))) {
            Fragment fragmentByTag = MoreObjects.firstNonNull(fm.findFragmentByTag(fragmentTag), newInstance);
            fm.beginTransaction().replace(container, fragmentByTag, fragmentTag)
                    .commit();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PrefsManager prefs = PrefsManager.getInstance(this);

        setContentView(R.layout.activity_book);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        multiPane = findViewById(ADDITIONAL_CONTAINER_ID) != null;
        L.i(TAG, "multiPane=" + multiPane);

        // if we are in multipane make sure every fragment is in place.
        if (multiPane) {
            makeSureFragmentIsInContainer(BASE_CONTAINER_ID, FM_BOOK_SHELF, new BookShelfFragment());
            makeSureFragmentIsInContainer(ADDITIONAL_CONTAINER_ID, FM_BOOK_PLAY,
                    BookPlayFragment.newInstance(prefs.getCurrentBookId()));
        } // if we are not in multipane, only react if there is no fragment in the container
        else if (getSupportFragmentManager().findFragmentById(BASE_CONTAINER_ID) == null) {
            L.i(TAG, "There is no fragment in the baseContainer.");
            // use the bookplay fragment if it exists
            Fragment bookPlayFragment = getSupportFragmentManager().findFragmentByTag(FM_BOOK_PLAY);
            if (bookPlayFragment != null) {
                makeSureFragmentIsInContainer(BASE_CONTAINER_ID, FM_BOOK_PLAY, bookPlayFragment);
            } // else use a bookshelf fragment
            else {
                makeSureFragmentIsInContainer(BASE_CONTAINER_ID, FM_BOOK_SHELF, new BookShelfFragment());
            }
        } else {
            L.i(TAG, "There is a fragment in the baseContainer");
            Fragment obsoleteFragment = getSupportFragmentManager().findFragmentById(ADDITIONAL_CONTAINER_ID);
            if (obsoleteFragment != null) {
                L.i(TAG, "Remove the fragment from the addtitional container=" + obsoleteFragment);
                getSupportFragmentManager().beginTransaction().remove(obsoleteFragment).commit();
                // execute pending transactions so book play fragments onCreateView does not get called again.
                getSupportFragmentManager().executePendingTransactions();
            }
        }


        if (savedInstanceState == null) {
            if (getIntent().hasExtra(NI_MALFORMED_FILE)) {
                File malformedFile = (File) getIntent().getSerializableExtra(NI_MALFORMED_FILE);
                new MaterialDialog.Builder(this)
                        .title(R.string.mal_file_title)
                        .content(getString(R.string.mal_file_message) + "\n\n" + malformedFile)
                        .show();
            }
            if (getIntent().hasExtra(NI_GO_TO_BOOK)) {
                long bookId = getIntent().getLongExtra(NI_GO_TO_BOOK, -1);
                onBookSelected(bookId, new HashMap<View, String>(0));
            }
        }
    }

    @Override
    public void onBookSelected(long bookId, Map<View, String> sharedElements) {
        L.i(TAG, "onBookSelected(" + bookId + ")");
        FragmentManager fm = getSupportFragmentManager();

        BookPlayFragment bookPlayFragment = (BookPlayFragment) fm.findFragmentByTag(FM_BOOK_PLAY);
        if (bookPlayFragment == null || bookPlayFragment.getBookId() != bookId) {
            bookPlayFragment = BookPlayFragment.newInstance(bookId);
        }

        if (!bookPlayFragment.isVisible()) {
            fm.beginTransaction().remove(bookPlayFragment).commit();
            fm.executePendingTransactions();

            fm.beginTransaction().remove(bookPlayFragment)
                    .commit();
            fm.executePendingTransactions();

            FragmentTransaction ft = fm.beginTransaction();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !multiPane) {
                Transition move = TransitionInflater.from(this).inflateTransition(android.R.transition.move);
                bookPlayFragment.setSharedElementEnterTransition(move);
                for (Map.Entry<View, String> entry : sharedElements.entrySet()) {
                    ft.addSharedElement(entry.getKey(), entry.getValue());
                }
            }
            ft.replace(multiPane ? ADDITIONAL_CONTAINER_ID : BASE_CONTAINER_ID, bookPlayFragment, FM_BOOK_PLAY)
                    .addToBackStack(null)
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        List<Integer> menuItemIds = new ArrayList<>(menu.size());
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (menuItemIds.contains(item.getItemId())) {
                menu.removeItem(item.getItemId());
            } else {
                menuItemIds.add(item.getItemId());
            }
        }

        return true;
    }
}
