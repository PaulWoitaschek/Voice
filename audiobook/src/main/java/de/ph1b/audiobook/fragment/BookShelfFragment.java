package de.ph1b.audiobook.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.FolderChooserActivity;
import de.ph1b.audiobook.activity.SettingsActivity;
import de.ph1b.audiobook.adapter.BookAdapter;
import de.ph1b.audiobook.dialog.EditBookDialog;
import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Chapter;
import de.ph1b.audiobook.service.BookAddingService;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.uitools.CoverReplacement;
import de.ph1b.audiobook.uitools.ImageHelper;
import de.ph1b.audiobook.uitools.ThemeUtil;
import de.ph1b.audiobook.utils.BaseApplication;
import de.ph1b.audiobook.utils.PrefsManager;


public class BookShelfFragment extends Fragment implements View.OnClickListener, EditBookDialog.OnEditBookFinished, BaseApplication.OnBookAddedListener, BaseApplication.OnBookDeletedListener, BaseApplication.OnPlayStateChangedListener, BaseApplication.OnPositionChangedListener, BaseApplication.OnScannerStateChangedListener {


    public static final String TAG = BookShelfFragment.class.getSimpleName();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private BookAdapter adapter;
    private ImageView currentCover;
    private TextView currentText;
    private ViewGroup playerWidget;
    private PrefsManager prefs;
    private BaseApplication baseApplication;
    private ServiceController controller;
    private ImageButton currentPlaying;
    private ProgressBar progressBar;
    private MaterialDialog noFolderWarning;
    private RecyclerView recyclerView;
    private ProgressBar recyclerReplacementView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_book_shelf, container, false);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        ((ActionBarActivity) getActivity()).setSupportActionBar(toolbar);
        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setTitle(this.getString(R.string.app_name));

        setHasOptionsMenu(true);

        playerWidget = (ViewGroup) view.findViewById(R.id.current);
        currentCover = (ImageView) view.findViewById(R.id.current_cover);
        currentText = (TextView) view.findViewById(R.id.current_text);
        currentPlaying = (ImageButton) view.findViewById(R.id.current_playing);
        progressBar = (ProgressBar) view.findViewById(R.id.progress);
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        recyclerReplacementView = (ProgressBar) view.findViewById(R.id.recyclerReplacement);

        playerWidget.setOnClickListener(this);
        currentPlaying.setOnClickListener(this);
        BookAdapter.OnItemClickListener onClickListener = new BookAdapter.OnItemClickListener() {
            @Override
            public void onCoverClicked(int position, final ViewGroup itemView) {
                Book book = adapter.getItem(position);
                baseApplication.setCurrentBook(book);
                prefs.setCurrentBookId(book.getId());

                /**
                 * Workaround. If we use a fake cover image transition will fail. So we don't du image transition in that case.
                 */
                boolean fakeCover = book.isUseCoverReplacement() || !book.getCoverFile().exists();
                ActivityOptionsCompat options;
                if (fakeCover) {
                    //noinspection unchecked
                    options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity());
                } else {
                    options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                            itemView, getString(R.string.cover_transition));
                }

                ViewCompat.setTransitionName(itemView, getString(R.string.cover_transition));

                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new BookPlayFragment(), BookPlayFragment.TAG)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onMenuClicked(final int position) {
                Book book = adapter.getItem(position);

                EditBookDialog editBookDialog = new EditBookDialog();
                Bundle bundle = new Bundle();

                ArrayList<Bitmap> covers = new ArrayList<>();
                CoverReplacement replacement = new CoverReplacement(book.getName(), getActivity());
                covers.add(ImageHelper.drawableToBitmap(replacement,
                        EditBookDialog.REPLACEMENT_DIMEN, EditBookDialog.REPLACEMENT_DIMEN));

                File coverFile = book.getCoverFile();
                if (coverFile.exists() && coverFile.canRead()) {
                    Bitmap defaultCover = BitmapFactory.decodeFile(coverFile.getAbsolutePath());
                    if (defaultCover != null) {
                        covers.add(defaultCover);
                    }
                }

                bundle.putParcelableArrayList(EditBookDialog.BOOK_COVER, covers);
                bundle.putLong(Book.TAG, book.getId());

                editBookDialog.setArguments(bundle);
                editBookDialog.show(getFragmentManager(), TAG);
            }
        };


        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), getAmountOfColumns()));
        adapter = new BookAdapter(baseApplication.getAllBooks(), getActivity(), onClickListener);
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        String malformedFile = getArguments() != null ? getArguments().getString(MediaPlayerController.MALFORMED_FILE, null) : null;
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

        baseApplication = (BaseApplication) getActivity().getApplication();
        prefs = new PrefsManager(getActivity());
        controller = new ServiceController(getActivity());
        noFolderWarning = new MaterialDialog.Builder(getActivity())
                .title(R.string.no_audiobook_folders_title)
                .content(R.string.no_audiobook_folders_summary)
                .positiveText(R.string.dialog_confirm)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        startActivity(new Intent(getActivity(), FolderChooserActivity.class));
                    }
                })
                .cancelable(false)
                .build();
    }

    /**
     * Returns the amount of columns the main-grid will need
     *
     * @return The amount of columns, but at least 2.
     */
    private int getAmountOfColumns() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        int columns = Math.round(dpWidth / getResources().getDimension(R.dimen.desired_medium_cover));
        return columns > 2 ? columns : 2;
    }

    @Override
    public void onResume() {
        super.onResume();

        baseApplication.addOnPlayStateChangedListener(this);
        baseApplication.addOnPositionChangedListener(this);

        // Scanning for new files here in case there are changes on the drive.
        baseApplication.bookLock.lock();
        try {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
            baseApplication.addOnBookAddedListener(this);
            baseApplication.addOnBookDeletedListener(this);
        } finally {
            baseApplication.bookLock.unlock();
        }

        baseApplication.addOnScannerStateChangedListener(this);

        getActivity().startService(BookAddingService.getRescanIntent(getActivity(), false));

        if (baseApplication.getCurrentBook() == null) {
            playerWidget.setVisibility(View.GONE);
        } else {
            playerWidget.setVisibility(View.VISIBLE);
        }

        initPlayerWidget();
        onPlayStateChanged(baseApplication.getPlayState());
        onPositionChanged(true);

        boolean audioFoldersEmpty = prefs.getAudiobookFolders().size() == 0;
        boolean noFolderWarningIsShowing = noFolderWarning.isShowing();
        if (audioFoldersEmpty && !noFolderWarningIsShowing) {
            noFolderWarning.show();
        }
        toggleRecyclerVisibilities(baseApplication.isScannerActive());
    }

    private void initPlayerWidget() {
        Book book = baseApplication.getCurrentBook();
        if (book != null) {
            // cover
            File coverFile = book.getCoverFile();
            String bookName = book.getName();
            Drawable coverReplacement = new CoverReplacement(bookName, getActivity());
            if (!book.isUseCoverReplacement() && coverFile.exists() && coverFile.canRead()) {
                Picasso.with(getActivity()).load(coverFile).fit().placeholder(coverReplacement).into(currentCover);
            } else {
                currentCover.setImageDrawable(coverReplacement);
            }

            // text
            currentText.setText(bookName);

            // progress
            ArrayList<Chapter> allChapters = book.getChapters();
            Chapter currentChapter = book.getCurrentChapter();
            float duration = 0;
            float timeTillBeginOfCurrentChapter = 0;
            for (Chapter c : allChapters) {
                duration += c.getDuration();
                if (allChapters.indexOf(c) < allChapters.indexOf(currentChapter)) {
                    timeTillBeginOfCurrentChapter += c.getDuration();
                }
            }
            int progress = Math.round((timeTillBeginOfCurrentChapter + book.getTime()) * 1000 / duration);
            progressBar.setProgress(progress);
        }
    }

    @Override
    public void onPlayStateChanged(final BaseApplication.PlayState state) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (state == BaseApplication.PlayState.PLAYING) {
                    currentPlaying.setImageResource(ThemeUtil.getResourceId(getActivity(), R.attr.book_shelf_pause));
                } else {
                    currentPlaying.setImageResource(ThemeUtil.getResourceId(getActivity(), R.attr.book_shelf_play));
                }
            }
        });
    }

    @Override
    public void onPositionChanged(boolean positionChanged) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                initPlayerWidget();
            }
        });
    }

    private void toggleRecyclerVisibilities(boolean scannerActive) {
        if (baseApplication.getAllBooks().size() == 0 && scannerActive) {
            recyclerReplacementView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            recyclerReplacementView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.current_playing:
                controller.playPause();
                break;
            case R.id.current:

                /**
                 * Workaround. If we use a fake cover image transition will fail. So we don't du image transition in that case.
                 */
                boolean fakeCover = baseApplication.getCurrentBook() != null && (baseApplication.getCurrentBook().isUseCoverReplacement() || !baseApplication.getCurrentBook().getCoverFile().exists());
                ActivityOptionsCompat options;
                if (fakeCover) {
                    //noinspection unchecked
                    options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity());
                } else {
                    options = ActivityOptionsCompat.makeSceneTransitionAnimation
                            (getActivity(), currentCover, getString(R.string.cover_transition));
                }

                ViewCompat.setTransitionName(currentCover, getString(R.string.cover_transition));

                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new BookPlayFragment(), BookPlayFragment.TAG)
                        .addToBackStack(null)
                        .commit();
                break;
            default:
                break;
        }
    }


    @Override
    public void onEditBookFinished(@NonNull Book book) {
        baseApplication.bookLock.lock();
        try {
            int oldIndex = baseApplication.getAllBooks().indexOf(book);
            Collections.sort(baseApplication.getAllBooks());
            int newIndex = baseApplication.getAllBooks().indexOf(book);
            adapter.notifyItemMoved(oldIndex, newIndex);
            adapter.notifyItemChanged(newIndex);

            initPlayerWidget();
        } finally {
            baseApplication.bookLock.unlock();
        }
    }

    @Override
    public void onBookAdded(final int position) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                toggleRecyclerVisibilities(baseApplication.isScannerActive());
                adapter.notifyItemInserted(position);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        baseApplication.removeOnBookAddedListener(this);
        baseApplication.removeOnBookDeletedListener(this);
        baseApplication.removeOnPlayStateChangedListener(this);
        baseApplication.removeOnPositionChangedListener(this);
        baseApplication.removeOnScannerStateChangedListener(this);
    }

    @Override
    public void onBookDeleted(final int position) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                toggleRecyclerVisibilities(baseApplication.isScannerActive());
                adapter.notifyItemRemoved(position);
            }
        });
    }

    @Override
    public void onScannerStateChanged(final boolean active) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                toggleRecyclerVisibilities(active);
            }
        });
    }

}
