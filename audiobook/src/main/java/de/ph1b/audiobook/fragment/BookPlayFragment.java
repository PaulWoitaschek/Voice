package de.ph1b.audiobook.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
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

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.SettingsActivity;
import de.ph1b.audiobook.dialog.BookmarkDialogFragment;
import de.ph1b.audiobook.dialog.JumpToPositionDialogFragment;
import de.ph1b.audiobook.dialog.PlaybackSpeedDialogFragment;
import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Chapter;
import de.ph1b.audiobook.model.DataBaseHelper;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.uitools.CoverReplacement;
import de.ph1b.audiobook.uitools.PlayPauseDrawable;
import de.ph1b.audiobook.uitools.ThemeUtil;
import de.ph1b.audiobook.utils.Communication;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.PrefsManager;


public class BookPlayFragment extends Fragment implements View.OnClickListener {


    public static final String TAG = BookPlayFragment.class.getSimpleName();
    private final PlayPauseDrawable playPauseDrawable = new PlayPauseDrawable();
    private final BroadcastReceiver onPlayStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setPlayState(true);
        }
    };
    private final BroadcastReceiver onSleepStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getActivity().invalidateOptionsMenu();
            if (MediaPlayerController.sleepTimerActive) {
                int minutes = prefs.getSleepTime();
                String message = getString(R.string.sleep_timer_started) + minutes + " " +
                        getString(R.string.minutes);
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), R.string.sleep_timer_stopped, Toast.LENGTH_LONG)
                        .show();
            }
        }
    };
    private TextView playedTimeView;
    private SeekBar seekBar;
    private volatile Spinner bookSpinner;
    private TextView maxTimeView;
    private PrefsManager prefs;
    private ServiceController controller;
    private Book book;
    private final BroadcastReceiver onBookSetChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            /**
             * Setting position as a tag, so we can make sure onItemSelected is only fired when
             * the user changes the position himself.
             */
            book = db.getBook(book.getId());
            if (book == null) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.content, new BookPlayFragment(), BookPlayFragment.TAG)
                        .commit();
                return;
            }
            L.d(TAG, "onBookSetChanged called with bookName=" + book.getName());

            ArrayList<Chapter> chapters = book.getChapters();
            Chapter chapter = book.getCurrentChapter();

            int position = chapters.indexOf(chapter);
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
    };
    private DataBaseHelper db;
    private LocalBroadcastManager bcm;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_play, container, false);

        book = db.getBook(prefs.getCurrentBookId());
        if (book == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.content, new BookPlayFragment(), BookPlayFragment.TAG)
                    .commit();
            return null;
        }

        //setup actionbar
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        AppCompatActivity appCompatActivity = (AppCompatActivity) getActivity();
        appCompatActivity.setSupportActionBar(toolbar);
        ActionBar actionBar = appCompatActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(book.getName());
        }

        setHasOptionsMenu(true);

        //init buttons
        seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        FloatingActionButton playButton = (FloatingActionButton) view.findViewById(R.id.play);
        ImageButton previous_button = (ImageButton) view.findViewById(R.id.previous);
        ImageButton next_button = (ImageButton) view.findViewById(R.id.next);
        playedTimeView = (TextView) view.findViewById(R.id.played);
        ImageView coverView = (ImageView) view.findViewById(R.id.book_cover);
        maxTimeView = (TextView) view.findViewById(R.id.maxTime);
        bookSpinner = (Spinner) view.findViewById(R.id.book_spinner);

        //setup buttons
        view.findViewById(R.id.fastForward).setOnClickListener(this);
        view.findViewById(R.id.rewind).setOnClickListener(this);
        previous_button.setOnClickListener(this);
        next_button.setOnClickListener(this);
        playButton.setOnClickListener(this);
        playButton.setIconDrawable(playPauseDrawable);
        playedTimeView.setOnClickListener(this);
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
                controller.changeTime(progress, book.getCurrentChapter()
                        .getPath());
                playedTimeView.setText(formatTime(progress, seekBar.getMax()));
            }
        });

        // adapter
        final ArrayList<String> chaptersAsStrings = new ArrayList<>();
        for (Chapter c : book.getChapters()) {
            chaptersAsStrings.add(c.getName());
        }
        // this is necessary due to a bug in android causing the layout to be ignored.
        // see: http://stackoverflow.com/questions/14139106/spinner-does-not-wrap-text-is-this-an-android-bug
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                R.layout.fragment_book_play_spinner, chaptersAsStrings) {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                final TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
                textView.post(new Runnable() {
                    @Override
                    public void run() {
                        textView.setSingleLine(false);
                    }
                });
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
        Drawable coverReplacement = new CoverReplacement(book.getName(),
                getActivity());
        if (!book.isUseCoverReplacement() && coverFile.exists() && coverFile.canRead()) {
            Picasso.with(getActivity()).load(coverFile).placeholder(coverReplacement).into(
                    coverView);
        } else {
            coverView.setImageDrawable(coverReplacement);
        }
        coverView.setOnClickListener(this);

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

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new PrefsManager(getActivity());
        db = DataBaseHelper.getInstance(getActivity());
        controller = new ServiceController(getActivity());
        bcm = LocalBroadcastManager.getInstance(getActivity());
    }

    private String formatTime(int ms, int duration) {
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
        JumpToPositionDialogFragment dialog = new JumpToPositionDialogFragment();
        Bundle bundle = new Bundle();
        dialog.setArguments(bundle);
        dialog.show(getFragmentManager(), JumpToPositionDialogFragment.TAG);
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
        if (MediaPlayerController.sleepTimerActive) {
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
                if (prefs.setBookmarkOnSleepTimer() && !MediaPlayerController.sleepTimerActive) {
                    String date = DateUtils.formatDateTime(getActivity(), System.currentTimeMillis(), DateUtils.FORMAT_SHOW_DATE |
                            DateUtils.FORMAT_SHOW_TIME |
                            DateUtils.FORMAT_NUMERIC_DATE);
                    BookmarkDialogFragment.addBookmark(book.getId(), date + ": " +
                            getString(R.string.action_sleep), getActivity());
                }
                return true;
            case R.id.action_time_lapse:
                PlaybackSpeedDialogFragment dialog = new PlaybackSpeedDialogFragment();
                dialog.show(getFragmentManager(), PlaybackSpeedDialogFragment.TAG);
                return true;
            case R.id.action_bookmark:
                BookmarkDialogFragment bookmarkDialogFragment = new BookmarkDialogFragment();
                Bundle args = new Bundle();
                args.putLong(BookmarkDialogFragment.BOOK_ID, book.getId());
                bookmarkDialogFragment.setArguments(args);
                bookmarkDialogFragment.show(getFragmentManager(), BookmarkDialogFragment.TAG);
                return true;
            case android.R.id.home:
            case R.id.home:
                getActivity().onBackPressed();
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

        bcm.registerReceiver(onBookSetChanged, new IntentFilter(Communication.BOOK_SET_CHANGED));
        bcm.registerReceiver(onPlayStateChanged, new IntentFilter(Communication.PLAY_STATE_CHANGED));
        bcm.registerReceiver(onSleepStateChanged, new IntentFilter(Communication.SLEEP_STATE_CHANGED));

        onBookSetChanged.onReceive(getActivity(), new Intent());
    }

    @Override
    public void onStop() {
        super.onStop();

        bcm.unregisterReceiver(onBookSetChanged);
        bcm.unregisterReceiver(onPlayStateChanged);
        bcm.unregisterReceiver(onSleepStateChanged);
    }
}
