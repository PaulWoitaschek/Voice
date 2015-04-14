package de.ph1b.audiobook.activity;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionSet;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.dialog.BookmarkDialog;
import de.ph1b.audiobook.dialog.JumpToPositionDialog;
import de.ph1b.audiobook.dialog.SetPlaybackSpeedDialog;
import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Chapter;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.uitools.CoverReplacement;
import de.ph1b.audiobook.uitools.ThemeUtil;
import de.ph1b.audiobook.utils.BaseApplication;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.PrefsManager;

public class BookPlayActivity extends BaseActivity implements View.OnClickListener, BaseApplication.OnPlayStateChangedListener, BaseApplication.OnPositionChangedListener, BaseApplication.OnSleepStateChangedListener {

    private static final String TAG = BookPlayActivity.class.getSimpleName();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile int duration = 0;
    private FloatingActionButton playButton;
    private TextView playedTimeView;
    private SeekBar seekBar;
    private volatile Spinner bookSpinner;
    private TextView maxTimeView;
    private PrefsManager prefs;
    private ServiceController controller;
    private BaseApplication baseApplication;
    private Book book;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_play);

        if (Build.VERSION.SDK_INT >= 21) {
            Transition sharedElementEnterTransition = TransitionInflater.from(this).inflateTransition(android.R.transition.move);
            //sharedElementEnterTransition.setStartDelay(100);
            sharedElementEnterTransition.excludeTarget(android.R.id.statusBarBackground, true);
            sharedElementEnterTransition.excludeTarget(android.R.id.navigationBarBackground, true);
            sharedElementEnterTransition.excludeTarget(R.id.toolbar, true);
            sharedElementEnterTransition.setDuration(500);
            getWindow().setSharedElementEnterTransition(sharedElementEnterTransition);

            TransitionSet enterTransitionSet = new TransitionSet();
            enterTransitionSet.setDuration(500);
            enterTransitionSet.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);
            enterTransitionSet.excludeTarget(android.R.id.statusBarBackground, true);
            enterTransitionSet.excludeTarget(android.R.id.navigationBarBackground, true);
            enterTransitionSet.excludeTarget(R.id.toolbar, true);

            Transition player = new Slide(Gravity.TOP);
            player.addTarget(R.id.previous);
            player.addTarget(R.id.rewind);
            player.addTarget(R.id.play);
            player.addTarget(R.id.fastForward);
            player.addTarget(R.id.next);
            enterTransitionSet.addTransition(player);

            Transition info = new Fade();
            info.setDuration(500);
            enterTransitionSet.addTransition(info);

            getWindow().setEnterTransition(enterTransitionSet);

            Transition returnTransition = new Fade();
            returnTransition.setDuration(100);
            returnTransition.excludeTarget(android.R.id.statusBarBackground, true);
            returnTransition.excludeTarget(android.R.id.navigationBarBackground, true);
            returnTransition.excludeTarget(R.id.toolbar, true);
            getWindow().setReturnTransition(returnTransition);
            getWindow().setExitTransition(returnTransition);

            getWindow().setTransitionBackgroundFadeDuration(500);
        }

        prefs = new PrefsManager(this);
        controller = new ServiceController(this);
        baseApplication = (BaseApplication) getApplication();
        book = baseApplication.getCurrentBook();

        if (book == null) {
            startActivity(BookShelfActivity.getClearStarterIntent(this));
            finish();
            return;
        }

        //setup actionbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(book.getName());

        //init buttons
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        playButton = (FloatingActionButton) findViewById(R.id.play);
        ImageButton rewind_button = (ImageButton) findViewById(R.id.rewind);
        ImageButton fast_forward_button = (ImageButton) findViewById(R.id.fastForward);
        ImageButton previous_button = (ImageButton) findViewById(R.id.previous);
        ImageButton next_button = (ImageButton) findViewById(R.id.next);
        playedTimeView = (TextView) findViewById(R.id.played);
        ImageView coverView = (ImageView) findViewById(R.id.book_cover);
        maxTimeView = (TextView) findViewById(R.id.maxTime);
        bookSpinner = (Spinner) findViewById(R.id.book_spinner);

        //setup buttons
        rewind_button.setOnClickListener(this);
        fast_forward_button.setOnClickListener(this);
        previous_button.setOnClickListener(this);
        next_button.setOnClickListener(this);
        playButton.setOnClickListener(this);
        playedTimeView.setOnClickListener(this);
        ThemeUtil.theme(seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                playedTimeView.setText(formatTime(progress)); //sets text to adjust while using seekBar
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                controller.changeTime(progress, baseApplication.getCurrentBook().getCurrentChapter().getPath());
                playedTimeView.setText(formatTime(progress));
            }
        });

        // adapter
        ArrayList<String> chaptersAsStrings = new ArrayList<>();
        for (Chapter c : baseApplication.getCurrentBook().getChapters()) {
            chaptersAsStrings.add(c.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.activity_book_play_spinner, chaptersAsStrings);
        adapter.setDropDownViewResource(R.layout.activity_book_play_spinner);
        bookSpinner.setAdapter(adapter);

        bookSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int newPosition, long id) {
                if (parent.getTag() != null && ((int) parent.getTag()) != newPosition) {
                    L.i(TAG, "spinner, onItemSelected, firing:" + newPosition);
                    controller.changeTime(0, baseApplication.getCurrentBook().getChapters().get(newPosition).getPath());
                    parent.setTag(newPosition);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // (Cover)
        File coverFile = book.getCoverFile();
        Drawable coverReplacement = new CoverReplacement(baseApplication.getCurrentBook().getName(), this);
        if (!book.isUseCoverReplacement() && coverFile.exists() && coverFile.canRead()) {
            Picasso.with(this).load(coverFile).placeholder(coverReplacement).into(coverView);
        } else {
            coverView.setImageDrawable(coverReplacement);
        }

        // Next/Prev/spinner hiding
        if (baseApplication.getCurrentBook().getChapters().size() == 1) {
            next_button.setVisibility(View.GONE);
            previous_button.setVisibility(View.GONE);
            bookSpinner.setVisibility(View.GONE);
        } else {
            next_button.setVisibility(View.VISIBLE);
            previous_button.setVisibility(View.VISIBLE);
            bookSpinner.setVisibility(View.VISIBLE);
        }
    }

    private String formatTime(int ms) {
        String h = String.valueOf(TimeUnit.MILLISECONDS.toHours(ms));
        String m = String.format("%02d", (TimeUnit.MILLISECONDS.toMinutes(ms) % 60));
        String s = String.format("%02d", (TimeUnit.MILLISECONDS.toSeconds(ms) % 60));
        return h + ":" + m + ":" + s;
    }

    @Override
    public void onResume() {
        super.onResume();

        onPositionChanged(true);
        onPlayStateChanged(baseApplication.getPlayState());
    }

    @Override
    public void onPositionChanged(boolean positionChanged) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                /**
                 * Setting position as a tag, so we can make sure onItemSelected is only fired when
                 * the user changes the position himself.
                 */
                ArrayList<Chapter> chapters = book.getChapters();
                Chapter chapter = book.getCurrentChapter();

                int position = chapters.indexOf(chapter);
                bookSpinner.setTag(position);
                bookSpinner.setSelection(position, true);
                duration = chapter.getDuration();
                seekBar.setMax(duration);
                maxTimeView.setText(formatTime(duration));

                // Setting seekBar and played time view
                if (!seekBar.isPressed()) {
                    int progress = book.getTime();
                    seekBar.setProgress(progress);
                    playedTimeView.setText(formatTime(progress));
                }
            }
        });
    }

    @Override
    public void onPlayStateChanged(final BaseApplication.PlayState state) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (state == BaseApplication.PlayState.PLAYING) {
                    playButton.setIcon(R.drawable.ic_pause_white_24dp);
                } else {
                    playButton.setIcon(R.drawable.ic_play_arrow_white_24dp);
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play:
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
        JumpToPositionDialog dialog = new JumpToPositionDialog();
        Bundle bundle = new Bundle();
        dialog.setArguments(bundle);
        dialog.show(getFragmentManager(), "timePicker");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.book_play, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem timeLapseItem = menu.findItem(R.id.action_time_lapse);
        timeLapseItem.setVisible(MediaPlayerController.playerCanSetSpeed);
        MenuItem sleepTimerItem = menu.findItem(R.id.action_sleep);
        if (baseApplication.isSleepTimerActive()) {
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
                return true;
            case R.id.action_time_lapse:
                SetPlaybackSpeedDialog dialog = new SetPlaybackSpeedDialog();
                dialog.show(getFragmentManager(), TAG);
                return true;
            case R.id.action_bookmark:
                BookmarkDialog bookmarkDialog = new BookmarkDialog();
                bookmarkDialog.show(getFragmentManager(), TAG);
                return true;
            case android.R.id.home:
            case R.id.home:
                onBackPressed();
                //ActivityCompat.finishAfterTransition(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        baseApplication.addOnPlayStateChangedListener(this);
        baseApplication.addOnPositionChangedListener(this);
        baseApplication.addOnSleepStateChangedListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        baseApplication.removeOnPlayStateChangedListener(this);
        baseApplication.removeOnPositionChangedListener(this);
        baseApplication.removeOnSleepStateChangedListener(this);
    }

    @Override
    public void onSleepStateChanged(final boolean active) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                invalidateOptionsMenu();
                if (active) {
                    int minutes = prefs.getSleepTime();
                    String message = getString(R.string.sleep_timer_started) + minutes + " " + getString(R.string.minutes);
                    Toast.makeText(BookPlayActivity.this, message, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(BookPlayActivity.this, R.string.sleep_timer_stopped, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
