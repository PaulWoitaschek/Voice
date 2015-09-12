package de.ph1b.audiobook.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
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
import de.ph1b.audiobook.utils.PermissionHelper;

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
    private static final int CONTAINER_PLAY = R.id.play_container;
    @IdRes
    private static final int CONTAINER_SHELF = R.id.shelf_container;
    private static final String NI_MALFORMED_FILE = "malformedFile";
    private static final String NI_GO_TO_BOOK = "niGotoBook";
    /**
     * Used for {@link #onSaveInstanceState(Bundle)} to get the previous panel mode.
     */
    private static final String SI_MULTI_PANEL = "siMultiPanel";
    private static final int PERMISSION_RESULT_READ_EXT_STORAGE = 17;
    private boolean multiPanel = false;

    /**
     * Returns an intent to start the activity with to inform the user that a certain file may be
     * defect
     *
     * @param c             The context
     * @param malformedFile The defect file
     * @return The intent to start the activity with.
     */
    public static Intent malformedFileIntent(Context c, File malformedFile) {
        Intent intent = new Intent(c, BookActivity.class);
        intent.putExtra(NI_MALFORMED_FILE, malformedFile);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /**
     * Returns an intent that lets you go directly to the playback screen for a certain book
     *
     * @param c      The context
     * @param bookId The book id to target
     * @return The intent
     */
    public static Intent goToBookIntent(Context c, long bookId) {
        Intent intent = new Intent(c, BookActivity.class);
        intent.putExtra(NI_GO_TO_BOOK, bookId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    public boolean isMultiPanel() {
        return multiPanel;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SI_MULTI_PANEL, isMultiPanel());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            boolean permissionGrantingWorked = PermissionHelper.permissionGrantingWorked(requestCode,
                    PERMISSION_RESULT_READ_EXT_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
                    permissions, grantResults);
            L.i(TAG, "permissionGrantingWorked=" + permissionGrantingWorked);
            if (!permissionGrantingWorked) {
                PermissionHelper.handleExtStorageRescan(this, PERMISSION_RESULT_READ_EXT_STORAGE);
                L.e(TAG, "could not get permission");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PrefsManager prefs = PrefsManager.getInstance(this);

        setContentView(R.layout.activity_book);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            boolean anyFolderSet = prefs.getCollectionFolders().size() + prefs.getSingleBookFolders().size() > 0;
            boolean canReadStorage = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            if (anyFolderSet && !canReadStorage) {
                PermissionHelper.handleExtStorageRescan(this, PERMISSION_RESULT_READ_EXT_STORAGE);
            }
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        multiPanel = findViewById(CONTAINER_SHELF) != null;
        boolean multiPaneChanged = savedInstanceState != null && savedInstanceState.getBoolean(SI_MULTI_PANEL) != multiPanel;
        L.i(TAG, "multiPane=" + multiPanel + ", multiPaneChanged=" + multiPaneChanged);

        // first retrieve the fragments
        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState == null) {
            BookShelfFragment bookShelfFragment = new BookShelfFragment();
            if (multiPanel) {
                fm.beginTransaction()
                        .replace(CONTAINER_SHELF, bookShelfFragment, FM_BOOK_SHELF)
                        .replace(CONTAINER_PLAY, BookPlayFragment.newInstance(prefs.getCurrentBookId()), FM_BOOK_PLAY)
                        .commit();
            } else {
                fm.beginTransaction()
                        .replace(CONTAINER_PLAY, bookShelfFragment, FM_BOOK_SHELF)
                        .commit();
            }
        } else if (multiPaneChanged) {
            // we need to pop the whole back-stack. Else we can't change the container id
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

            // restore book shelf or create new one
            BookShelfFragment bookShelfFragment = (BookShelfFragment) fm.findFragmentByTag(FM_BOOK_SHELF);
            if (bookShelfFragment == null) {
                bookShelfFragment = new BookShelfFragment();
                L.v(TAG, "new fragment=" + bookShelfFragment);
            } else {
                fm.beginTransaction()
                        .remove(bookShelfFragment)
                        .commit();
                fm.executePendingTransactions();
                L.v(TAG, "restored fragment=" + bookShelfFragment);
            }

            // restore book play fragment or create new one
            BookPlayFragment bookPlayFragment = (BookPlayFragment) fm.findFragmentByTag(FM_BOOK_PLAY);
            if (bookPlayFragment == null) {
                bookPlayFragment = BookPlayFragment.newInstance(prefs.getCurrentBookId());
                L.v(TAG, "new fragment=" + bookPlayFragment);
            } else {
                fm.beginTransaction()
                        .remove(bookPlayFragment)
                        .commit();
                fm.executePendingTransactions();
                L.v(TAG, "restored fragment=" + bookPlayFragment);
                if (bookPlayFragment.getBookId() != prefs.getCurrentBookId()) {
                    bookPlayFragment = BookPlayFragment.newInstance(prefs.getCurrentBookId());
                    L.v(TAG, "id did not match. Created new fragment=" + bookPlayFragment);
                }
            }

            if (multiPanel) {
                fm.beginTransaction()
                        .replace(CONTAINER_SHELF, bookShelfFragment, FM_BOOK_SHELF)
                        .replace(CONTAINER_PLAY, bookPlayFragment, FM_BOOK_PLAY)
                        .commit();
            } else {
                fm.beginTransaction()
                        .replace(CONTAINER_PLAY, bookShelfFragment, FM_BOOK_SHELF)
                        .commit();
                fm.beginTransaction()
                        .replace(CONTAINER_PLAY, bookPlayFragment, FM_BOOK_PLAY)
                        .addToBackStack(null)
                        .commit();
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
    public void onBookSelected(long bookId, Map<View, String> sharedViews) {
        L.i(TAG, "onBookSelected(" + bookId + ")");

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        BookPlayFragment bookPlayFragment = BookPlayFragment.newInstance(bookId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !multiPanel) {
            Transition move = TransitionInflater.from(this).inflateTransition(android.R.transition.move);
            bookPlayFragment.setSharedElementEnterTransition(move);
            for (Map.Entry<View, String> entry : sharedViews.entrySet()) {
                L.v(TAG, "Added sharedElement=" + entry);
                ft.addSharedElement(entry.getKey(), entry.getValue());
            }
        }

        // only replace if there is not already a fragment with that id
        Fragment containingFragment = getSupportFragmentManager().findFragmentById(CONTAINER_PLAY);
        if (containingFragment == null || !(containingFragment instanceof BookPlayFragment) || (((BookPlayFragment) containingFragment).getBookId() != bookId)) {
            ft.replace(CONTAINER_PLAY, bookPlayFragment, FM_BOOK_PLAY).addToBackStack(null)
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

    @Override
    public void onBackPressed() {
        Fragment bookShelfFragment = getSupportFragmentManager().findFragmentByTag(FM_BOOK_SHELF);
        if ((bookShelfFragment != null && bookShelfFragment.isVisible())) {
            finish();
        } else {
            super.onBackPressed();
        }
    }
}
