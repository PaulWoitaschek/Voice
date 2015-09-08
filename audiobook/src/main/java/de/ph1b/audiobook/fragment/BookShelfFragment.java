package de.ph1b.audiobook.fragment;

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
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.getbase.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import de.ph1b.audiobook.utils.Communication;

/**
 * Showing the shelf of all the available books and provide a navigation to each book
 *
 * @author Paul Woitaschek
 */
public class BookShelfFragment extends Fragment implements View.OnClickListener,
        BookShelfAdapter.OnItemClickListener {

    public static final String TAG = BookShelfFragment.class.getSimpleName();
    private static final Communication COMMUNICATION = Communication.getInstance();
    private final PlayPauseDrawable playPauseDrawable = new PlayPauseDrawable();
    private BookShelfAdapter adapter;
    private PrefsManager prefs;
    private ServiceController controller;
    private MaterialDialog noFolderWarning;
    private RecyclerView recyclerView;
    private ProgressBar recyclerReplacementView;
    private FloatingActionButton fab;
    private final Communication.SimpleBookCommunication listener = new Communication.SimpleBookCommunication() {
        @Override
        public void onBookContentChanged(@NonNull final Book book) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.updateOrAddBook(book);
                }
            });
        }

        @Override
        public void onPlayStateChanged() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setPlayState(true);
                }
            });
        }

        @Override
        public void onScannerStateChanged() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    checkVisibilities();
                }
            });
        }

        @Override
        public void onBookSetChanged(@NonNull final List<Book> activeBooks) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.newDataSet(activeBooks);
                    checkVisibilities();
                }
            });
        }


        @Override
        public void onCurrentBookIdChanged(final long oldId) {
            getActivity().runOnUiThread(new Runnable() {
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
    private DataBaseHelper db;
    private RecyclerView.ItemDecoration listDecoration;
    private GridLayoutManager gridLayoutManager;
    private RecyclerView.LayoutManager linearLayoutManager;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_shelf, container, false);

        // find views
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        assert actionBar != null;
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        recyclerReplacementView = (ProgressBar) view.findViewById(R.id.recyclerReplacement);
        fab = (FloatingActionButton) view.findViewById(R.id.fab);

        // init views
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setTitle(getString(R.string.app_name));
        fab.setIconDrawable(playPauseDrawable);
        fab.setOnClickListener(this);
        recyclerView.setHasFixedSize(true);
        // without this the item would blink on every change
        recyclerView.getItemAnimator().setSupportsChangeAnimations(false);

        listDecoration = new DividerItemDecoration(getActivity());
        gridLayoutManager = new GridLayoutManager(getActivity(), getAmountOfColumns());
        linearLayoutManager = new LinearLayoutManager(getActivity());
        initRecyclerView();

        return view;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        // init variables
        prefs = PrefsManager.getInstance(getActivity());
        db = DataBaseHelper.getInstance(getActivity());
        controller = new ServiceController(getActivity());
        noFolderWarning = new MaterialDialog.Builder(getActivity())
                .title(R.string.no_audiobook_folders_title)
                .content(getString(R.string.no_audiobook_folders_summary_start) + "\n\n" +
                        getString(R.string.no_audiobook_folders_end))
                .positiveText(R.string.dialog_confirm)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        startActivity(new Intent(getActivity(), FolderOverviewActivity.class));
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
        adapter = new BookShelfAdapter(getActivity(), defaultDisplayMode, this);
        adapter.newDataSet(db.getActiveBooks());
        recyclerView.setAdapter(adapter);
        getActivity().invalidateOptionsMenu();
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
        if (((MultiPaneInformer) getActivity()).isMultiPanel()) {
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
        BookAdder.getInstance(getActivity()).scanForFiles(false);

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
        COMMUNICATION.addBookCommunicationListener(listener);
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

        if (((MultiPaneInformer) getActivity()).isMultiPanel() || !currentBookExists) {
            fab.setVisibility(View.GONE);
        } else {
            fab.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.book_shelf, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // sets menu item visible if there is a current book
        MenuItem currentPlaying = menu.findItem(R.id.action_current);
        boolean multiPane = ((MultiPaneInformer) getActivity()).isMultiPanel();
        currentPlaying.setVisible(!multiPane && db.getBook(prefs.getCurrentBookId()) != null);

        MenuItem displayModeItem = menu.findItem(R.id.action_change_layout);
        DisplayMode displayMode = prefs.getDisplayMode();
        displayModeItem.setIcon(displayMode == DisplayMode.GRID ?
                R.drawable.ic_view_list_white_24dp : R.drawable.ic_view_grid_white_24dp);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(getActivity(), SettingsActivity.class));
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
        ((BookSelectionCallback) getActivity()).onBookSelected(bookId, sharedElements);
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

        COMMUNICATION.removeBookCommunicationListener(listener);
    }

    @Override
    public void onCoverClicked(int position) {
        long bookId = adapter.getItemId(position);
        invokeBookSelectionCallback(bookId);
    }

    @Override
    public void onMenuClicked(final int position, final View editBook) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), editBook);
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
                                adapter.notifyItemAtIdChanged(book.getId());
                            }
                        });
                        fragment.show(getFragmentManager(), EditCoverDialogFragment.TAG);
                        return true;
                    case R.id.edit_title:
                        EditBookTitleDialogFragment editBookTitle = EditBookTitleDialogFragment.newInstance(book.getName());
                        editBookTitle.setOnTextChangedListener(new EditBookTitleDialogFragment.OnTextChanged() {
                            @Override
                            public void onTitleChanged(@NonNull String newTitle) {
                                //noinspection SynchronizeOnNonFinalField
                                synchronized (db) {
                                    Book dbBook = db.getBook(book.getId());
                                    if (dbBook != null) {
                                        dbBook.setName(newTitle);
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
        void onBookSelected(long bookId, Map<View, String> sharedViews);
    }
}
