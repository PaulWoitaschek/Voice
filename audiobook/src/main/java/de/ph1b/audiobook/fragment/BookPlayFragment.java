package de.ph1b.audiobook.fragment;


import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
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

public class BookPlayFragment extends Fragment implements OnClickListener {

    private static final String TAG = "de.ph1b.audiobook.fragment.BookPlayFragment";
    private final OnStateChangedListener onStateChangedListener = new OnStateChangedListener() {
        @Override
        public void onStateChanged(final PlayerStates state) {
            Activity a = getActivity();
            if (a != null) {
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setPlayPauseButtons(state);
                    }
                });
            }
        }
    };
    public AudioPlayerService mService;
    public boolean mBound = false;
    private ImageButton play_button;
    private TextView playedTimeView;
    private SeekBar seekBar;
    private Spinner bookSpinner;
    private TextView maxTimeView;
    private ImageButton previous_button;
    private ImageButton next_button;
    private int position;
    private LocalBroadcastManager bcm;
    private int oldPosition = -1;
    private boolean seekBarIsUpdating = false;
    private final OnTimeChangedListener onTimeChangedListener = new OnTimeChangedListener() {
        @Override
        public void onTimeChanged(final int time) {
            if (!seekBarIsUpdating) {
                Activity a = getActivity();
                if (a != null) {
                    a.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            playedTimeView.setText(formatTime(time));
                            seekBar.setProgress(time);
                        }
                    });
                }
            }
        }
    };
    private BookDetail book;
    private ArrayList<MediaDetail> allMedia;
    private int duration;
    private final BroadcastReceiver updateGUIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(AudioPlayerService.GUI)) {
                if (mBound) {
                    //setting up time related stuff
                    seekBar.setProgress(stateManager.getTime());
                    playedTimeView.setText(formatTime(stateManager.getTime()));
                }

                MediaDetail media = intent.getParcelableExtra(AudioPlayerService.GUI_MEDIA);
                //checks if file exists
                File testFile = new File(media.getPath());
                if (!testFile.exists()) {
                    noMediaFound();
                }

                //hides control elements if there is only one media to start
                if (allMedia.size() == 1) {
                    bookSpinner.setVisibility(View.GONE);
                    previous_button.setVisibility(View.GONE);
                    next_button.setVisibility(View.GONE);
                } else {
                    bookSpinner.setVisibility(View.VISIBLE);
                    previous_button.setVisibility(View.VISIBLE);
                    next_button.setVisibility(View.VISIBLE);
                    for (int i = 0; i < allMedia.size(); i++) {
                        if (allMedia.get(i).getId() == media.getId()) {
                            bookSpinner.setSelection(i);
                        }
                    }
                }

                //sets duration of file
                duration = media.getDuration();
                maxTimeView.setText(formatTime(duration));

                //sets seekBar to current position and correct length
                seekBar.setMax(duration);
            }
        }
    };
    private boolean sleepTimerActive = false;
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            AudioPlayerService.LocalBinder binder = (AudioPlayerService.LocalBinder) service;
            mService = binder.getService();

            setPlayPauseButtons(stateManager.getState());
            stateManager.addStateChangeListener(onStateChangedListener);
            stateManager.addTimeChangedListener(onTimeChangedListener);
            sleepTimerActive = mService.sleepSandActive();
            Activity a = getActivity();
            if (a != null) {
                getActivity().invalidateOptionsMenu();
            }
            mService.setOnSleepStateChangedListener(new AudioPlayerService.OnSleepStateChangedListener() {
                @Override
                public void onSleepStateChanged(boolean active) {
                    sleepTimerActive = active;
                    Activity a = getActivity();
                    if (a != null) {
                        a.invalidateOptionsMenu();
                    }
                }
            });

            //invalidateOptionsMenu to have the variable speed button shown or not.
            getActivity().invalidateOptionsMenu();

            mService.updateGUI();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private StateManager stateManager;

    private String formatTime(int ms) {
        String h = String.valueOf(TimeUnit.MILLISECONDS.toHours(ms));
        String m = String.format("%02d", (TimeUnit.MILLISECONDS.toMinutes(ms) % 60));
        String s = String.format("%02d", (TimeUnit.MILLISECONDS.toSeconds(ms) % 60));
        return h + ":" + m + ":" + s;
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getActivity(), AudioPlayerService.class);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            stateManager.removeStateChangeListener(onStateChangedListener);
            stateManager.removeTimeChangedListener(onTimeChangedListener);
            mService.setOnSleepStateChangedListener(null);
            getActivity().unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setup actionbar
        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        setHasOptionsMenu(true);

        bcm = LocalBroadcastManager.getInstance(getActivity());
        DataBaseHelper db = DataBaseHelper.getInstance(getActivity());

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);

        Intent i = getActivity().getIntent();

        stateManager = StateManager.getInstance(getActivity());

        long bookId = i.getLongExtra(AudioPlayerService.GUI_BOOK_ID, -1);
        book = db.getBook(bookId);
        if (book == null) {
            noMediaFound();
        } else {
            allMedia = db.getMediaFromBook(book.getId());
            //starting the service
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Intent serviceIntent = new Intent(getActivity(), AudioPlayerService.class);
                    serviceIntent.putExtra(AudioPlayerService.GUI_BOOK_ID, book.getId());
                    getActivity().startService(serviceIntent);
                }
            }).start();
        }
    }

    private void setPlayPauseButtons(PlayerStates state) {
        if (state == PlayerStates.STARTED) {
            play_button.setImageResource(R.drawable.ic_ic_pause_circle_fill_72dp);
        } else {
            play_button.setImageResource(R.drawable.ic_ic_play_circle_fill_72dp);
        }
    }

    private void noMediaFound() {
        String text = getString(R.string.file_not_found);
        Toast toast = Toast.makeText(getActivity(), text, Toast.LENGTH_LONG);
        toast.show();

        Intent i = new Intent(getActivity(), BookChoose.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_book_play, container, false);
        initGUI(v);
        return v;
    }

    private void initGUI(View v) {
        //init buttons
        seekBar = (SeekBar) v.findViewById(R.id.seekBar);
        play_button = (ImageButton) v.findViewById(R.id.play);
        ImageButton rewind_button = (ImageButton) v.findViewById(R.id.rewind);
        ImageButton fast_forward_button = (ImageButton) v.findViewById(R.id.fast_forward);
        previous_button = (ImageButton) v.findViewById(R.id.previous);
        next_button = (ImageButton) v.findViewById(R.id.next);
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

        int mediaDuration = 0;
        for (MediaDetail m : allMedia) {
            if (m.getId() == book.getCurrentMediaId()) {
                mediaDuration = m.getDuration();
                break;
            }
        }

        int mediaPosition = book.getCurrentMediaPosition();
        seekBar.setProgress(mediaPosition);
        seekBar.setMax(mediaDuration);
        maxTimeView.setText(formatTime(mediaDuration));
        playedTimeView.setText(formatTime(mediaPosition));

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
                if (mBound)
                    mService.changePosition(position);

                playedTimeView.setText(formatTime(position));
                seekBarIsUpdating = false;
            }
        });

        //cover
        String imagePath = book.getCover();
        if (imagePath == null || imagePath.equals("") || !new File(imagePath).exists() || new File(imagePath).isDirectory()) {
            Bitmap cover = ImageHelper.genCapital(book.getName(), getActivity(), ImageHelper.TYPE_COVER);
            coverView.setImageBitmap(cover);
        } else
            coverView.setImageURI(Uri.parse(imagePath));


        //setting book name
        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        String bookName = book.getName();
        actionBar.setTitle(bookName);

        if (allMedia.size() > 1) {

            bookSpinner.setVisibility(View.VISIBLE);
            previous_button.setVisibility(View.VISIBLE);
            next_button.setVisibility(View.VISIBLE);

            MediaSpinnerAdapter adapter = new MediaSpinnerAdapter(getActivity(), allMedia);
            bookSpinner.setAdapter(adapter);
            for (int i = 0; i < allMedia.size(); i++) {
                if (allMedia.get(i).getId() == book.getCurrentMediaId()) {
                    bookSpinner.setSelection(i);
                }
            }
            bookSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position != oldPosition) {
                        if (mBound)
                            mService.changeBookPosition(allMedia.get(position).getId());
                        oldPosition = position;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        } else {
            bookSpinner.setVisibility(View.GONE);
            previous_button.setVisibility(View.GONE);
            next_button.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem timeLapseItem = menu.findItem(R.id.action_time_lapse);
        timeLapseItem.setVisible(false);
        if (mBound) {
            timeLapseItem.setVisible(Build.VERSION.SDK_INT >= 16);

            MenuItem sleepTimerItem = menu.findItem(R.id.action_sleep);

            if (sleepTimerActive) {
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
                    if (stateManager.getState() == PlayerStates.STARTED) {
                        mService.pause(true);
                    } else
                        mService.play();
                    break;
                case R.id.rewind:
                    mService.rewind();
                    break;
                case R.id.fast_forward:
                    mService.fastForward();
                    break;
                case R.id.next:
                    mService.next();
                    break;
                case R.id.previous:
                    mService.previous();
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
                    mService.toggleSleepSand();
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
    public void onDestroy() {
        bcm.unregisterReceiver(updateGUIReceiver);
        super.onDestroy();
    }


    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioPlayerService.GUI);
        bcm.registerReceiver(updateGUIReceiver, filter);
    }


    @Override
    public void onPause() {
        bcm.unregisterReceiver(updateGUIReceiver);
        super.onPause();
    }
}
