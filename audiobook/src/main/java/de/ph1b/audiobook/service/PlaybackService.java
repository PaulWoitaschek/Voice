package de.ph1b.audiobook.service;


import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.utils.BookDetail;
import de.ph1b.audiobook.utils.DataBaseHelper;
import de.ph1b.audiobook.utils.MediaDetail;
import de.ph1b.audiobook.interfaces.OnStateChangedListener;
import de.ph1b.audiobook.receiver.RemoteControlReceiver;
import de.ph1b.audiobook.receiver.WidgetProvider;

public class PlaybackService {


    private static final String TAG = "PlaybackService";

    private final Context context;
    private final AudioManager audioManager;
    private MediaPlayer mediaPlayer = new MediaPlayer();


    public static final String GUI = "de.ph1b.audiobook.GUI";
    public static final String GUI_BOOK = "de.ph1b.audiobook.GUI_BOOK";
    public static final String GUI_ALL_MEDIA = "de.ph1b.audiobook.GUI_ALL_MEDIA";
    public static final String GUI_SEEK = "de.ph1b.audiobook.GUI_SEEK";
    public static final String GUI_PLAY_ICON = "de.ph1b.audiobook.GUI_PLAY_ICON";
    public static final String GUI_MAKE_TOAST = "de.ph1b.audiobook.GUI_MAKE_TOAST";

    private boolean pauseBecauseHeadset = false;

    private int lastState;

    private MediaDetail media;
    private MediaDetail[] allMedia;
    private BookDetail book;
    private final DataBaseHelper db;
    private final Handler handler = new Handler();

    private RemoteControlClient mRemoteControlClient;

    private final ComponentName myEventReceiver;
    private boolean shouldUpdateCover;

    private final LocalBroadcastManager bcm;

    private int audioFocus = 0;

    private final ReentrantLock playerLock = new ReentrantLock();

    //keeps track of bc registered
    private boolean noisyRCRegistered = false;
    private boolean headsetRCRegistered = false;

    private final ComponentName widgetComponentName;

    @SuppressLint("NewApi")
    public PlaybackService(final Context context) {
        final String TAG = PlaybackService.TAG + "PlaybackService()";
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Initializing new PlaybackService");

        this.context = context;
        shouldUpdateCover = true;

        bcm = LocalBroadcastManager.getInstance(context);
        db = DataBaseHelper.getInstance(context);


        // Create an Intent to launch ExampleActivity


        widgetComponentName = new ComponentName(context, WidgetProvider.class);
        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.widget);

        //state manager to update widget
        StateManager.setStateChangeListener(new OnStateChangedListener() {
            @Override
            public void onStateChanged(PlayerStates state) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "state changed called: " + state);
                if (state == PlayerStates.STARTED) {
                    remoteViews.setImageViewResource(R.id.widgetPlayButton, R.drawable.av_pause);
                } else {
                    remoteViews.setImageViewResource(R.id.widgetPlayButton, R.drawable.av_play);
                }
                AppWidgetManager.getInstance(context).updateAppWidget(widgetComponentName, remoteViews);
            }
        });

        myEventReceiver = new ComponentName(context.getPackageName(), RemoteControlReceiver.class.getName());

        //setup remote client
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            ComponentName nmm = new ComponentName(context.getPackageName(), RemoteControlReceiver.class.getName());
            mediaButtonIntent.setComponent(nmm);
            PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(context, 0, mediaButtonIntent, 0);
            mRemoteControlClient = new RemoteControlClient(mediaPendingIntent);
        }
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    private final BroadcastReceiver audioBecomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "audio becoming noisy!");
            if (StateManager.getState() == PlayerStates.STARTED)
                pauseBecauseHeadset = true;
            pause();
        }
    };

    private final BroadcastReceiver headsetPlugReceiver = new BroadcastReceiver() {

        private static final int CONNECTED = 1;

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
                    int headsetState = intent.getIntExtra("state", -1);
                    if (headsetState != -1) {
                        if (headsetState == CONNECTED) {
                            if (pauseBecauseHeadset) {
                                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                                boolean resumeOnReplug = sharedPref.getBoolean(context.getString(R.string.pref_resume_on_replug), true);
                                if (resumeOnReplug) {
                                    play();
                                    if (BuildConfig.DEBUG)
                                        Log.d(TAG, "Resuming player because headset was replugged");
                                }
                                pauseBecauseHeadset = false;
                            }
                        }
                    }
                }
            }
        }
    };


    @SuppressLint("NewApi")
    public void initBook(BookDetail book, MediaDetail[] allMedia) {
        this.book = book;
        this.allMedia = allMedia;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {

            if (book.getMediaIds().length > 1) {
                mRemoteControlClient.setTransportControlFlags(
                        RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                                RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                                RemoteControlClient.FLAG_KEY_MEDIA_NEXT
                );
            } else {
                mRemoteControlClient.setTransportControlFlags(
                        RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
                );
            }
        }
    }

    private MediaDetail getMediaById(int mediaId) {
        for (MediaDetail m : allMedia) {
            if (m.getId() == mediaId) {
                return m;
            }
        }
        return null;
    }


    public void prepare(int mediaId) {
        if (StateManager.getState() == PlayerStates.STARTED) {
            pause();
        }

        playerLock.lock();
        try {
            if (book.getPosition() != mediaId) {
                book.setPosition(mediaId);
                db.updateBookAsync(book);
            }

            media = getMediaById(mediaId);
            String path = media.getPath();
            int position = media.getPosition();


            reset();

            if (BuildConfig.DEBUG)
                Log.d(TAG, "creating new player");
            if (StateManager.getState() == PlayerStates.END)
                mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mediaPlayer.setDataSource(context, Uri.parse(path));
                mediaPlayer.prepare();
                StateManager.setState(PlayerStates.PREPARED);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //requests wake-mode which is automatically released when pausing
            mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
            mediaPlayer.seekTo(position);
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Next Song by onCompletion");
                    media.setPosition(0);
                    db.updateMediaAsync(media);
                    nextSong();
                }
            });
        } finally {
            playerLock.unlock();
        }
    }


    private void reset() {
        playerLock.lock();
        try {
            PlayerStates state = StateManager.getState();
            if (state == PlayerStates.STARTED)
                pause();
            if (state == PlayerStates.IDLE || state == PlayerStates.PREPARED || state == PlayerStates.STARTED || state == PlayerStates.PAUSED) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "resetting player");
                mediaPlayer.reset();
                StateManager.setState(PlayerStates.IDLE);
            }
        } finally {
            playerLock.unlock();
        }
    }

    public void updateGUI() {
        Intent i = new Intent(GUI);
        i.putExtra(GUI_BOOK, book);
        i.putExtra(GUI_ALL_MEDIA, allMedia);

        if (StateManager.getState() == PlayerStates.STARTED) {
            i.putExtra(GUI_PLAY_ICON, R.drawable.av_pause);
        } else {
            i.putExtra(GUI_PLAY_ICON, R.drawable.av_play);
        }
        bcm.sendBroadcast(i);
    }

    public void sleepTimer(int sleepTime) {

        Timer sandman = new Timer();

        TimerTask sleepSand = new TimerTask() {
            @Override
            public void run() {
                playerLock.lock();
                try {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Good night everyone");
                    reset();
                    if (StateManager.getState() == PlayerStates.IDLE) {
                        mediaPlayer.release();
                        StateManager.setState(PlayerStates.END);
                    }
                } finally {
                    playerLock.unlock();
                }
            }
        };
        if (BuildConfig.DEBUG)
            Log.d(TAG, "I should fall asleep after " + sleepTime / 1000 + " s");
        sandman.schedule(sleepSand, sleepTime);
    }

    public void fastForward() {
        playerLock.lock();
        try {
            PlayerStates state = StateManager.getState();
            if (state == PlayerStates.STARTED || state == PlayerStates.PREPARED || state == PlayerStates.PAUSED) {
                int position = mediaPlayer.getCurrentPosition();
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                int changeTimeAmount = sharedPref.getInt(context.getString(R.string.pref_change_amount), 20) * 1000;
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Fast Forward by " + changeTimeAmount);
                int newPosition = position + changeTimeAmount;
                if (newPosition < 0)
                    newPosition = 0;
                mediaPlayer.seekTo(newPosition);
                media.setPosition(newPosition);
                db.updateMediaAsync(media);

                updateGUI();
            }
        } finally {
            playerLock.unlock();
        }
    }

    public void rewind() {
        playerLock.lock();
        try {
            PlayerStates state = StateManager.getState();
            if (state == PlayerStates.STARTED || state == PlayerStates.PREPARED || state == PlayerStates.PAUSED) {
                int position = mediaPlayer.getCurrentPosition();
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                int changeTimeAmount = sharedPref.getInt(context.getString(R.string.pref_change_amount), 20) * 1000;
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Rewind by " + changeTimeAmount);
                int newPosition = position - changeTimeAmount;
                if (newPosition < 0)
                    newPosition = 0;
                mediaPlayer.seekTo(newPosition);
                media.setPosition(newPosition);
                db.updateMediaAsync(media);

                updateGUI();
            }
        } finally {
            playerLock.unlock();
        }
    }

    public void previousSong() {
        PlayerStates state = StateManager.getState();
        if (state == PlayerStates.STARTED || state == PlayerStates.PREPARED || state == PlayerStates.PAUSED) {
            int[] allIds = book.getMediaIds();
            int currentId = media.getId();
            for (int i = 1; i < allIds.length; i++) { //starting at #1 to prevent change when on first song
                if (allIds[i] == currentId) {
                    boolean wasPlaying = false;
                    if (StateManager.getState() == PlayerStates.STARTED) {
                        wasPlaying = true;
                    }
                    prepare(allIds[i - 1]);
                    book.setPosition(media.getId());
                    db.updateBookAsync(book);
                    if (wasPlaying)
                        play();
                    updateGUI();
                    break;
                }
            }
        }
    }

    public void nextSong() {
        PlayerStates state = StateManager.getState();
        if (state == PlayerStates.STARTED || state == PlayerStates.PREPARED || state == PlayerStates.PAUSED) {
            int[] allIds = book.getMediaIds();
            int currentId = media.getId();
            for (int i = 0; i < allIds.length - 1; i++) { //-1 to prevent change when already last song reached
                if (allIds[i] == currentId) {
                    boolean wasPlaying = false;
                    if (StateManager.getState() == PlayerStates.STARTED)
                        wasPlaying = true;
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "preparing now");
                    prepare(allIds[i + 1]);
                    book.setPosition(media.getId());
                    db.updateBookAsync(book);
                    if (wasPlaying)
                        play();
                    updateGUI();
                    break;
                }

            }

            // if at last position, remove handler and notification, audio-focus
            if (currentId == allIds[allIds.length - 1])
                pause();
        }
    }

    public void changeBookPosition(int mediaId) {
        if (mediaId != book.getPosition()) {
            book.setPosition(mediaId);
            db.updateBookAsync(book);
            boolean wasPlaying = false;
            if (StateManager.getState() == PlayerStates.STARTED)
                wasPlaying = true;
            prepare(mediaId);
            if (wasPlaying)
                play();
            updateGUI();
        }
    }


    @SuppressLint("NewApi")
    public void play() {
        String TAG = PlaybackService.TAG + "play()";
        playerLock.lock();
        try {
            switch (StateManager.getState()) {
                case PREPARED:
                case PAUSED:
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "starting MediaPlayer");
                    mediaPlayer.start();
                    StateManager.setState(PlayerStates.STARTED);

                    //start notification
                    Intent i = new Intent(AudioPlayerService.CONTROL_SETUP_NOTIFICATION);
                    i.putExtra(AudioPlayerService.CONTROL_SETUP_NOTIFICATION, true);
                    bcm.sendBroadcast(i);

                    //requesting audio-focus and setting up lock-screen-controls
                    if (audioFocus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        audioFocus = audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Audio-focus granted. Setting up RemoteControlClient");

                        audioManager.registerMediaButtonEventReceiver(myEventReceiver);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                            if (BuildConfig.DEBUG)
                                Log.d(TAG, "Setting up remote Control Client");
                            audioManager.registerRemoteControlClient(mRemoteControlClient);
                            mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                        updateMetaData(shouldUpdateCover);

                    //starting runner to update gui
                    handler.postDelayed(savePositionRunner, 10000);
                    handler.postDelayed(updateSeekBarRunner, 1000);

                    //setting logo
                    updateGUI();

                    //registering receivers for information on media controls
                    if (!noisyRCRegistered) {
                        context.registerReceiver(audioBecomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
                        noisyRCRegistered = true;
                    }
                    if (!headsetRCRegistered) {
                        context.registerReceiver(headsetPlugReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
                        headsetRCRegistered = true;
                    }
                    break;
                case IDLE:
                case END:
                case STOPPED:
                    prepare(media.getId());
                    play();
                    break;
                default:
                    break;
            }
        } finally {
            playerLock.unlock();
        }

    }


    @SuppressLint("NewApi")
    public void pause() {
        playerLock.lock();
        try {
            if (StateManager.getState() == PlayerStates.STARTED) {
                String TAG = PlaybackService.TAG + "pause()";
                //stops runner who were updating gui frequently
                handler.removeCallbacks(savePositionRunner);
                handler.removeCallbacks(updateSeekBarRunner);

                //saves current position, then pauses
                int position = mediaPlayer.getCurrentPosition();
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "setting position to " + position);
                if (position > 0) {
                    media.setPosition(position);
                    db.updateMediaAsync(media);
                }

                mediaPlayer.pause();
                StateManager.setState(PlayerStates.PAUSED);
            }
            Intent i = new Intent(AudioPlayerService.CONTROL_SETUP_NOTIFICATION);
            i.putExtra(AudioPlayerService.CONTROL_SETUP_NOTIFICATION, false);
            bcm.sendBroadcast(i);

            //abandon audio-focus and disabling lock-screen controls
            audioManager.abandonAudioFocus(audioFocusChangeListener);
            audioFocus = 0;
            audioManager.unregisterMediaButtonEventReceiver(myEventReceiver);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                audioManager.unregisterRemoteControlClient(mRemoteControlClient);
                mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
            }

            //setting logo
            updateGUI();

            //setting should update thumb to true so thumb in lock-screen gets updated next time
            shouldUpdateCover = true;

            //releasing control receivers because pause
            if (noisyRCRegistered) {
                context.unregisterReceiver(audioBecomingNoisyReceiver);
                noisyRCRegistered = false;
            }
        } finally {
            playerLock.unlock();
        }
    }

    public void finish() {
        playerLock.lock();
        try {
            handler.removeCallbacks(savePositionRunner);
            handler.removeCallbacks(updateSeekBarRunner);

            audioManager.abandonAudioFocus(audioFocusChangeListener);
            audioFocus = 0;
            audioManager.unregisterMediaButtonEventReceiver(myEventReceiver);

            if (StateManager.getState() != PlayerStates.END)
                mediaPlayer.reset();
            mediaPlayer.release();

            StateManager.setState(PlayerStates.END);
            if (noisyRCRegistered) {
                context.unregisterReceiver(audioBecomingNoisyReceiver);
                noisyRCRegistered = false;
            }
            if (headsetRCRegistered) {
                context.unregisterReceiver(headsetPlugReceiver);
                headsetRCRegistered = false;
            }
        } finally {
            playerLock.unlock();
        }
    }

    public void changePosition(int position) {
        playerLock.lock();
        try {
            PlayerStates state = StateManager.getState();
            if (state == PlayerStates.STARTED || state == PlayerStates.PREPARED || state == PlayerStates.PAUSED) {
                mediaPlayer.seekTo(position);
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "setting position to " + position + " by changePosition");
                media.setPosition(position);
                db.updateMediaAsync(media);
            }
            updateGUI();
        } finally {
            playerLock.unlock();
        }
    }


    private final Runnable savePositionRunner = new Runnable() {
        @Override
        public void run() {
            if (playerLock.tryLock()) {
                try {
                    if (StateManager.getState() == PlayerStates.STARTED) {
                        int position = mediaPlayer.getCurrentPosition();
                        if (position > 0) {
                            media.setPosition(position);
                            db.updateMediaAsync(media);
                        }
                    }
                } finally {
                    playerLock.unlock();
                }
            }
            handler.postDelayed(savePositionRunner, 10000);
        }
    };

    private final Runnable updateSeekBarRunner = new Runnable() {
        @Override
        public void run() {
            playerLock.lock();
            try {
                if (StateManager.getState() == PlayerStates.STARTED) {
                    Intent i = new Intent(GUI_SEEK);
                    i.putExtra(GUI_SEEK, mediaPlayer.getCurrentPosition());
                    bcm.sendBroadcast(i);
                }
            } finally {
                playerLock.unlock();
            }
            handler.postDelayed(updateSeekBarRunner, 1000);
        }
    };


    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Paused by audio-focus loss transient.");
                    pause();
                    lastState = focusChange;
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "started by audioFocus gained");
                    switch (lastState) {
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            play();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
                            break;
                        default:
                            break;
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "paused by audioFocus loss");
                    pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "audio focus loss transient can duck");
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
                    lastState = focusChange;
                    break;
                default:
                    break;
            }
        }
    };

    @SuppressLint("NewApi")
    private void updateMetaData(boolean wholeBook) {
        if (wholeBook) {
            new AsyncTask<Void, Void, Void>() {
                private RemoteControlClient.MetadataEditor editor;

                @Override
                protected Void doInBackground(Void... params) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        editor = mRemoteControlClient.editMetadata(true);
                        editor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, media.getName());
                        editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, book.getName());
                        Bitmap bm = BitmapFactory.decodeFile(book.getCover());
                        editor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, bm);
                        editor.apply();
                    }
                    shouldUpdateCover = false;
                    return null;
                }
            }.execute();
        } else {
            new AsyncTask<Void, Void, Void>() {
                private RemoteControlClient.MetadataEditor editor;

                @Override
                protected Void doInBackground(Void... params) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        editor = mRemoteControlClient.editMetadata(false); //false to leave thumb!
                        editor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, media.getName());
                        editor.apply();
                    }
                    return null;
                }
            }.execute();
        }
    }
}