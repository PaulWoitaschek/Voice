package de.ph1b.audiobook.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.getbase.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.FolderOverviewActivity;
import de.ph1b.audiobook.activity.SettingsActivity;
import de.ph1b.audiobook.adapter.BookShelfAdapter;
import de.ph1b.audiobook.dialog.BookmarkDialogFragment;
import de.ph1b.audiobook.dialog.EditBookTitleDialogFragment;
import de.ph1b.audiobook.dialog.EditCoverDialogFragment;
import de.ph1b.audiobook.interfaces.MultiPaneInformer;
import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.BookAdder;
import de.ph1b.audiobook.persistence.DataBaseHelper;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.uitools.DividerItemDecoration;
import de.ph1b.audiobook.uitools.PlayPauseDrawable;
import de.ph1b.audiobook.utils.App;
import de.ph1b.audiobook.utils.Communication;

/**
 * Showing the shelf of all the available books and provide a navigation to each book
 *
 * @author Paul Woitaschek
 */
public class BookShelfFragment extends Fragment implements BookShelfAdapter.OnItemClickListener {

    public static final String TAG = BookShelfFragment.class.getSimpleName();
    private final PlayPauseDrawable playPauseDrawable = new PlayPauseDrawable();
    @Inject Communication communication;
    @Bind(R.id.recyclerView) RecyclerView recyclerView;
    @Bind(R.id.recyclerReplacement) ProgressBar recyclerReplacementView;
    @Bind(R.id.fab) FloatingActionButton fab;
    @Inject PrefsManager prefs;
    @Inject DataBaseHelper db;
    @Inject BookAdder bookAdder;
    private BookShelfAdapter adapter;
    private ServiceController controller;
    private MaterialDialog noFolderWarning;
    private RecyclerView.ItemDecoration listDecoration;
    private GridLayoutManager gridLayoutManager;
    private RecyclerView.LayoutManager linearLayoutManager;
    private boolean isMultiPanel;
    private MultiPaneInformer multiPaneInformer;
    private BookSelectionCallback bookSelectionCallback;
    private AppCompatActivity hostingActivity;
    private final Communication.SimpleBookCommunication listener = new Communication.SimpleBookCommunication() {
        @Override
        public void onBookContentChanged(@NonNull final Book book) {
            hostingActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.updateOrAddBook(book);
                }
            });
        }

        @Override
        public void onPlayStateChanged() {
            hostingActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setPlayState(true);
                }
            });
        }

        @Override
        public void onScannerStateChanged() {
            hostingActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    checkVisibilities();
                }
            });
        }

        @Override
        public void onBookSetChanged(@NonNull final List<Book> activeBooks) {
            hostingActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.newDataSet(activeBooks);
                    checkVisibilities();
                }
            });
        }

        @Override
        public void onCurrentBookIdChanged(final long oldId) {
            hostingActivity.runOnUiThread(new Runnable() {
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
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_shelf, container, false);
        ButterKnife.bind(this, view);

        isMultiPanel = multiPaneInformer.isMultiPanel();

        // find views
        ActionBar actionBar = hostingActivity.getSupportActionBar();
        assert actionBar != null;

        // init views
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setTitle(getString(R.string.app_name));
        fab.setIconDrawable(playPauseDrawable);
        recyclerView.setHasFixedSize(true);
        // without this the item would blink on every change
        SimpleItemAnimator anim = (SimpleItemAnimator) recyclerView.getItemAnimator();
        anim.setSupportsChangeAnimations(false);

        listDecoration = new DividerItemDecoration(getContext());
        gridLayoutManager = new GridLayoutManager(getContext(), getAmountOfColumns());
        linearLayoutManager = new LinearLayoutManager(getContext());
        initRecyclerView();

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        App.getComponent().inject(this);

        setRetainInstance(true);
        setHasOptionsMenu(true);

        // init variables
        controller = new ServiceController(getContext());
        noFolderWarning = new MaterialDialog.Builder(getContext())
                .title(R.string.no_audiobook_folders_title)
                .content(getString(R.string.no_audiobook_folders_summary_start) + "\n\n" +
                        getString(R.string.no_audiobook_folders_end))
                .positiveText(R.string.dialog_confirm)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        startActivity(new Intent(getContext(), FolderOverviewActivity.class));
                    }
                })
                .cancelable(false)
                .build();
    }

    private void initRecyclerView() {
        DisplayMode defaultDisplayMode = prefs.getDisplayMode();
        recyclerView.removeItemDecoration(listDecoration);
        if (defaultDisplayMode == BookShelfFragment.DisplayMode.GRID) {
            recyclerView.setLayoutManager(gridLayoutManager);
        } else {
            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.addItemDecoration(listDecoration);
        }
        adapter = new BookShelfAdapter(getContext(), defaultDisplayMode, this);
        adapter.newDataSet(db.getActiveBooks());
        recyclerView.setAdapter(adapter);
        hostingActivity.invalidateOptionsMenu();
    }

    /**
     * Returns the amount of columns the main-grid will need.
     *
     * @return The amount of columns, but at least 2.
     */
    private int getAmountOfColumns() {
        Resources r = recyclerView.getResources();
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float widthPx = displayMetrics.widthPixels;
        if (isMultiPanel) {
            widthPx = widthPx / 2;
        }
        float desiredPx = r.getDimensionPixelSize(R.dimen.desired_medium_cover);
        int columns = Math.round(widthPx / desiredPx);
        return Math.max(columns, 2);
    }

    @Override
    public void onResume() {
        super.onResume();

        // scan for files
        bookAdder.scanForFiles(false);

        // show dialog if no folders are set
        boolean audioFoldersEmpty = (prefs.getCollectionFolders().size() +
                prefs.getSingleBookFolders().size()) == 0;
        boolean noFolderWarningIsShowing = noFolderWarning.isShowing();
        if (audioFoldersEmpty && !noFolderWarningIsShowing) {
            noFolderWarning.show();
        }

        // update items and set ui
        setPlayState(false);
        listener.onBookSetChanged(db.getActiveBooks());

        // register receivers
        communication.addBookCommunicationListener(listener);
    }

    private void setPlayState(boolean animated) {
        if (MediaPlayerController.getPlayState() == MediaPlayerController.PlayState.PLAYING) {
            playPauseDrawable.transformToPause(animated);
        } else {
            playPauseDrawable.transformToPlay(animated);
        }
    }

    private void checkVisibilities() {
        final boolean hideRecycler = adapter.getItemCount() == 0 && bookAdder.isScannerActive();
        if (hideRecycler) {
            recyclerReplacementView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            recyclerReplacementView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        boolean currentBookExists = false;
        long currentBookId = prefs.getCurrentBookId();
        for (int i = 0; i < adapter.getItemCount(); i++) {
            if (currentBookId == adapter.getItemId(i)) {
                currentBookExists = true;
                break;
            }
        }

        if (isMultiPanel || !currentBookExists) {
            fab.setVisibility(View.GONE);
        } else {
            fab.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.book_shelf, menu);

        // sets menu item visible if there is a current book
        MenuItem currentPlaying = menu.findItem(R.id.action_current);
        currentPlaying.setVisible(!isMultiPanel && db.getBook(prefs.getCurrentBookId()) != null);

        // sets the grid / list toggle icon
        MenuItem displayModeItem = menu.findItem(R.id.action_change_layout);
        boolean gridMode = prefs.getDisplayMode() == DisplayMode.GRID;
        displayModeItem.setIcon(gridMode ? R.drawable.ic_view_list_white_24dp : R.drawable.ic_view_grid_white_24dp);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(getContext(), SettingsActivity.class));
                return true;
            case R.id.action_current:
                invokeBookSelectionCallback(prefs.getCurrentBookId());
                return true;
            case R.id.action_change_layout:
                DisplayMode mode = prefs.getDisplayMode();
                DisplayMode invertedMode = mode == DisplayMode.GRID ? DisplayMode.LIST : DisplayMode.GRID;
                prefs.setDisplayMode(invertedMode);
                initRecyclerView();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void invokeBookSelectionCallback(long bookId) {
        prefs.setCurrentBookIdAndInform(bookId);

        Map<View, String> sharedElements = new HashMap<>(2);
        BookShelfAdapter.BaseViewHolder viewHolder = (BookShelfAdapter.BaseViewHolder) recyclerView.findViewHolderForItemId(bookId);
        if (viewHolder != null) {
            sharedElements.put(viewHolder.coverView, ViewCompat.getTransitionName(viewHolder.coverView));
        }
        sharedElements.put(fab, ViewCompat.getTransitionName(fab));
        bookSelectionCallback.onBookSelected(bookId, sharedElements);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        bookSelectionCallback = (BookSelectionCallback) context;
        multiPaneInformer = (MultiPaneInformer) context;
        hostingActivity = (AppCompatActivity) context;
    }

    @OnClick(R.id.fab)
    void playPauseClicked() {
        controller.playPause();
    }

    @Override
    public void onPause() {
        super.onPause();

        communication.removeBookCommunicationListener(listener);
    }

    @Override
    public void onItemClicked(int position) {
        long bookId = adapter.getItemId(position);
        invokeBookSelectionCallback(bookId);
    }

    @Override
    public void onMenuClicked(final int position, final View view) {
        PopupMenu popupMenu = new PopupMenu(getContext(), view);
        popupMenu.inflate(R.menu.bookshelf_popup);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final Book book = adapter.getItem(position);
                switch (item.getItemId()) {
                    case R.id.edit_cover:
                        EditCoverDialogFragment fragment = EditCoverDialogFragment.newInstance(book);
                        fragment.setOnEditBookFinished(new EditCoverDialogFragment.OnEditBookFinished() {
                            @Override
                            public void onEditBookFinished() {
                                // this is necessary for the cover update
                                adapter.notifyItemAtIdChanged(book.id());
                            }
                        });
                        fragment.show(getFragmentManager(), EditCoverDialogFragment.TAG);
                        return true;
                    case R.id.edit_title:
                        EditBookTitleDialogFragment editBookTitle = EditBookTitleDialogFragment.newInstance(book.name());
                        editBookTitle.setOnTextChangedListener(new EditBookTitleDialogFragment.OnTextChanged() {
                            @Override
                            public void onTitleChanged(@NonNull String newTitle) {
                                //noinspection SynchronizeOnNonFinalField
                                synchronized (db) {
                                    Book dbBook = db.getBook(book.id());
                                    if (dbBook != null) {
                                        dbBook = Book.builder(dbBook)
                                                .name(newTitle)
                                                .build();
                                        db.updateBook(dbBook);
                                    }
                                }
                            }
                        });
                        editBookTitle.show(getFragmentManager(), EditBookTitleDialogFragment.TAG);
                        return true;
                    case R.id.bookmark:
                        BookmarkDialogFragment.newInstance(adapter.getItemId(position))
                                .show(getFragmentManager(), TAG);
                        return true;
                    default:
                        return false;
                }
            }
        });
        popupMenu.show();
    }

    public enum DisplayMode {
        GRID,
        LIST
    }

    public interface BookSelectionCallback {
        /**
         * This is called when a selection has been made
         *
         * @param bookId      the id of the selected book
         * @param sharedViews A mapping of the shared views and their transition names
         */
        void onBookSelected(long bookId, Map<View, String> sharedViews);
    }
}
