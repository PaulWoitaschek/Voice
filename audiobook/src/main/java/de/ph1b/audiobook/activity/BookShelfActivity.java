package de.ph1b.audiobook.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.ActionBar;
import android.support.v7.util.SortedList;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.getbase.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.adapter.BookShelfAdapter;
import de.ph1b.audiobook.dialog.BookmarkDialogFragment;
import de.ph1b.audiobook.dialog.EditBookDialogFragment;
import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.BookAdder;
import de.ph1b.audiobook.model.DataBaseHelper;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.uitools.PlayPauseDrawable;
import de.ph1b.audiobook.utils.Communication;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.PrefsManager;

/**
 * Created by Paul Woitaschek (woitaschek@posteo.de, paul-woitaschek.de) on 12.07.15.
 * Showing the shelf of all the available books and provide a navigation to each book
 */
public class BookShelfActivity extends BaseActivity implements View.OnClickListener, Communication.OnBookSetChangedListener, Communication.OnCurrentBookIdChangedListener, Communication.OnScannerStateChangedListener, Communication.OnPlayStateChangedListener, Communication.OnCoverChangedListener, BookShelfAdapter.OnItemClickListener {

    private static final String TAG = BookShelfActivity.class.getSimpleName();
    private static final String MALFORMED_FILE = "malformedFile";
    private final PlayPauseDrawable playPauseDrawable = new PlayPauseDrawable();
    private final Communication communication = Communication.getInstance();
    private BookShelfAdapter adapter;
    private PrefsManager prefs;
    private ServiceController controller;
    private MaterialDialog noFolderWarning;
    private RecyclerView recyclerView;
    private ProgressBar recyclerReplacementView;
    private FloatingActionButton fab;
    private DataBaseHelper db;

    public static Intent malformedFileIntent(Context c, String malformedFile) {
        Intent intent = new Intent(c, BookShelfActivity.class);
        intent.putExtra(MALFORMED_FILE, malformedFile);
        return intent;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init variables
        prefs = PrefsManager.getInstance(this);
        db = DataBaseHelper.getInstance(this);
        controller = new ServiceController(this);
        noFolderWarning = new MaterialDialog.Builder(this)
                .title(R.string.no_audiobook_folders_title)
                .content(getString(R.string.no_audiobook_folders_summary_start) + "\n\n" +
                        getString(R.string.no_audiobook_folders_end))
                .positiveText(R.string.dialog_confirm)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        startActivity(new Intent(BookShelfActivity.this, FolderOverviewActivity.class));
                    }
                })
                .cancelable(false)
                .build();

        // find views
        setContentView(R.layout.activity_book_shelf);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerReplacementView = (ProgressBar) findViewById(R.id.recyclerReplacement);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        // init views
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setTitle(getString(R.string.app_name));
        fab.setIconDrawable(playPauseDrawable);
        fab.setOnClickListener(this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this, getAmountOfColumns()));
        adapter = new BookShelfAdapter(this, this);
        adapter.addAll(db.getActiveBooks());
        recyclerView.setAdapter(adapter);

        if (savedInstanceState == null) {
            if (getIntent().hasExtra(MALFORMED_FILE)) {
                String malformedFile = getIntent().getStringExtra(MALFORMED_FILE);
                new MaterialDialog.Builder(this)
                        .title(R.string.mal_file_title)
                        .content(getString(R.string.mal_file_message) + "\n\n" + malformedFile)
                        .show();
            }
        }
    }

    /**
     * Returns the amount of columns the main-grid will need.
     *
     * @return The amount of columns, but at least 2.
     */
    private int getAmountOfColumns() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float widthPx = displayMetrics.widthPixels;
        float desiredPx = getResources().getDimensionPixelSize(R.dimen.desired_medium_cover);
        int columns = Math.round(widthPx / desiredPx);
        return Math.max(columns, 2);
    }

    @Override
    public void onResume() {
        super.onResume();

        // scan for files
        BookAdder.getInstance(this).scanForFiles(false);

        // show dialog if no folders are set
        boolean audioFoldersEmpty = (prefs.getCollectionFolders().size() +
                prefs.getSingleBookFolders().size()) == 0;
        boolean noFolderWarningIsShowing = noFolderWarning.isShowing();
        if (audioFoldersEmpty && !noFolderWarningIsShowing) {
            noFolderWarning.show();
        }

        // update items and set ui
        setPlayState(false);
        onBookSetChanged(db.getActiveBooks());

        // register receivers
        communication.addOnBookSetChangedListener(this);
        communication.addOnCoverChangedListener(this);
        communication.addOnCurrentBookIdChangedListener(this);
        communication.addOnPlayStateChangedListener(this);
        communication.addOnScannerStateChangedListener(this);
    }

    private void setPlayState(boolean animated) {
        if (MediaPlayerController.getPlayState() == MediaPlayerController.PlayState.PLAYING) {
            playPauseDrawable.transformToPause(animated);
        } else {
            playPauseDrawable.transformToPlay(animated);
        }
    }

    private void checkVisibilities() {
        final boolean hideRecycler = adapter.getItemCount() == 0 && BookAdder.scannerActive;
        L.v(TAG, "checkVisibilities hidesRecycler=" + hideRecycler);
        if (hideRecycler) {
            recyclerReplacementView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            recyclerReplacementView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        boolean currentBookExists = false;
        SortedList<Book> sortedList = adapter.getSortedList();
        for (int i = 0; i < sortedList.size(); i++) {
            Book b = sortedList.get(i);
            if (b.getId() == prefs.getCurrentBookId()) {
                currentBookExists = true;
                break;
            }
        }
        if (currentBookExists) {
            fab.setVisibility(View.VISIBLE);
        } else {
            fab.setVisibility(View.GONE);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.book_shelf, menu);

        // sets menu item visible if there is a current book
        MenuItem currentPlaying = menu.findItem(R.id.action_current);
        currentPlaying.setVisible(db.getBook(prefs.getCurrentBookId()) != null);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_current:
                startBookPlay();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startBookPlay() {
        Book currentBook = db.getBook(prefs.getCurrentBookId());
        if (currentBook != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setReturnTransition(null);
                getWindow().setReenterTransition(null);
                getWindow().setSharedElementReturnTransition(null);
                getWindow().setSharedElementReenterTransition(null);
                getWindow().setSharedElementsUseOverlay(false);
            }

            List<Pair<View, String>> shared = new ArrayList<>();
            BookShelfAdapter.ViewHolder viewHolder = (BookShelfAdapter.ViewHolder) recyclerView
                    .findViewHolderForItemId(currentBook.getId());
            if (viewHolder != null && !currentBook.isUseCoverReplacement() && currentBook.getCoverFile().exists()) {
                shared.add(Pair.create((View) viewHolder.coverView, BookPlayActivity.TRANSITION_COVER));
            }
            // buggy
            //shared.add(Pair.create((View) fab, BookPlayActivity.TRANSITION_FAB));
            @SuppressWarnings("unchecked") ActivityOptionsCompat opts = ActivityOptionsCompat.makeSceneTransitionAnimation(this, shared.toArray(new Pair[shared.size()]));
            ActivityCompat.startActivity(this, BookPlayActivity.newIntent(this, prefs.getCurrentBookId()), opts.toBundle());
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab:
                controller.playPause();
                break;
            default:
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        communication.removeOnBookSetChangedListener(this);
        communication.removeOnCoverChangedListener(this);
        communication.removeOnCurrentBookIdChangedListener(this);
        communication.removeOnPlayStateChangedListener(this);
        communication.removeOnScannerStateChangedListener(this);
    }

    @Override
    public void onBookSetChanged(@NonNull final List<Book> activeBooks) {
        L.v(TAG, "onBookSetChanged called");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.newDataSet(activeBooks);
                checkVisibilities();
            }
        });
    }

    @Override
    public void onCoverChanged(final long bookId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                L.v(TAG, "onCoverChanged called");
                SortedList<Book> sortedList = adapter.getSortedList();
                for (int i = 0; i < sortedList.size(); i++) {
                    if (adapter.getItemId(i) == bookId) {
                        adapter.notifyItemChanged(i);
                    }
                }
            }
        });
    }

    @Override
    public void onPlayStateChanged() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setPlayState(true);
            }
        });
    }

    @Override
    public void onScannerStateChanged() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                checkVisibilities();
            }
        });
    }

    @Override
    public void onCurrentBookIdChanged(final long oldId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < adapter.getItemCount(); i++) {
                    long itemId = adapter.getItemId(i);
                    if (itemId == oldId || itemId == prefs.getCurrentBookId()) {
                        adapter.notifyItemChanged(i);
                    }
                }
                checkVisibilities();
            }
        });
    }

    @Override
    public void onCoverClicked(int position) {
        Book book = adapter.getItem(position);
        prefs.setCurrentBookIdAndInform(book.getId());
        startBookPlay();
    }

    @Override
    public void onMenuClicked(final int position, View editBook) {
        PopupMenu popupMenu = new PopupMenu(BookShelfActivity.this, editBook);
        popupMenu.inflate(R.menu.bookshelf_popup);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.edit_book:
                        Book book = adapter.getItem(position);
                        EditBookDialogFragment.newInstance(book, BookShelfActivity.this).show(
                                getSupportFragmentManager(), EditBookDialogFragment.TAG);
                        return true;
                    case R.id.bookmark:
                        BookmarkDialogFragment.newInstance(adapter.getItemId(position))
                                .show(getSupportFragmentManager(), TAG);
                        return true;
                    default:
                        return false;
                }
            }
        });
        popupMenu.show();
    }
}
