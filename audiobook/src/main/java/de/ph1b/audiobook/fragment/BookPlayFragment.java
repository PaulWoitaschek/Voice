package de.ph1b.audiobook.fragment;


import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
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
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BookChoose;
import de.ph1b.audiobook.activity.Settings;
import de.ph1b.audiobook.adapter.MediaSpinnerAdapter;
import de.ph1b.audiobook.content.BookDetail;
import de.ph1b.audiobook.content.DataBaseHelper;
import de.ph1b.audiobook.content.MediaDetail;
import de.ph1b.audiobook.dialog.BookmarkDialog;
import de.ph1b.audiobook.dialog.JumpToPosition;
import de.ph1b.audiobook.dialog.SetPlaybackSpeedDialog;
import de.ph1b.audiobook.interfaces.OnStateChangedListener;
import de.ph1b.audiobook.interfaces.OnTimeChangedListener;
import de.ph1b.audiobook.service.AudioPlayerService;
import de.ph1b.audiobook.service.PlayerStates;
import de.ph1b.audiobook.service.StateManager;
import de.ph1b.audiobook.utils.ImageHelper;
import de.ph1b.audiobook.utils.MaterialCompatThemer;
import de.ph1b.audiobook.utils.Prefs;

public class BookPlayFragment extends Fragment implements OnClickListener, AudioPlayerService.OnSleepStateChangedListener, StateManager.OnMediaChangedListener, OnStateChangedListener, OnTimeChangedListener {

    private static final String TAG = "de.ph1b.audiobook.fragment.BookPlayFragment";

    public AudioPlayerService service;
    public boolean mBound = false;
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder iBinder) {
            AudioPlayerService.LocalBinder binder = (AudioPlayerService.LocalBinder) iBinder;
            BookPlayFragment.this.service = binder.getService();

            service.stateManager.setOnMediaChangedListener(BookPlayFragment.this);
            service.stateManager.addTimeChangedListener(BookPlayFragment.this);
            service.stateManager.setOnSleepStateChangedListener(BookPlayFragment.this);
            service.stateManager.addStateChangeListener(BookPlayFragment.this);

            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    private int duration = 0;
    private ImageButton play_button;
    private TextView playedTimeView;
    private SeekBar seekBar;
    private Spinner bookSpinner;
    private TextView maxTimeView;
    private int position;
    private int oldPosition = -1;
    private boolean seekBarIsUpdating = false;
    private ArrayList<MediaDetail> allMedia;
    private Prefs prefs;
    private DataBaseHelper db;

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume start");

        Intent serviceIntent = new Intent(getActivity(), AudioPlayerService.class);
        getActivity().startService(serviceIntent);
        Log.d(TAG, "onResume end");
    }

    private String formatTime(int ms) {
        String h = String.valueOf(TimeUnit.MILLISECONDS.toHours(ms));
        String m = String.format("%02d", (TimeUnit.MILLISECONDS.toMinutes(ms) % 60));
        String s = String.format("%02d", (TimeUnit.MILLISECONDS.toSeconds(ms) % 60));
        return h + ":" + m + ":" + s;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onstart start");
        Intent intent = new Intent(getActivity(), AudioPlayerService.class);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "onStartEnd");
    }

    @Override
    public void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            service.stateManager.removeStateChangeListener(this);
            service.stateManager.removeTimeChangedListener(this);
            service.stateManager.setOnSleepStateChangedListener(null);
            service.stateManager.setOnMediaChangedListener(null);
            getActivity().unbindService(mConnection);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new Prefs(getActivity());
        db = DataBaseHelper.getInstance(getActivity());

        //setup actionbar
        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_book_play, container, false);


        long bookId = prefs.getCurrentBookId();
        BookDetail book = db.getBook(bookId);
        allMedia = db.getMediaFromBook(bookId);

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
                position = progress;
                playedTimeView.setText(formatTime(progress)); //sets text to adjust while using seekBar
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekBarIsUpdating = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mBound) {
                    service.changePosition(position);
                }

                playedTimeView.setText(formatTime(position));
                seekBarIsUpdating = false;
            }
        });

        MediaSpinnerAdapter adapter = new MediaSpinnerAdapter(getActivity(), allMedia);
        bookSpinner.setAdapter(adapter);
        bookSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != oldPosition) {
                    if (mBound) {
                        service.changeBookPosition(allMedia.get(position).getId());
                    }
                    oldPosition = position;
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
        String cover = book.getCover();
        if (cover != null && new File(cover).isFile()) {
            Picasso.with(getActivity()).load(new File(cover)).into(coverView);
        } else {
            Log.d(TAG, "cover is null or no file, setting replacment");
            Bitmap coverReplacement = ImageHelper.genCapital(bookName, getActivity(), ImageHelper.TYPE_COVER);
            coverView.setImageBitmap(coverReplacement);
        }

        // Next/Prev/spinner hiding
        if (allMedia.size() == 1) {
            next_button.setVisibility(View.GONE);
            previous_button.setVisibility(View.GONE);
            bookSpinner.setVisibility(View.GONE);
        } else {
            next_button.setVisibility(View.VISIBLE);
            previous_button.setVisibility(View.VISIBLE);
            bookSpinner.setVisibility(View.VISIBLE);
        }

        //invoke initially init chosen media
        for (MediaDetail m : allMedia) {
            if (m.getId() == book.getCurrentMediaId()) {
                if (!new File(m.getPath()).exists()) {
                    fileNotFound();
                }
                onMediaChanged(m);
                break;
            }
        }
        onTimeChanged(book.getCurrentMediaPosition());

        return v;
    }

    private void fileNotFound() {
        String text = getString(R.string.file_not_found);
        Toast toast = Toast.makeText(getActivity(), text, Toast.LENGTH_LONG);
        toast.show();
        Intent i = new Intent(getActivity(), BookChoose.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem timeLapseItem = menu.findItem(R.id.action_time_lapse);
        timeLapseItem.setVisible(false);
        if (mBound) {
            timeLapseItem.setVisible(Build.VERSION.SDK_INT >= 16);
            MenuItem sleepTimerItem = menu.findItem(R.id.action_sleep);

            if (service.sleepSandActive()) {
                sleepTimerItem.setIcon(R.drawable.ic_alarm_on_white_24dp);
            } else {
                sleepTimerItem.setIcon(R.drawable.ic_snooze_white_24dp);
            }
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onClick(View view) {
        if (mBound) {
            switch (view.getId()) {
                case R.id.play:
                    if (service.stateManager.getState() == PlayerStates.STARTED) {
                        service.pause(true);
                    } else
                        service.play();
                    break;
                case R.id.rewind:
                    service.rewind();
                    break;
                case R.id.fast_forward:
                    service.fastForward();
                    break;
                case R.id.next:
                    service.next();
                    break;
                case R.id.previous:
                    service.previous();
                    break;
                case R.id.played:
                    launchJumpToPositionDialog();
                    break;
                default:
                    break;
            }
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
        bundle.putInt(JumpToPosition.POSITION, position);
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
                if (mBound) {
                    service.toggleSleepSand();
                }
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
    public void onSleepStateChanged() {
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onMediaChanged(MediaDetail media) {
        bookSpinner.setSelection(allMedia.indexOf(media));

        duration = media.getDuration();
        seekBar.setMax(duration);
        maxTimeView.setText(formatTime(duration));
    }

    @Override
    public void onStateChanged(PlayerStates state) {
        if (service.stateManager.getState() == PlayerStates.STARTED) {
            play_button.setImageResource(R.drawable.ic_ic_pause_circle_fill_72dp);
        } else {
            play_button.setImageResource(R.drawable.ic_ic_play_circle_fill_72dp);
        }
    }

    @Override
    public void onTimeChanged(int time) {
        // Setting seekbar and played time view
        if (!seekBarIsUpdating) {
            seekBar.setProgress(time);
            playedTimeView.setText(formatTime(time));
        }
    }
}
