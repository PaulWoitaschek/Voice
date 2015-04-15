package de.ph1b.audiobook.fragment;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import de.ph1b.audiobook.activity.SettingsActivity;
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


public class BookPlayFragment extends Fragment implements View.OnClickListener, BaseApplication.OnPlayStateChangedListener, BaseApplication.OnPositionChangedListener, BaseApplication.OnSleepStateChangedListener {


    public static final String TAG = BookPlayFragment.class.getSimpleName();
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_book_play, container, false);

        book = baseApplication.getCurrentBook();
        if (book == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.content, new BookPlayFragment(), BookPlayFragment.TAG)
                    .commit();
            return null;
        }

        //setup actionbar
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        ActionBarActivity actionBarActivity = (ActionBarActivity) getActivity();
        actionBarActivity.setSupportActionBar(toolbar);
        ActionBar actionBar = actionBarActivity.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(book.getName());

        setHasOptionsMenu(true);

        //init buttons
        seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        playButton = (FloatingActionButton) view.findViewById(R.id.play);
        ImageButton rewind_button = (ImageButton) view.findViewById(R.id.rewind);
        ImageButton fast_forward_button = (ImageButton) view.findViewById(R.id.fastForward);
        ImageButton previous_button = (ImageButton) view.findViewById(R.id.previous);
        ImageButton next_button = (ImageButton) view.findViewById(R.id.next);
        playedTimeView = (TextView) view.findViewById(R.id.played);
        ImageView coverView = (ImageView) view.findViewById(R.id.book_cover);
        maxTimeView = (TextView) view.findViewById(R.id.maxTime);
        bookSpinner = (Spinner) view.findViewById(R.id.book_spinner);

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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.activity_book_play_spinner, chaptersAsStrings);
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
        Drawable coverReplacement = new CoverReplacement(baseApplication.getCurrentBook().getName(), getActivity());
        if (!book.isUseCoverReplacement() && coverFile.exists() && coverFile.canRead()) {
            Picasso.with(getActivity()).load(coverFile).placeholder(coverReplacement).into(coverView);
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


        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new PrefsManager(getActivity());
        controller = new ServiceController(getActivity());
        baseApplication = (BaseApplication) getActivity().getApplication();
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
        getActivity().runOnUiThread(new Runnable() {
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
        getActivity().runOnUiThread(new Runnable() {
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
        dialog.show(getFragmentManager(), JumpToPositionDialog.TAG);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.book_play, menu);
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem timeLapseItem = menu.findItem(R.id.action_time_lapse);
        timeLapseItem.setVisible(MediaPlayerController.playerCanSetSpeed);
        MenuItem sleepTimerItem = menu.findItem(R.id.action_sleep);
        if (baseApplication.isSleepTimerActive()) {
            sleepTimerItem.setIcon(R.drawable.ic_alarm_on_white_24dp);
        } else {
            sleepTimerItem.setIcon(R.drawable.ic_snooze_white_24dp);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            case R.id.action_time_change:
                launchJumpToPositionDialog();
                return true;
            case R.id.action_sleep:
                controller.toggleSleepSand();
                return true;
            case R.id.action_time_lapse:
                SetPlaybackSpeedDialog dialog = new SetPlaybackSpeedDialog();
                dialog.show(getFragmentManager(), SetPlaybackSpeedDialog.TAG);
                return true;
            case R.id.action_bookmark:
                BookmarkDialog bookmarkDialog = new BookmarkDialog();
                bookmarkDialog.show(getFragmentManager(), BookmarkDialog.TAG);
                return true;
            case android.R.id.home:
            case R.id.home:
                getActivity().onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        baseApplication.addOnPlayStateChangedListener(this);
        baseApplication.addOnPositionChangedListener(this);
        baseApplication.addOnSleepStateChangedListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        baseApplication.removeOnPlayStateChangedListener(this);
        baseApplication.removeOnPositionChangedListener(this);
        baseApplication.removeOnSleepStateChangedListener(this);
    }

    @Override
    public void onSleepStateChanged(final boolean active) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().invalidateOptionsMenu();
                if (active) {
                    int minutes = prefs.getSleepTime();
                    String message = getString(R.string.sleep_timer_started) + minutes + " " + getString(R.string.minutes);
                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), R.string.sleep_timer_stopped, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
