package de.ph1b.audiobook.activity;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
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

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.adapter.MediaAdapter;
import de.ph1b.audiobook.dialog.AudioFolderOverviewDialog;
import de.ph1b.audiobook.dialog.EditBookDialog;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Chapter;
import de.ph1b.audiobook.service.BookAddingService;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.utils.BaseApplication;
import de.ph1b.audiobook.utils.CustomOnSimpleGestureListener;
import de.ph1b.audiobook.utils.ImageHelper;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.PrefsManager;

public class BookShelfActivity extends BaseActivity implements View.OnClickListener, EditBookDialog.OnEditBookFinished, RecyclerView.OnItemTouchListener, BaseApplication.OnBookAddedListener, BaseApplication.OnBookDeletedListener, BaseApplication.OnPlayStateChangedListener, BaseApplication.OnPositionChangedListener {

    private static final String TAG = BookShelfActivity.class.getSimpleName();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AudioFolderOverviewDialog audioFolderOverviewDialog = new AudioFolderOverviewDialog();
    private MediaAdapter adapter;
    private ImageView currentCover;
    private TextView currentText;
    private ViewGroup current;
    private PrefsManager prefs;
    private GestureDetectorCompat detector;
    private Book bookToEdit;
    private float scrollBy = 0;
    private BaseApplication baseApplication;
    private ServiceController controller;
    private ImageButton currentPlaying;
    private ProgressBar progressBar;
    private AlertDialog noFolderWarning;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_shelf);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setTitle(this.getString(R.string.app_name));

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
                        audioFolderOverviewDialog.show(getFragmentManager(), TAG);
                    }
                })
                .cancelable(false)
                .build();

        current = (ViewGroup) findViewById(R.id.current);
        currentCover = (ImageView) findViewById(R.id.current_cover);
        currentText = (TextView) findViewById(R.id.current_text);
        currentPlaying = (ImageButton) findViewById(R.id.current_playing);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.listMediaView);

        current.setOnClickListener(this);
        currentPlaying.setOnClickListener(this);
        MediaAdapter.OnItemClickListener onClickListener = new MediaAdapter.OnItemClickListener() {
            @Override
            public void onCoverClicked(int position) {
                Book book = adapter.getItem(position);
                if (baseApplication.getCurrentBook() != book) {
                    baseApplication.setCurrentBook(book);
                }
                prefs.setCurrentBookId(book.getId());
                startActivity(new Intent(BookShelfActivity.this, BookPlayActivity.class));
            }

            @Override
            public void onMenuClicked(final int position) {
                bookToEdit = adapter.getItem(position);

                EditBookDialog editBookDialog = new EditBookDialog();
                Bundle bundle = new Bundle();

                ArrayList<Bitmap> bitmap = new ArrayList<>();
                Bitmap defaultCover = BitmapFactory.decodeFile(bookToEdit.getCover());
                if (defaultCover != null)
                    bitmap.add(defaultCover);

                bundle.putParcelableArrayList(EditBookDialog.BOOK_COVER, bitmap);
                bundle.putString(EditBookDialog.DIALOG_TITLE, getString(R.string.edit_book_title));
                bundle.putString(EditBookDialog.BOOK_NAME, bookToEdit.getName());

                editBookDialog.setArguments(bundle);
                editBookDialog.show(getFragmentManager(), TAG);
            }
        };

        CustomOnSimpleGestureListener.OnItemLongClickListener onLongClickListener = new CustomOnSimpleGestureListener.OnItemLongClickListener() {
            @Override
            public void onItemLongClicked(int position, View view) {
                ClipData cData = ClipData.newPlainText("position", String.valueOf(position));
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                view.startDrag(cData, shadowBuilder, view, 0);
            }
        };

        adapter = new MediaAdapter(baseApplication.getAllBooks(), this, onClickListener);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this, getAmountOfColumns()));
        recyclerView.setAdapter(adapter);
        recyclerView.addOnItemTouchListener(this);
        final Handler handler = new Handler();

        final Runnable smoothScroll = new Runnable() {
            @Override
            public void run() {
                recyclerView.smoothScrollBy(0, Math.round(scrollBy));
                handler.postDelayed(this, 50);
            }
        };

        View.OnDragListener onDragListener = new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {

                int action = event.getAction();
                switch (action) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        return event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                    case DragEvent.ACTION_DRAG_ENTERED:
                        return true;
                    case DragEvent.ACTION_DRAG_LOCATION:

                        int height = recyclerView.getMeasuredHeight();

                        float scrollArea = height / 4;
                        float maxScrollStrength = scrollArea / 3;

                        float y = event.getY();

                        scrollBy = 0;
                        if (y <= scrollArea && y > 0) {
                            scrollBy = maxScrollStrength * (y / scrollArea - 1);
                        } else if (y >= (height - scrollArea) && y <= height) {
                            scrollBy = maxScrollStrength * (1 - y / height);

                            float factor = 1 + (y * y - height * height) /
                                    (2 * scrollArea * height - scrollArea * scrollArea);

                            scrollBy = maxScrollStrength * factor;
                        }
                        handler.removeCallbacks(smoothScroll);
                        if (scrollBy != 0) {
                            handler.removeCallbacks(smoothScroll);
                            handler.post(smoothScroll);
                        }

                        return true;
                    case DragEvent.ACTION_DRAG_EXITED:
                        return true;
                    case DragEvent.ACTION_DROP:
                        ClipData.Item item = event.getClipData().getItemAt(0);
                        String dragData = String.valueOf(item.getText());
                        int from = Integer.valueOf(dragData);

                        float endX = event.getX();
                        float endY = event.getY();
                        L.d(TAG, endX + "/" + endY);
                        View endChild = recyclerView.findChildViewUnder(endX, endY);
                        int to = recyclerView.getChildAdapterPosition(endChild);

                        if (from != -1 && to != -1) {
                            adapter.swapItems(from, to);
                            return true;
                        } else if (from != -1) {
                            to = adapter.getItemCount() - 1;
                            adapter.swapItems(from, to);
                            return true;
                        }
                        return false;
                    case DragEvent.ACTION_DRAG_ENDED:
                        handler.removeCallbacks(smoothScroll);
                        scrollBy = 0;
                        return true;
                }
                return false;
            }
        };
        recyclerView.setOnDragListener(onDragListener);
        detector = new GestureDetectorCompat(this,
                new CustomOnSimpleGestureListener(recyclerView, onLongClickListener));
    }

    private void initPlayerWidget() {
        long currentBookPosition = prefs.getCurrentBookId();

        boolean widgetInitialized = false;
        for (final Book b : adapter.getBooks()) {
            if (b.getId() == currentBookPosition) {
                //setting cover
                Picasso.with(this).load(new File(b.getCover())).into(currentCover);

                //setting text
                currentText.setText(b.getName());
                widgetInitialized = true;
                break;
            }
        }
        if (!widgetInitialized) {
            current.setVisibility(View.GONE);
        } else {
            current.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_only_settings, menu);
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
                Intent i = new Intent(BookShelfActivity.this, BookPlayActivity.class);
                startActivity(i);
                break;
            default:
                break;
        }
    }

    @Override
    public void onEditBookFinished(String bookName, Bitmap cover, Boolean success) {
        if (success) {
            bookToEdit.setName(bookName);
            if (cover != null) {
                String coverPath = ImageHelper.saveCover(cover, this);
                bookToEdit.setCover(coverPath);
            }
            adapter.updateItem(bookToEdit);
            initPlayerWidget();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        detector.onTouchEvent(motionEvent);
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {

    }

    @Override
    public void onBookAdded() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        }, 100); // Necessary because of bug in RecyclerView: https://code.google.com/p/android/issues/detail?id=77232
    }

    @Override
    public void onPause() {
        super.onPause();

        baseApplication.removeOnBookAddedListener(this);
        baseApplication.removeOnBookDeletedListener(this);
        baseApplication.removeOnPlayStateChangedListener(this);
        baseApplication.removeOnPositionChangedListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        baseApplication.addOnPlayStateChangedListener(this);
        baseApplication.addOnPositionChangedListener(this);

        // Scanning for new files here in case there are changes on the drive.
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        }, 100);
        baseApplication.addOnBookAddedListener(this);
        baseApplication.addOnBookDeletedListener(this);

        startService(BookAddingService.getUpdateIntent(this));

        initPlayerWidget();
        onPlayStateChanged(baseApplication.getPlayState());
        onPositionChanged();

        boolean audioFolderOverviewDialogIsVisible = audioFolderOverviewDialog.getDialog() != null && audioFolderOverviewDialog.getDialog().isShowing();
        boolean audioFoldersEmpty = prefs.getAudiobookFolders().size() == 0;
        boolean noFolderWarningIsShowing = noFolderWarning.isShowing();
        if (audioFoldersEmpty && !audioFolderOverviewDialogIsVisible && !noFolderWarningIsShowing) {
            noFolderWarning.show();
        }
    }

    @Override
    public void onBookDeleted() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        }, 100); // Necessary because of bug in RecyclerView: https://code.google.com/p/android/issues/detail?id=77232
    }

    @Override
    public void onPlayStateChanged(final BaseApplication.PlayState state) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (state == BaseApplication.PlayState.PLAYING) {
                    currentPlaying.setImageResource(R.drawable.ic_pause_grey600_48dp);
                } else {
                    currentPlaying.setImageResource(R.drawable.ic_play_arrow_grey600_48dp);
                }
            }
        });
    }

    @Override
    public void onPositionChanged() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Book book = baseApplication.getCurrentBook();
                if (book != null) {
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
        });
    }
}
