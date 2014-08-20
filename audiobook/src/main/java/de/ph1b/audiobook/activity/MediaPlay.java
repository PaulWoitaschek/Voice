package de.ph1b.audiobook.activity;

import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.concurrent.TimeUnit;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.adapter.MediaSpinnerAdapter;
import de.ph1b.audiobook.fragment.JumpToPosition;
import de.ph1b.audiobook.fragment.SettingsFragment;
import de.ph1b.audiobook.fragment.SleepDialog;
import de.ph1b.audiobook.helper.BookDetail;
import de.ph1b.audiobook.helper.CommonTasks;
import de.ph1b.audiobook.helper.MediaDetail;
import de.ph1b.audiobook.service.AudioPlayerService;
import de.ph1b.audiobook.service.PlaybackService;
import de.ph1b.audiobook.service.PlayerStates;
import de.ph1b.audiobook.service.StateManager;

public class MediaPlay extends ActionBarActivity implements OnClickListener {


    private ImageButton play_button;
    private TextView playedTimeView;
    private ImageView coverView;
    private ImageButton previous_button;
    private ImageButton forward_button;
    private SeekBar seek_bar;
    private Spinner bookSpinner;
    private TextView maxTimeView;
    private int position;

    private LocalBroadcastManager bcm;

    private static int bookId;
    private int oldPosition = -1;
    private static final String TAG = "MediaPlay";

    private boolean seekBarIsUpdating = false;
    private MediaDetail[] allMedia;
    private MediaDetail media;

    private int duration;


    private final BroadcastReceiver updateGUIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (action.equals(PlaybackService.GUI)) {

                //update book
                final BookDetail b = intent.getParcelableExtra(PlaybackService.GUI_BOOK);
                int mediaId = b.getPosition();
                allMedia = (MediaDetail[]) intent.getParcelableArrayExtra(PlaybackService.GUI_ALL_MEDIA);

                for (MediaDetail m : allMedia) {
                    if (m.getId() == mediaId) {
                        media = m;
                        break;
                    }
                }

                //checks if file exists
                File testFile = new File(media.getPath());
                if (!testFile.exists()) {
                    makeToast(getString(R.string.file_not_found), Toast.LENGTH_LONG);
                    startActivity(new Intent(getApplicationContext(), MediaView.class));
                }

                //setting book name
                String bookName = b.getName();
                getSupportActionBar().setTitle(bookName);

                bookId = b.getId();

                //setting cover
                coverView.getViewTreeObserver().addOnPreDrawListener(
                        new ViewTreeObserver.OnPreDrawListener() {
                            public boolean onPreDraw() {
                                int height = coverView.getMeasuredHeight();
                                int width = coverView.getMeasuredWidth();
                                String imagePath = b.getCover();
                                Bitmap cover;
                                if (imagePath.equals("") || new File(imagePath).isDirectory()) {
                                    cover = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                                    Canvas c = new Canvas(cover);
                                    Paint textPaint = new Paint();
                                    textPaint.setTextSize(4 * width / 5);
                                    Resources r = getApplicationContext().getResources();

                                    textPaint.setColor(r.getColor(android.R.color.white));
                                    textPaint.setAntiAlias(true);
                                    textPaint.setTextAlign(Paint.Align.CENTER);
                                    Paint backgroundPaint = new Paint();
                                    backgroundPaint.setColor(r.getColor(R.color.file_chooser_audio));
                                    c.drawRect(0, 0, width, height, backgroundPaint);
                                    int y = (int) ((c.getHeight() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2));
                                    c.drawText(b.getName().substring(0, 1).toUpperCase(), width / 2, y, textPaint);
                                    coverView.setImageBitmap(cover);
                                } else {
                                    coverView.setImageURI(Uri.parse(imagePath));
                                }
                                return true;
                            }
                        }
                );


                //hides control elements if there is only one media to play
                if (allMedia.length == 1) {
                    previous_button.setVisibility(View.GONE);
                    forward_button.setVisibility(View.GONE);
                    bookSpinner.setVisibility(View.GONE);
                } else {
                    previous_button.setVisibility(View.VISIBLE);
                    forward_button.setVisibility(View.VISIBLE);
                    bookSpinner.setVisibility(View.VISIBLE);
                    MediaSpinnerAdapter adapter = new MediaSpinnerAdapter(getApplicationContext(), allMedia);
                    int currentPosition = b.getPosition();
                    bookSpinner.setSelection(adapter.getPositionByMediaDetailId(currentPosition));
                    bookSpinner.setAdapter(adapter);
                    //sets correct position in spinner
                    if (allMedia.length > 1) {
                        bookSpinner.setSelection(adapter.getPositionByMediaDetailId(mediaId));
                    }
                    bookSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (position != oldPosition) {
                                int newMediaId = allMedia[position].getId();
                                Intent i = new Intent(AudioPlayerService.CONTROL_CHANGE_BOOK_POSITION);
                                i.putExtra(AudioPlayerService.CONTROL_CHANGE_BOOK_POSITION, newMediaId);
                                bcm.sendBroadcast(i);
                                oldPosition = position;
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });
                }


                //updates media
                int position = media.getPosition();
                playedTimeView.setText(formatTime(position));

                //sets duration of file
                duration = media.getDuration();
                maxTimeView.setText(formatTime(duration));


                //sets seekBar to current position and correct length
                seek_bar.setMax(duration);
                seek_bar.setProgress(position);

                //sets play-button logo depending on player playing
                int icon = intent.getExtras().getInt(PlaybackService.GUI_PLAY_ICON);
                play_button.setImageResource(icon);
            }

            //updates seekBar by frequent calls
            if (action.equals(PlaybackService.GUI_SEEK)) {
                if (!seekBarIsUpdating)
                    seek_bar.setProgress(intent.getExtras().getInt(PlaybackService.GUI_SEEK));
            }

            if (action.equals(PlaybackService.GUI_MAKE_TOAST)) {
                String text = intent.getStringExtra(PlaybackService.GUI_MAKE_TOAST);
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Received text for toast: " + text);
                makeToast(text, Toast.LENGTH_SHORT);
            }
        }
    };

    private void makeToast(String text, int duration) {
        if (text != null) {
            Toast toast = Toast.makeText(getApplicationContext(), text, duration);
            toast.show();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);
        new CommonTasks().checkExternalStorage(this);

        bcm = LocalBroadcastManager.getInstance(this);

        //setup actionbar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //starting AudioPlayerService and give him bookId to play
        if (getIntent().hasExtra(MediaView.PLAY_BOOK))
            bookId = getIntent().getIntExtra(MediaView.PLAY_BOOK, 0);


        //init buttons
        seek_bar = (SeekBar) findViewById(R.id.seekBar);
        play_button = (ImageButton) findViewById(R.id.play);
        ImageButton rewind_button = (ImageButton) findViewById(R.id.rewind);
        ImageButton fast_forward_button = (ImageButton) findViewById(R.id.fast_forward);
        playedTimeView = (TextView) findViewById(R.id.played);
        forward_button = (ImageButton) findViewById(R.id.next_song);
        previous_button = (ImageButton) findViewById(R.id.previous_song);
        coverView = (ImageView) findViewById(R.id.book_cover);
        maxTimeView = (TextView) findViewById(R.id.maxTime);
        bookSpinner = (Spinner) findViewById(R.id.book_spinner);


        //setup buttons
        forward_button.setOnClickListener(this);
        previous_button.setOnClickListener(this);
        rewind_button.setOnClickListener(this);
        fast_forward_button.setOnClickListener(this);
        play_button.setOnClickListener(this);

        seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

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
                Intent i = new Intent(AudioPlayerService.CONTROL_CHANGE_MEDIA_POSITION);
                i.putExtra(AudioPlayerService.CONTROL_CHANGE_MEDIA_POSITION, position);
                bcm.sendBroadcast(i);

                playedTimeView.setText(formatTime(position));
                seekBarIsUpdating = false;
            }
        });
    }

    private String formatTime(int ms) {
        String h = String.valueOf(TimeUnit.MILLISECONDS.toHours(ms));
        String m = String.format("%02d", (TimeUnit.MILLISECONDS.toMinutes(ms) % 60));
        String s = String.format("%02d", (TimeUnit.MILLISECONDS.toSeconds(ms) % 60));
        return h + ":" + m + ":" + s;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play:
                bcm.sendBroadcast(new Intent(AudioPlayerService.CONTROL_PLAY_PAUSE));
                break;
            case R.id.rewind:
                bcm.sendBroadcast(new Intent(AudioPlayerService.CONTROL_REWIND));
                break;
            case R.id.fast_forward:
                bcm.sendBroadcast(new Intent(AudioPlayerService.CONTROL_FAST_FORWARD));
                break;
            case R.id.next_song:
                bcm.sendBroadcast(new Intent(AudioPlayerService.CONTROL_FORWARD));
                break;
            case R.id.previous_song:
                bcm.sendBroadcast(new Intent(AudioPlayerService.CONTROL_PREVIOUS));
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_media_play, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                fragmentTransaction.replace(android.R.id.content, new SettingsFragment());
                fragmentTransaction.commit();
                return true;
            case R.id.action_time_change:
                if (duration > 0) {
                    JumpToPosition jumpToPosition = new JumpToPosition();
                    Bundle bundle = new Bundle();
                    bundle.putInt(JumpToPosition.DURATION, duration);
                    bundle.putInt(JumpToPosition.POSITION, position);
                    jumpToPosition.setArguments(bundle);
                    jumpToPosition.show(getSupportFragmentManager(), "timePicker");
                }
                return true;
            case R.id.action_sleep:
                SleepDialog sleepDialog = new SleepDialog();
                sleepDialog.show(getSupportFragmentManager(), "sleep_timer");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MediaView.class));
    }


    @Override
    public void onDestroy() {
        bcm.unregisterReceiver(updateGUIReceiver);

        super.onDestroy();
    }


    @Override
    public void onResume() {
        super.onResume();

        //starting the service
        Intent serviceIntent = new Intent(this, AudioPlayerService.class);
        serviceIntent.putExtra(AudioPlayerService.BOOK_ID, bookId);
        startService(serviceIntent);

        Intent pokeIntent = new Intent(AudioPlayerService.CONTROL_POKE_UPDATE);
        pokeIntent.setAction(AudioPlayerService.CONTROL_POKE_UPDATE);
        bcm.sendBroadcast(pokeIntent);

        if (StateManager.getState() == PlayerStates.STARTED) {
            play_button.setImageResource(R.drawable.av_pause);
        } else {
            play_button.setImageResource(R.drawable.av_play);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(PlaybackService.GUI);
        filter.addAction(PlaybackService.GUI_SEEK);
        filter.addAction(PlaybackService.GUI_PLAY_ICON);
        filter.addAction(PlaybackService.GUI_MAKE_TOAST);
        bcm.registerReceiver(updateGUIReceiver, filter);

        //checking if external storage is available
        new CommonTasks().checkExternalStorage(this);
    }


    @Override
    public void onPause() {
        bcm.unregisterReceiver(updateGUIReceiver);
        super.onPause();
    }

}
