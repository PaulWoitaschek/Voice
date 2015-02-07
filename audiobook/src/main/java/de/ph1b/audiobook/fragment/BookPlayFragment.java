package de.ph1b.audiobook.fragment;


import android.app.Fragment;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.concurrent.TimeUnit;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.Settings;
import de.ph1b.audiobook.adapter.MediaSpinnerAdapter;
import de.ph1b.audiobook.content.Book;
import de.ph1b.audiobook.content.DataBaseHelper;
import de.ph1b.audiobook.content.Media;
import de.ph1b.audiobook.dialog.BookmarkDialog;
import de.ph1b.audiobook.dialog.JumpToPosition;
import de.ph1b.audiobook.dialog.SetPlaybackSpeedDialog;
import de.ph1b.audiobook.service.AudioPlayerService;
import de.ph1b.audiobook.service.PlayerStates;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.service.StateManager;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.MaterialCompatThemer;
import de.ph1b.audiobook.utils.MusicUtil;
import de.ph1b.audiobook.utils.Prefs;

public class BookPlayFragment extends Fragment implements OnClickListener, StateManager.ChangeListener {

    private static final String TAG = "de.ph1b.audiobook.fragment.BookPlayFragment";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private StateManager stateManager;
    private volatile int duration = 0;
    private ImageButton play_button;
    private TextView playedTimeView;
    private SeekBar seekBar;
    private volatile Spinner bookSpinner;
    private TextView maxTimeView;
    private volatile Book book;
    private Prefs prefs;
    private DataBaseHelper db;
    private ServiceController controller;

    @Override
    public void onResume() {
        super.onResume();

        stateManager.addChangeListener(this);

        book = db.getBook(prefs.getCurrentBookId());
        onPositionChanged(book.getPosition());
        onTimeChanged(book.getTime());
        onStateChanged(stateManager.getState());
    }

    @Override
    public void onPause() {
        stateManager.removeChangeListener(this);

        super.onPause();
    }

    private String formatTime(int ms) {
        String h = String.valueOf(TimeUnit.MILLISECONDS.toHours(ms));
        String m = String.format("%02d", (TimeUnit.MILLISECONDS.toMinutes(ms) % 60));
        String s = String.format("%02d", (TimeUnit.MILLISECONDS.toSeconds(ms) % 60));
        return h + ":" + m + ":" + s;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new Prefs(getActivity());
        db = DataBaseHelper.getInstance(getActivity());
        controller = new ServiceController(getActivity());
        stateManager = StateManager.getInstance(getActivity());

        //setup actionbar
        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_book_play, container, false);

        long bookId = prefs.getCurrentBookId();
        book = db.getBook(bookId);

        // filling missing durations in the background
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (Media m : book.getContainingMedia()) {
                    if (m.getDuration() == 0) {
                        MusicUtil.fillMissingDuration(m);
                        db.updateMedia(m);
                    }
                }
            }
        }).start();

        //init buttons
        seekBar = (SeekBar) v.findViewById(R.id.seekBar);
        play_button = (ImageButton) v.findViewById(R.id.play);
        ImageButton rewind_button = (ImageButton) v.findViewById(R.id.rewind);
        ImageButton fast_forward_button = (ImageButton) v.findViewById(R.id.fast_forward);
        ImageButton previous_button = (ImageButton) v.findViewById(R.id.previous);
        ImageButton next_button = (ImageButton) v.findViewById(R.id.next);
        playedTimeView = (TextView) v.findViewById(R.id.played);
        ImageView coverView = (ImageView) v.findViewById(R.id.book_cover);
        maxTimeView = (TextView) v.findViewById(R.id.maxTime);
        bookSpinner = (Spinner) v.findViewById(R.id.book_spinner);

        //setup buttons
        rewind_button.setOnClickListener(this);
        fast_forward_button.setOnClickListener(this);
        previous_button.setOnClickListener(this);
        next_button.setOnClickListener(this);
        play_button.setOnClickListener(this);
        playedTimeView.setOnClickListener(this);

        play_button.setColorFilter(getResources().getColor(R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
        MaterialCompatThemer.theme(seekBar);

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
                controller.changeTime(progress);
                playedTimeView.setText(formatTime(progress));
            }
        });

        MediaSpinnerAdapter adapter = new MediaSpinnerAdapter(getActivity(), book);
        bookSpinner.setAdapter(adapter);
        bookSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int newPosition, long id) {
                if (((int) parent.getTag()) != newPosition) {
                    L.i(TAG, "spinner, onItemSelected, firing:" + newPosition);
                    controller.changeBookPosition(newPosition, 0);
                    parent.setTag(newPosition);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // (ActionBarTitle)
        String bookName = book.getName();
        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(bookName);

        // (Cover)
        Picasso.with(getActivity()).load(new File(book.getCover())).into(coverView);

        // Next/Prev/spinner hiding
        if (book.getContainingMedia().size() == 1) {
            next_button.setVisibility(View.GONE);
            previous_button.setVisibility(View.GONE);
            bookSpinner.setVisibility(View.GONE);
        } else {
            next_button.setVisibility(View.VISIBLE);
            previous_button.setVisibility(View.VISIBLE);
            bookSpinner.setVisibility(View.VISIBLE);
        }

        getActivity().startService(new Intent(getActivity(), AudioPlayerService.class));

        return v;
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem timeLapseItem = menu.findItem(R.id.action_time_lapse);
        timeLapseItem.setVisible(false);
        timeLapseItem.setVisible(Build.VERSION.SDK_INT >= 16);
        MenuItem sleepTimerItem = menu.findItem(R.id.action_sleep);

        if (stateManager.isSleepTimerActive()) {
            sleepTimerItem.setIcon(R.drawable.ic_alarm_on_white_24dp);
        } else {
            sleepTimerItem.setIcon(R.drawable.ic_snooze_white_24dp);
        }

        super.onPrepareOptionsMenu(menu);
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
            case R.id.fast_forward:
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.action_media_play, menu);
    }


    private void launchJumpToPositionDialog() {
        JumpToPosition dialog = new JumpToPosition();
        Bundle bundle = new Bundle();
        bundle.putInt(JumpToPosition.DURATION, duration);
        bundle.putInt(JumpToPosition.POSITION, stateManager.getTime());
        dialog.setArguments(bundle);
        dialog.setTargetFragment(this, 42);
        dialog.show(getFragmentManager(), "timePicker");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(getActivity(), Settings.class));
                return true;
            case R.id.action_time_change:
                launchJumpToPositionDialog();
                return true;
            case R.id.action_sleep:
                controller.toggleSleepSand();
                return true;
            case R.id.action_time_lapse:
                SetPlaybackSpeedDialog dialog = new SetPlaybackSpeedDialog();
                dialog.setTargetFragment(this, 42);
                dialog.show(getFragmentManager(), TAG);
                return true;
            case R.id.action_bookmark:
                BookmarkDialog bookmarkDialog = new BookmarkDialog();
                bookmarkDialog.show(getFragmentManager(), TAG);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStateChanged(final PlayerStates state) {
        L.d(TAG, "onStateChanged" + state);
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (state != PlayerStates.PLAYING) {
                    play_button.setImageResource(R.drawable.ic_ic_play_circle_fill_72dp);
                } else {
                    play_button.setImageResource(R.drawable.ic_ic_pause_circle_fill_72dp);
                }
            }
        });
    }

    @Override
    public void onSleepTimerSet(final boolean sleepTimerActive) {

        handler.post(new Runnable() {
            @Override
            public void run() {
                getActivity().invalidateOptionsMenu();
                if (sleepTimerActive) {
                    int minutes = prefs.getSleepTime();
                    String message = getString(R.string.sleep_timer_started) + minutes + " " + getString(R.string.minutes);
                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onPositionChanged(final int position) {
        L.v(TAG, "onPositionChanged called: " + position);
        handler.post(new Runnable() {
            @Override
            public void run() {
                /**
                 * Setting position as a tag, so we can make sure onItemSelected is only fired when
                 * the user changes the position himself.
                 */
                L.v(TAG, "onPositionChanged executed:" + position);
                bookSpinner.setTag(position);
                bookSpinner.setSelection(position, true);

                duration = book.getContainingMedia().get(position).getDuration();
                L.d(TAG, "onPositionChanged, duration=" + duration);
                seekBar.setMax(duration);
                maxTimeView.setText(formatTime(duration));
            }
        });
    }

    @Override
    public void onTimeChanged(final int time) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                // Setting seekBar and played time view
                if (!seekBar.isPressed()) {
                    seekBar.setProgress(time);
                    playedTimeView.setText(formatTime(time));
                }
            }
        });
    }
}
