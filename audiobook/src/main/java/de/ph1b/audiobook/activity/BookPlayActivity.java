package de.ph1b.audiobook.activity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.transition.Transition;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.dialog.BookmarkDialogFragment;
import de.ph1b.audiobook.dialog.JumpToPositionDialogFragment;
import de.ph1b.audiobook.dialog.PlaybackSpeedDialogFragment;
import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Chapter;
import de.ph1b.audiobook.model.DataBaseHelper;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.uitools.CoverReplacement;
import de.ph1b.audiobook.uitools.ImageHelper;
import de.ph1b.audiobook.uitools.PlayPauseDrawable;
import de.ph1b.audiobook.uitools.ThemeUtil;
import de.ph1b.audiobook.utils.Communication;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.PrefsManager;

/**
 * Created by Paul Woitaschek (woitaschek@posteo.de, paul-woitaschek.de) on 12.07.15.
 * Base class for book playing interaction.
 */
public class BookPlayActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = BookPlayActivity.class.getSimpleName();
    private static final String BOOK_ID = "bookId";
    private static final Communication communication = Communication.getInstance();
    private final PlayPauseDrawable playPauseDrawable = new PlayPauseDrawable();
    @Nullable
    private Snackbar snackbar;
    private TextView playedTimeView;
    private SeekBar seekBar;
    private volatile Spinner bookSpinner;
    private TextView maxTimeView;
    private PrefsManager prefs;
    private ServiceController controller;
    private long bookId;
    private DataBaseHelper db;
    private CoordinatorLayout coordinatorLayout;
    private TextView timerCountdownView;
    private CountDownTimer countDownTimer;

    private final Communication.SimpleBookCommunication listener = new Communication.SimpleBookCommunication() {

        @Override
        public void onSleepStateChanged() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ActivityCompat.invalidateOptionsMenu(BookPlayActivity.this);
                    if (MediaPlayerController.sleepTimerActive) {
                        int minutes = prefs.getSleepTime();
                        String message = getString(R.string.sleep_timer_started) + " " + minutes + " " +
                                getString(R.string.minutes);
                        snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG).setAction(
                                R.string.stop, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        controller.toggleSleepSand();
                                    }
                                });
                        ThemeUtil.theme(snackbar);
                        snackbar.show();
                        long delay = System.currentTimeMillis() - MediaPlayerController.sleepTimerDelay;
                        showTimerCountdown(minutes * 60000 - delay);
                    } else showTimerCountdown(0);
                }
            });
        }

        @Override
        public void onBookContentChanged(@NonNull final Book book) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    L.d(TAG, "onBookContentChangedReciever called with bookId=" + book.getId());
                    if (book.getId() == bookId) {

                        List<Chapter> chapters = book.getChapters();
                        Chapter chapter = book.getCurrentChapter();

                        int position = chapters.indexOf(chapter);
                        /**
                         * Setting position as a tag, so we can make sure onItemSelected is only fired when
                         * the user changes the position himself.
                         */
                        bookSpinner.setTag(position);
                        bookSpinner.setSelection(position, true);
                        int duration = chapter.getDuration();
                        seekBar.setMax(duration);
                        maxTimeView.setText(formatTime(duration, duration));

                        // Setting seekBar and played time view
                        if (!seekBar.isPressed()) {
                            int progress = book.getTime();
                            seekBar.setProgress(progress);
                            playedTimeView.setText(formatTime(progress, duration));
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

    };

    public static Intent newIntent(Context c, long bookId) {
        Intent intent = new Intent(c, BookPlayActivity.class);
        intent.putExtra(BOOK_ID, bookId);
        return intent;
    }

    public static PendingIntent getTaskStackPI(Context c, long bookId) {
        // Use TaskStackBuilder to build the back stack and get the PendingIntent
        Intent bookPlayIntent = newIntent(c, bookId);
        return TaskStackBuilder.create(c)
                .addParentStack(BookShelfActivity.class)
                .addNextIntentWithParentStack(bookPlayIntent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static String formatTime(int ms, int duration) {
        String h = String.valueOf(TimeUnit.MILLISECONDS.toHours(ms));
        String m = String.format("%02d", (TimeUnit.MILLISECONDS.toMinutes(ms) % 60));
        String s = String.format("%02d", (TimeUnit.MILLISECONDS.toSeconds(ms) % 60));

        if (TimeUnit.MILLISECONDS.toHours(duration) == 0) {
            return m + ":" + s;
        } else {
            return h + ":" + m + ":" + s;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PrefsManager.getInstance(this);
        db = DataBaseHelper.getInstance(this);
        controller = new ServiceController(this);

        setContentView(R.layout.activity_book_play);

        bookId = getIntent().getLongExtra(BOOK_ID, -1);
        final Book book = db.getBook(bookId);
        if (book == null) {
            startActivity(new Intent(this, BookShelfActivity.class));
            return;
        }

        //setup actionbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(book.getName());


        //init buttons
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        FloatingActionButton playButton = (FloatingActionButton) findViewById(R.id.play);
        View previous_button = findViewById(R.id.previous);
        View next_button = findViewById(R.id.next);
        playedTimeView = (TextView) findViewById(R.id.played);
        final ImageView coverView = (ImageView) findViewById(R.id.book_cover);
        maxTimeView = (TextView) findViewById(R.id.maxTime);
        bookSpinner = (Spinner) findViewById(R.id.book_spinner);
        timerCountdownView = (TextView) findViewById(R.id.timerView);

        //setup buttons
        playButton.setIconDrawable(playPauseDrawable);
        ThemeUtil.theme(seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //sets text to adjust while using seekBar
                playedTimeView.setText(formatTime(progress, seekBar.getMax()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                Book currentBook = db.getBook(bookId);
                if (currentBook != null) {
                    controller.changeTime(progress, currentBook.getCurrentChapter()
                            .getPath());
                    playedTimeView.setText(formatTime(progress, seekBar.getMax()));
                }
            }
        });

        // adapter
        List<Chapter> chapters = book.getChapters();
        final List<String> chaptersAsStrings = new ArrayList<>(chapters.size());
        for (int i = 0; i < chapters.size(); i++) {
            String chapterName = chapters.get(i).getName();

            // cutting leading zeros
            chapterName = chapterName.replaceFirst("^0", "");
            String number = String.valueOf(i + 1);

            // desired format is "1 - Title"
            if (!chapterName.startsWith(number + " - ")) { // if title does not match desired format
                if (chapterName.startsWith(number)) {
                    // if it starts with a number, a " - " should follow
                    chapterName = number + " - " + chapterName.substring(chapterName.indexOf(number)
                            + number.length());
                } else {
                    // if the name does not match at all, set the correct format
                    chapterName = number + " - " + chapterName;
                }
            }

            chaptersAsStrings.add(chapterName);
        }

        SpinnerAdapter adapter = new ArrayAdapter<String>(this,
                R.layout.activity_book_play_spinner, chaptersAsStrings) {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                final TextView textView = (TextView) super.getDropDownView(position, convertView,
                        parent);

                // this is necessary due to a bug in android causing the layout to be ignored.
                // see: http://stackoverflow.com/questions/14139106/spinner-does-not-wrap-text-is-th
                // is-an-android-bug
                textView.post(new Runnable() {
                    @Override
                    public void run() {
                        textView.setSingleLine(false);
                    }
                });

                // highlights the selected item and un-highlights an item if it is not selected.
                // default implementation uses a ViewHolder, so this is necessary.
                if (position == bookSpinner.getSelectedItemPosition()) {
                    textView.setBackgroundColor(getResources().getColor(ThemeUtil.getResourceId(
                            BookPlayActivity.this, R.attr.colorAccent)));
                    textView.setTextColor(getResources().getColor(R.color.abc_primary_text_material_dark));
                } else {
                    textView.setBackgroundColor(getResources().getColor(ThemeUtil.getResourceId(
                            BookPlayActivity.this, android.R.attr.windowBackground)));
                    textView.setTextColor(getResources().getColor(ThemeUtil.getResourceId(
                            BookPlayActivity.this, android.R.attr.textColorPrimary)));
                }

                return textView;
            }
        };
        bookSpinner.setAdapter(adapter);

        bookSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int newPosition, long id) {
                if (parent.getTag() != null && ((int) parent.getTag()) != newPosition) {
                    L.i(TAG, "spinner, onItemSelected, firing:" + newPosition);
                    controller.changeTime(0, book.getChapters().get(
                            newPosition).getPath());
                    parent.setTag(newPosition);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // (Cover)
        File coverFile = book.getCoverFile();
        final Drawable coverReplacement = new CoverReplacement(book.getName(), this);
        if (!book.isUseCoverReplacement() && coverFile.exists() && coverFile.canRead()) {
            Picasso.with(this).load(coverFile).placeholder(coverReplacement).into(coverView);
        } else {
            // this hack is necessary because otherwise the transition will fail
            coverView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    coverView.setImageBitmap(ImageHelper.drawableToBitmap(coverReplacement, coverView.getWidth(), coverView.getHeight()));
                    coverView.getViewTreeObserver().removeOnPreDrawListener(this);
                    return true;
                }
            });
        }

        // Next/Prev/spinner hiding
        if (book.getChapters().size() == 1) {
            next_button.setVisibility(View.GONE);
            previous_button.setVisibility(View.GONE);
            bookSpinner.setVisibility(View.GONE);
        } else {
            next_button.setVisibility(View.VISIBLE);
            previous_button.setVisibility(View.VISIBLE);
            bookSpinner.setVisibility(View.VISIBLE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Transition enterTransition = getWindow().getEnterTransition();
            enterTransition.excludeTarget(R.id.toolbar, true);
            enterTransition.excludeTarget(android.R.id.statusBarBackground, true);
            enterTransition.excludeTarget(android.R.id.navigationBarBackground, true);
            getWindow().setReturnTransition(null);
        }

        // Sleep timer countdown view
        if (MediaPlayerController.sleepTimerActive) {
            long delay = System.currentTimeMillis() - MediaPlayerController.sleepTimerDelay;
            int minutes = prefs.getSleepTime();
            showTimerCountdown(minutes * 60000 - delay);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play:
            case R.id.book_cover:
                controller.playPause();
                break;
            case R.id.rewind:
                controller.rewind();
                break;
            case R.id.fastForward:
                controller.fastForward();
                break;
            case R.id.next:
                controller.next();
                break;
            case R.id.previous:
                controller.previous();
                break;
            case R.id.played:
                launchJumpToPositionDialog();
                break;
            default:
                break;
        }
    }

    private void launchJumpToPositionDialog() {
        new JumpToPositionDialogFragment().show(getSupportFragmentManager(), JumpToPositionDialogFragment.TAG);
    }

    private void showTimerCountdown(long time) {
        if (time != 0) {
            timerCountdownView.setVisibility(View.VISIBLE);
            countDownTimer = new CountDownTimer(time, 1000) {
                public void onTick(long m) {
                    int timer = (int) m - 1000;
                    timerCountdownView.setText(formatTime(timer, timer));
                }

                public void onFinish() {
                    timerCountdownView.setVisibility(View.GONE);
                    L.i(TAG, "Countdown timer finished");
                }
            }.start();
        } else if (countDownTimer != null) {
            timerCountdownView.setVisibility(View.GONE);
            countDownTimer.cancel();
            L.i(TAG, "Countdown timer canceled");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.book_play, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem timeLapseItem = menu.findItem(R.id.action_time_lapse);
        timeLapseItem.setVisible(MediaPlayerController.canSetSpeed());
        MenuItem sleepTimerItem = menu.findItem(R.id.action_sleep);
        if (MediaPlayerController.sleepTimerActive) {
            sleepTimerItem.setIcon(R.drawable.ic_alarm_on_white_24dp);
        } else {
            sleepTimerItem.setIcon(R.drawable.ic_snooze_white_24dp);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_time_change:
                launchJumpToPositionDialog();
                return true;
            case R.id.action_sleep:
                controller.toggleSleepSand();
                if (MediaPlayerController.sleepTimerActive && snackbar != null) {
                    snackbar.dismiss();
                }
                if (prefs.setBookmarkOnSleepTimer() && !MediaPlayerController.sleepTimerActive) {
                    String date = DateUtils.formatDateTime(this, System.currentTimeMillis(), DateUtils.FORMAT_SHOW_DATE |
                            DateUtils.FORMAT_SHOW_TIME |
                            DateUtils.FORMAT_NUMERIC_DATE);
                    BookmarkDialogFragment.addBookmark(bookId, date + ": " +
                            getString(R.string.action_sleep), this);
                }
                return true;
            case R.id.action_time_lapse:
                new PlaybackSpeedDialogFragment().show(getSupportFragmentManager(),
                        PlaybackSpeedDialogFragment.TAG);
                return true;
            case R.id.action_bookmark:
                BookmarkDialogFragment.newInstance(bookId).show(getSupportFragmentManager(),
                        BookmarkDialogFragment.TAG);
                return true;
            case android.R.id.home:
                ActivityCompat.finishAfterTransition(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setPlayState(boolean animated) {
        if (MediaPlayerController.getPlayState() == MediaPlayerController.PlayState.PLAYING) {
            playPauseDrawable.transformToPause(animated);
        } else {
            playPauseDrawable.transformToPlay(animated);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        setPlayState(false);

        Book book = db.getBook(bookId);
        if (book != null) {
            listener.onBookContentChanged(book);
        }

        ActivityCompat.invalidateOptionsMenu(this);

        communication.addBookCommunicationListener(listener);

        // Sleep timer countdown view
        if (MediaPlayerController.sleepTimerActive) {
            long delay = System.currentTimeMillis() - MediaPlayerController.sleepTimerDelay;
            int minutes = prefs.getSleepTime();
            showTimerCountdown(minutes * 60000 - delay);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        communication.removeBookCommunicationListener(listener);
    }
}
