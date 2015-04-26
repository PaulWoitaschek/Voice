package de.ph1b.audiobook.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.MaterialDialog;

import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.FolderOverviewActivity;
import de.ph1b.audiobook.activity.SettingsActivity;
import de.ph1b.audiobook.adapter.BookShelfAdapter;
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


public class BookShelfFragment extends Fragment implements View.OnClickListener,
        EditBookDialogFragment.OnEditBookFinishedListener {

    public static final String TAG = BookShelfFragment.class.getSimpleName();
    private static final String RECYCLER_VIEW_STATE = "recyclerViewState";
    private final PlayPauseDrawable playPauseDrawable = new PlayPauseDrawable();
    private final ArrayList<Book> allBooks = new ArrayList<>();
    private final BroadcastReceiver onBookSetChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // old books
            ArrayList<Book> oldBooks = new ArrayList<>();
            for (Book b : allBooks) {
                oldBooks.add(new Book(b));
            }

            // new books
            ArrayList<Book> newBooks = db.getAllBooks();

            // find books to delete
            ArrayList<Book> booksToDelete = new ArrayList<>();
            deleteLoop:
            for (Book oldB : oldBooks) {
                for (Book newB : newBooks) {
                    if (oldB.getId() == newB.getId())
                        continue deleteLoop;
                }
                booksToDelete.add(oldB);
            }
            for (Book bookToDelete : booksToDelete) {
                int index = allBooks.indexOf(bookToDelete);
                allBooks.remove(index);
                adapter.notifyItemRemoved(index);
            }

            // find books to add
            ArrayList<Book> booksToAdd = new ArrayList<>();
            addLoop:
            for (Book newB : newBooks) {
                for (Book oldB : oldBooks) {
                    if (newB.getId() == oldB.getId())
                        continue addLoop;
                }
                booksToAdd.add(newB);
            }
            for (Book bookToAdd : booksToAdd) {
                allBooks.add(bookToAdd);
                Collections.sort(allBooks);
                adapter.notifyItemInserted(allBooks.indexOf(bookToAdd));
            }

            toggleRecyclerVisibilities();
        }
    };
    private final BroadcastReceiver onCoverChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long bookChangedId = intent.getLongExtra(Communication.COVER_CHANGED_BOOK_ID, -1);
            for (int i = 0; i < allBooks.size(); i++) {
                if (allBooks.get(i).getId() == bookChangedId) {
                    adapter.notifyItemChanged(i);
                }
            }

            for (Book b : allBooks) {
                if (b.getId() == intent.getLongExtra(Communication.COVER_CHANGED_BOOK_ID, -1))
                    adapter.notifyItemChanged(allBooks.indexOf(b));
            }
        }
    };
    private final BroadcastReceiver onCurrentBookChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            fab.setVisibility(View.VISIBLE);
            long oldId = intent.getLongExtra(Communication.CURRENT_BOOK_CHANGED_OLD_ID, -1);
            for (int i = 0; i < adapter.getItemCount(); i++) {
                long itemId = adapter.getItemId(i);
                if (itemId == oldId || itemId == prefs.getCurrentBookId())
                    adapter.notifyItemChanged(i);
            }
        }
    };
    private final BroadcastReceiver onPlayStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setPlayState(true);
        }
    };
    private final BroadcastReceiver onScannerStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            toggleRecyclerVisibilities();
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
                        long oldId = prefs.getCurrentBookId();
                        prefs.setCurrentBookId(book.getId());
                        Communication.sendCurrentBookChanged(getActivity(), oldId);

                        startBookPlay(cover);
                    }

                    @Override
                    public void onMenuClicked(final int position) {
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
                };

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), getAmountOfColumns()));
        allBooks.clear();
        allBooks.addAll(db.getAllBooks());
        adapter = new BookShelfAdapter(allBooks, getActivity(), onClickListener);
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

        L.d(TAG, "onCreate called");

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

        if (db.getBook(prefs.getCurrentBookId()) == null) {
            fab.setVisibility(View.GONE);
        } else {
            fab.setVisibility(View.VISIBLE);
        }
        if (MediaPlayerController.getPlayState() == MediaPlayerController.PlayState.PLAYING) {
            playPauseDrawable.transformToPause(false);
        } else {
            playPauseDrawable.transformToPlay(false);
        }

        adapter.notifyDataSetChanged();

        BookAdder.getInstance(getActivity()).scanForFiles(false);

        setPlayState(false);
        onBookSetChangedReceiver.onReceive(getActivity(), new Intent());

        boolean audioFoldersEmpty = (prefs.getCollectionFolders().size() +
                prefs.getSingleBookFolders().size()) == 0;
        boolean noFolderWarningIsShowing = noFolderWarning.isShowing();
        if (audioFoldersEmpty && !noFolderWarningIsShowing) {
            noFolderWarning.show();
        }
        toggleRecyclerVisibilities();

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

    private void toggleRecyclerVisibilities() {
        L.v(TAG, "toggleRecyclerVisibilities");
        final boolean hideRecycler = adapter.getItemCount() == 0 && BookAdder.scannerActive;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (hideRecycler) {
                    recyclerReplacementView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    recyclerReplacementView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        });
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
    public void onEditBookFinished(@NonNull Book book) {
        int oldIndex = allBooks.indexOf(book);
        Collections.sort(allBooks);
        int newIndex = allBooks.indexOf(book);
        adapter.notifyItemMoved(oldIndex, newIndex);
        adapter.notifyItemChanged(newIndex);
        db.updateBook(book);
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
