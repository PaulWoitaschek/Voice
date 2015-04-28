package de.ph1b.audiobook.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.SortedList;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.getbase.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.FolderOverviewActivity;
import de.ph1b.audiobook.activity.SettingsActivity;
import de.ph1b.audiobook.adapter.BookShelfAdapter;
import de.ph1b.audiobook.dialog.BookmarkDialogFragment;
import de.ph1b.audiobook.dialog.EditBookDialogFragment;
import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.BookAdder;
import de.ph1b.audiobook.model.DataBaseHelper;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.uitools.CoverReplacement;
import de.ph1b.audiobook.uitools.ImageHelper;
import de.ph1b.audiobook.uitools.PlayPauseDrawable;
import de.ph1b.audiobook.utils.Communication;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.PrefsManager;


public class BookShelfFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = BookShelfFragment.class.getSimpleName();
    private final BroadcastReceiver onBookSetChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            L.v(TAG, "onBookSetChanged called");
            adapter.newDataSet(db.getAllBooks());
            checkVisibilities();
        }
    };
    private final BroadcastReceiver onCoverChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            L.v(TAG, "onCoverChanged called");
            long bookChangedId = intent.getLongExtra(Communication.COVER_CHANGED_BOOK_ID, -1);
            SortedList<Book> sortedList = adapter.getSortedList();
            for (int i = 0; i < sortedList.size(); i++) {
                if (adapter.getItemId(i) == bookChangedId) {
                    adapter.notifyItemChanged(i);
                }
            }
        }
    };
    private final BroadcastReceiver onCurrentBookChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            L.v(TAG, "onCurrentBookChanged called");
            long oldId = intent.getLongExtra(Communication.CURRENT_BOOK_CHANGED_OLD_ID, -1);
            for (int i = 0; i < adapter.getItemCount(); i++) {
                long itemId = adapter.getItemId(i);
                if (itemId == oldId || itemId == prefs.getCurrentBookId())
                    adapter.notifyItemChanged(i);
            }
            checkVisibilities();
        }
    };
    private static final String RECYCLER_VIEW_STATE = "recyclerViewState";
    private final PlayPauseDrawable playPauseDrawable = new PlayPauseDrawable();
    private final BroadcastReceiver onPlayStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setPlayState(true);
        }
    };
    private final BroadcastReceiver onScannerStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkVisibilities();
        }
    };
    private BookShelfAdapter adapter;
    private PrefsManager prefs;
    private ServiceController controller;
    private MaterialDialog noFolderWarning;
    private RecyclerView recyclerView;
    private ProgressBar recyclerReplacementView;
    private FloatingActionButton fab;
    private DataBaseHelper db;
    private LocalBroadcastManager bcm;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_shelf, container, false);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setTitle(this.getString(R.string.app_name));
        }

        setHasOptionsMenu(true);

        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        recyclerReplacementView = (ProgressBar) view.findViewById(R.id.recyclerReplacement);
        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setIconDrawable(playPauseDrawable);
        fab.setOnClickListener(this);
        BookShelfAdapter.OnItemClickListener onClickListener =
                new BookShelfAdapter.OnItemClickListener() {
                    @Override
                    public void onCoverClicked(int position, ImageView cover) {
                        Book book = adapter.getItem(position);

                        prefs.setCurrentBookIdAndInform(book.getId());

                        startBookPlay(cover);
                    }

                    @Override
                    public void onMenuClicked(final int position, ImageButton editBook) {
                        PopupMenu popupMenu = new PopupMenu(getActivity(), editBook);
                        popupMenu.inflate(R.menu.bookshelf_popup);
                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                switch (item.getItemId()) {
                                    case R.id.cover:
                                        CharSequence[] items = new CharSequence[]{getString(R.string.edit_book_title)};
                                        new MaterialDialog.Builder(getActivity())
                                                .items(items)
                                                .title(R.string.cover)
                                                .itemsCallback(new MaterialDialog.ListCallback() {
                                                    @Override
                                                    public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                                                        switch (i) {
                                                            case 0:
                                                                Book book = adapter.getItem(position);

                                                                EditBookDialogFragment editBookDialogFragment = new EditBookDialogFragment();
                                                                Bundle bundle = new Bundle();

                                                                ArrayList<Bitmap> covers = new ArrayList<>();
                                                                CoverReplacement replacement = new CoverReplacement(book.getName(), getActivity());
                                                                covers.add(ImageHelper.drawableToBitmap(replacement,
                                                                        EditBookDialogFragment.REPLACEMENT_DIMEN,
                                                                        EditBookDialogFragment.REPLACEMENT_DIMEN));

                                                                File coverFile = book.getCoverFile();
                                                                if (coverFile.exists() && coverFile.canRead()) {
                                                                    Bitmap defaultCover = BitmapFactory.decodeFile(coverFile.getAbsolutePath());
                                                                    if (defaultCover != null) {
                                                                        covers.add(defaultCover);
                                                                    }
                                                                }

                                                                bundle.putParcelableArrayList(EditBookDialogFragment.BOOK_COVER, covers);
                                                                bundle.putLong(Book.TAG, book.getId());

                                                                editBookDialogFragment.setArguments(bundle);
                                                                editBookDialogFragment.show(getFragmentManager(), EditBookDialogFragment.TAG);
                                                        }
                                                    }
                                                })
                                                .show();
                                        return true;
                                    case R.id.bookmark:
                                        DialogFragment bookmarkDialogFragment = new BookmarkDialogFragment();
                                        Bundle args = new Bundle();
                                        args.putLong(BookmarkDialogFragment.BOOK_ID, adapter.getItemId(position));
                                        bookmarkDialogFragment.setArguments(args);
                                        bookmarkDialogFragment.show(getFragmentManager(), BookmarkDialogFragment.TAG);
                                        return true;
                                    default:
                                        return false;
                                }
                            }
                        });
                        popupMenu.show();
                    }
                };

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), getAmountOfColumns()));
        adapter = new BookShelfAdapter(getActivity(), onClickListener);
        adapter.addAll(db.getAllBooks());
        recyclerView.setAdapter(adapter);

        if (savedInstanceState != null) {
            recyclerView.getLayoutManager().onRestoreInstanceState(savedInstanceState
                    .getParcelable(RECYCLER_VIEW_STATE));
        }

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        String malformedFile = getArguments() != null ? getArguments().getString(
                MediaPlayerController.MALFORMED_FILE) : null;
        if (malformedFile != null) {
            new MaterialDialog.Builder(getActivity())
                    .title(R.string.mal_file_title)
                    .content(getString(R.string.mal_file_message) + "\n\n" + malformedFile)
                    .show();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new PrefsManager(getActivity());
        db = DataBaseHelper.getInstance(getActivity());
        bcm = LocalBroadcastManager.getInstance(getActivity());
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
        return columns > 2 ? columns : 2;
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
        onBookSetChangedReceiver.onReceive(getActivity(), new Intent());

        // register receivers
        bcm.registerReceiver(onBookSetChangedReceiver, new IntentFilter(Communication.BOOK_SET_CHANGED));
        bcm.registerReceiver(onCoverChanged, new IntentFilter(Communication.COVER_CHANGED));
        bcm.registerReceiver(onCurrentBookChanged, new IntentFilter(Communication.CURRENT_BOOK_CHANGED));
        bcm.registerReceiver(onPlayStateChanged, new IntentFilter(Communication.PLAY_STATE_CHANGED));
        bcm.registerReceiver(onScannerStateChanged, new IntentFilter(Communication.SCANNER_STATE_CHANGED));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (recyclerView != null) {
            outState.putParcelable(RECYCLER_VIEW_STATE, recyclerView.getLayoutManager()
                    .onSaveInstanceState());
        }

        super.onSaveInstanceState(outState);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.book_shelf, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startBookPlay(View view) {
        ViewCompat.setTransitionName(view, getString(R.string.transition_cover));
        Fragment bookPlayFragment = new BookPlayFragment();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            int enterTransitionDuration = 300;

            Transition sharedElementEnterTransition = TransitionInflater.from(getActivity()).inflateTransition(android.R.transition.move);
            sharedElementEnterTransition.setDuration(enterTransitionDuration);
            bookPlayFragment.setSharedElementEnterTransition(sharedElementEnterTransition);

            Transition fade = new Fade();
            fade.setDuration(enterTransitionDuration);
            fade.excludeTarget(R.id.toolbar, true);
            fade.excludeTarget(R.id.book_cover, true);

            bookPlayFragment.setEnterTransition(fade);
            bookPlayFragment.setReturnTransition(null);
            bookPlayFragment.setExitTransition(null);
            setReturnTransition(null);
            setReenterTransition(null);
        }

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content, bookPlayFragment, BookPlayFragment.TAG)
                .addSharedElement(fab, getString(R.string.transition_fab))
                .addToBackStack(null);

        /**
         * Only use transition if we don't use a cover replacement. Else there is a bug so the
         * replacement won't scale correctly.
         */
        Book currentBook = db.getBook(prefs.getCurrentBookId());
        boolean isRealCover = currentBook != null &&
                (!currentBook.isUseCoverReplacement() && currentBook.getCoverFile().exists());
        if (isRealCover)
            ft.addSharedElement(view, getString(R.string.transition_cover));

        ft.commit();
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

        bcm.unregisterReceiver(onBookSetChangedReceiver);
        bcm.unregisterReceiver(onCoverChanged);
        bcm.unregisterReceiver(onCurrentBookChanged);
        bcm.unregisterReceiver(onPlayStateChanged);
        bcm.unregisterReceiver(onScannerStateChanged);
    }
}
