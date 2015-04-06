package de.ph1b.audiobook.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Menu;
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
import de.ph1b.audiobook.adapter.BookAdapter;
import de.ph1b.audiobook.dialog.EditBookDialog;
import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Chapter;
import de.ph1b.audiobook.service.BookAddingService;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.uitools.CoverReplacement;
import de.ph1b.audiobook.uitools.ThemeUtil;
import de.ph1b.audiobook.utils.BaseApplication;
import de.ph1b.audiobook.utils.PrefsManager;

public class BookShelfActivity extends BaseActivity implements View.OnClickListener, EditBookDialog.OnEditBookFinished, BaseApplication.OnBookAddedListener, BaseApplication.OnBookDeletedListener, BaseApplication.OnPlayStateChangedListener, BaseApplication.OnPositionChangedListener, BaseApplication.OnScannerStateChangedListener, BaseApplication.OnCurrentBookChangedListener {

    private static final String TAG = BookShelfActivity.class.getSimpleName();
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
    private AlertDialog noFolderWarning;
    private RecyclerView recyclerView;
    private ProgressBar recyclerReplacementView;

    public static Intent getClearStarterIntent(Context c) {
        Intent intent = new Intent(c, BookShelfActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_shelf);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setTitle(this.getString(R.string.app_name));

        if (getIntent().hasExtra(MediaPlayerController.MALFORMED_FILE)) {
            String malformedFile = getIntent().getStringExtra(MediaPlayerController.MALFORMED_FILE);
            new MaterialDialog.Builder(this)
                    .title(R.string.mal_file_title)
                    .content(getString(R.string.mal_file_message) + "\n\n" + malformedFile)
                    .show();
        }

        baseApplication = (BaseApplication) getApplication();
        prefs = new PrefsManager(this);
        controller = new ServiceController(this);
        noFolderWarning = new MaterialDialog.Builder(this)
                .title(R.string.no_audiobook_folders_title)
                .content(R.string.no_audiobook_folders_summary)
                .positiveText(R.string.dialog_confirm)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        startActivity(new Intent(BookShelfActivity.this, FolderChooserActivity.class));
                    }
                })
                .cancelable(false)
                .build();

        playerWidget = (ViewGroup) findViewById(R.id.current);
        currentCover = (ImageView) findViewById(R.id.current_cover);
        currentText = (TextView) findViewById(R.id.current_text);
        currentPlaying = (ImageButton) findViewById(R.id.current_playing);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerReplacementView = (ProgressBar) findViewById(R.id.recyclerReplacement);

        playerWidget.setOnClickListener(this);
        currentPlaying.setOnClickListener(this);
        BookAdapter.OnItemClickListener onClickListener = new BookAdapter.OnItemClickListener() {
            @Override
            public void onCoverClicked(int position, ImageView imageView) {
                Book book = adapter.getItem(position);
                baseApplication.setCurrentBook(book);
                prefs.setCurrentBookId(book.getId());
                Intent intent = new Intent(BookShelfActivity.this, BookPlayActivity.class);
                ActivityOptionsCompat options =
                        ActivityOptionsCompat.makeSceneTransitionAnimation(BookShelfActivity.this,
                                imageView, getString(R.string.cover_transition));
                ActivityCompat.startActivity(BookShelfActivity.this, intent, options.toBundle());
                //  initPlayerWidget();
            }

            @Override
            public void onMenuClicked(final int position) {
                Book bookToEdit = adapter.getItem(position);

                EditBookDialog editBookDialog = new EditBookDialog();
                Bundle bundle = new Bundle();

                ArrayList<Bitmap> bitmap = new ArrayList<>();
                File coverFile = bookToEdit.getCoverFile();
                if (coverFile.exists() && coverFile.canRead()) {
                    Bitmap defaultCover = BitmapFactory.decodeFile(coverFile.getAbsolutePath());
                    if (defaultCover != null) {
                        bitmap.add(defaultCover);
                    }
                }

                bundle.putParcelableArrayList(EditBookDialog.BOOK_COVER, bitmap);
                bundle.putLong(Book.TAG, bookToEdit.getId());

                editBookDialog.setArguments(bundle);
                editBookDialog.show(getFragmentManager(), TAG);
            }
        };

        adapter = new BookAdapter(baseApplication.getAllBooks(), this, onClickListener);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this, getAmountOfColumns()));
        recyclerView.setAdapter(adapter);
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
        baseApplication.addOnCurrentBookChangedListener(this);

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

        startService(BookAddingService.getRescanIntent(this, false));

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
            Drawable coverReplacement = new CoverReplacement(bookName, this);
            if (!book.isUseCoverReplacement() && coverFile.exists() && coverFile.canRead()) {
                Picasso.with(this).load(coverFile).placeholder(coverReplacement).into(currentCover);
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
                    currentPlaying.setImageResource(ThemeUtil.getResourceId(BookShelfActivity.this, R.attr.book_shelf_pause));
                } else {
                    currentPlaying.setImageResource(ThemeUtil.getResourceId(BookShelfActivity.this, R.attr.book_shelf_play));
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.book_shelf, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
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
                Intent intent = new Intent(BookShelfActivity.this, BookPlayActivity.class);
                ActivityOptionsCompat options =
                        ActivityOptionsCompat.makeSceneTransitionAnimation(BookShelfActivity.this,
                                currentCover, getString(R.string.cover_transition));
                ActivityCompat.startActivity(BookShelfActivity.this, intent, options.toBundle());
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
        baseApplication.removeOnCurrentBookChangedListener(this);
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

    @Override
    public void onCurrentBookChanged(Book book) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                initPlayerWidget();
            }
        });
    }
}
