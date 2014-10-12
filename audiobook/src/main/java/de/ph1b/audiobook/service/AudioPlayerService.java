package de.ph1b.audiobook.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BookChoose;
import de.ph1b.audiobook.activity.BookPlay;
import de.ph1b.audiobook.interfaces.OnStateChangedListener;
import de.ph1b.audiobook.receiver.RemoteControlReceiver;
import de.ph1b.audiobook.receiver.WidgetProvider;
import de.ph1b.audiobook.utils.BookDetail;
import de.ph1b.audiobook.utils.CommonTasks;
import de.ph1b.audiobook.utils.DataBaseHelper;
import de.ph1b.audiobook.utils.MediaDetail;

public class AudioPlayerService extends Service {


    public static final String BOOK_ID = "de.ph1b.audiobook.BOOK_ID";
    private static final int NOTIFICATION_ID = 1;

    //private int bookId;

    private DataBaseHelper db;
    private LocalBroadcastManager bcm;

    //indicates if service is running for MediaPlay to check on resume
    //public static boolean serviceRunning = true;

    private final IBinder mBinder = new LocalBinder();

    private static final String TAG = "de.ph1b.audiobook.AudioPlayerService";

    public static final String CONTROL_PLAY_PAUSE = TAG + ".CONTROL_PLAY_PAUSE";
    public static final String CONTROL_CHANGE_MEDIA_POSITION = TAG + ".CONTROL_CHANGE_MEDIA_POSITION";
    public static final String CONTROL_SLEEP = TAG + ".CONTROL_SLEEP";

    public static final String GUI = TAG + ".GUI";
    public static final String GUI_BOOK = TAG + ".GUI_BOOK";
    public static final String GUI_ALL_MEDIA = TAG + ".GUI_ALL_MEDIA";
    public static final String GUI_MEDIA = TAG + ".GUI_MEDIA";

    private static final String NOTIFICATION_PAUSE = TAG + ".NOTIFICATION_PAUSE";


    private AudioManager audioManager;
    private MediaPlayer mediaPlayer;

    private boolean pauseBecauseHeadset = false;

    private int lastState;

    private MediaDetail media;
    private ArrayList<MediaDetail> allMedia;
    private BookDetail book;
    private Handler handler;

    private RemoteControlClient mRemoteControlClient;

    private ComponentName myEventReceiver;

    private int audioFocus = 0;

    private final ReentrantLock playerLock = new ReentrantLock();

    //keeps track of bc registered
    private boolean noisyRCRegistered = false;
    private boolean headsetRCRegistered = false;

    private ComponentName widgetComponentName;
    private RemoteViews remoteViews;

    public final StateManager stateManager = new StateManager();

    private final OnStateChangedListener onStateChangedListener = new OnStateChangedListener() {
        @Override
        public void onStateChanged(PlayerStates state) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "state changed called: " + state);
            if (state == PlayerStates.STARTED) {
                remoteViews.setImageViewResource(R.id.widgetPlayButton, R.drawable.av_pause);
            } else {
                remoteViews.setImageViewResource(R.id.widgetPlayButton, R.drawable.av_play);
            }
            AppWidgetManager.getInstance(getApplicationContext()).updateAppWidget(widgetComponentName, remoteViews);
        }
    };


    public class LocalBinder extends Binder {
        public AudioPlayerService getService() {
            return AudioPlayerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        bcm = LocalBroadcastManager.getInstance(this);
        db = DataBaseHelper.getInstance(this);
        mediaPlayer = new MediaPlayer();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        handler = new Handler();


        myEventReceiver = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());

        //setup remote client
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            ComponentName nmm = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());
            mediaButtonIntent.setComponent(nmm);
            PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
            mRemoteControlClient = new RemoteControlClient(mediaPendingIntent);
        }


        //state manager to update widget
        widgetComponentName = new ComponentName(this, WidgetProvider.class);
        remoteViews = new RemoteViews(this.getPackageName(), R.layout.widget);
        stateManager.addStateChangeListener(onStateChangedListener);

        //registering receiver for pause from notification
        registerReceiver(notificationPauseReceiver, new IntentFilter(NOTIFICATION_PAUSE));
    }

    private void handleAction(Intent intent) {
        String action = intent.getAction();
        if (intent.getAction() != null) {
            if (action.equals(CONTROL_PLAY_PAUSE)) {
                if (stateManager.getState() == PlayerStates.STARTED)
                    pause();
                else
                    play();
            } else if (action.equals(CONTROL_CHANGE_MEDIA_POSITION)) {
                int position = intent.getIntExtra(CONTROL_CHANGE_MEDIA_POSITION, -1);
                if (position != -1)
                    changePosition(position);
            } else if (action.equals(CONTROL_SLEEP)) {
                int sleepTime = intent.getIntExtra(CONTROL_SLEEP, -1);
                if (sleepTime > 0)
                    sleepTimer(sleepTime);
            }
        }
    }

    private void handleKeyCode(int keyCode) {
        switch (keyCode) {
            case -1:
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (stateManager.getState() == PlayerStates.STARTED)
                    pause();
                else
                    play();
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                nextSong();
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                previousSong();
                break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                fastForward();
                break;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                rewind();
                break;
            default:
                break;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);


        //int newBookId = intent.getIntExtra(BOOK_ID, 0);
        BookDetail newBook = intent.getParcelableExtra(GUI_BOOK);
        if (newBook != null)
            if ((book != null && book.getId() != newBook.getId()) || book == null) {
                book = newBook;
                allMedia = intent.getParcelableArrayListExtra(GUI_ALL_MEDIA);
                initBook();
                prepare(book.getPosition());
            }

        handleAction(intent);

        int keyCode = intent.getIntExtra(RemoteControlReceiver.KEYCODE, -1);
        handleKeyCode(keyCode);

        updateGUI();

        //sticky to re-init when stopped
        return Service.START_REDELIVER_INTENT;
    }


    private final BroadcastReceiver notificationPauseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pause();
        }
    };

    private void foreground(Boolean set) {
        if (set)
            new StartNotificationAsync().execute();
        else
            stopForeground(true);
    }


    private class StartNotificationAsync extends AsyncTask<Void, Void, Bitmap> {

        @Override
        protected void onPreExecute() {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(BookChoose.SHARED_PREFS_CURRENT, book.getId());
            editor.apply();
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            String thumbPath = book.getThumb();

            Resources res = getApplicationContext().getResources();
            int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
            int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
            int size = height < width ? height : width;

            if (thumbPath == null || thumbPath.equals("") || !new File(thumbPath).exists() || new File(thumbPath).isDirectory()) {
                return CommonTasks.genCapital(book.getName(), size, getResources());
            } else {
                Bitmap thumb = BitmapFactory.decodeFile(thumbPath);
                return Bitmap.createScaledBitmap(thumb, width, height, false);
            }
        }

        @SuppressWarnings("deprecation")
        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            Context c = getApplicationContext();

            Intent bookPlayIntent = new Intent(c, BookPlay.class);
            PendingIntent pendingIntent = android.support.v4.app.TaskStackBuilder.create(c)
                    .addNextIntentWithParentStack(bookPlayIntent)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent i = new Intent(NOTIFICATION_PAUSE);
            PendingIntent pauseActionPI = PendingIntent.getBroadcast(getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification.Builder builder = new Notification.Builder(getApplicationContext());
            builder.setContentTitle(book.getName())
                    .setContentText(media.getName())
                    .setLargeIcon(result)
                    .setSmallIcon(R.drawable.notification)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setAutoCancel(true);
            int pos = positionInAllMedia(media.getId());
            if (allMedia.size() > 1 && pos != -1) {
                builder.setContentInfo(String.valueOf(pos) + "/" + String.valueOf(allMedia.size()));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                builder.setPriority(Notification.PRIORITY_HIGH);
                builder.addAction(R.drawable.av_pause_dark, "Pause", pauseActionPI);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                builder.setShowWhen(false);
            Notification notification = builder.getNotification();
            notification.flags |= Notification.FLAG_HIGH_PRIORITY;
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onDestroy() {
        playerLock.lock();
        try {
            registerAsPlaying(false);
            if (stateManager.getState() != PlayerStates.DEAD)
                mediaPlayer.reset();
            mediaPlayer.release();
            stateManager.setState(PlayerStates.DEAD);
        } finally {
            playerLock.unlock();
        }
        unregisterReceiver(notificationPauseReceiver);
        super.onDestroy();
    }


    private final BroadcastReceiver audioBecomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (stateManager.getState() == PlayerStates.STARTED)
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
                                if (resumeOnReplug)
                                    play();
                                pauseBecauseHeadset = false;
                            }
                        }
                    }
                }
            }
        }
    };


    @SuppressLint("NewApi")
    private void initBook() {

        if (BuildConfig.DEBUG) Log.d(TAG, "Init book with id " + book.getId());

        registerAsPlaying(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (allMedia.size() > 1) {
                mRemoteControlClient.setTransportControlFlags(
                        RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                                RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                                RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                                RemoteControlClient.FLAG_KEY_MEDIA_REWIND |
                                RemoteControlClient.FLAG_KEY_MEDIA_FAST_FORWARD
                );
            } else {
                mRemoteControlClient.setTransportControlFlags(
                        RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                                RemoteControlClient.FLAG_KEY_MEDIA_REWIND |
                                RemoteControlClient.FLAG_KEY_MEDIA_FAST_FORWARD
                );
            }
        }
    }

    private void prepare(int mediaId) {
        if (BuildConfig.DEBUG) Log.d(TAG, "prepare()" + mediaId);
        playerLock.lock();
        try {
            //if no proper current song is found, use the first
            for (MediaDetail m : allMedia)
                if (m.getId() == mediaId)
                    media = m;
            if (media == null)
                media = allMedia.get(0);

            if (stateManager.getState() == PlayerStates.DEAD)
                mediaPlayer = new MediaPlayer();
            else
                mediaPlayer.reset();
            stateManager.setState(PlayerStates.IDLE);

            if (book.getPosition() != media.getId()) {
                book.setPosition(media.getId());
                db.updateBookAsync(book);
            }


            String path = media.getPath();
            int position = media.getPosition();

            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mediaPlayer.setDataSource(this, Uri.parse(path));
                stateManager.setState(PlayerStates.INITIALIZED);
                mediaPlayer.prepare();
                stateManager.setState(PlayerStates.PREPARED);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //updates metadata for proper lock screen information
            updateMetaData();

            //requests wake-mode which is automatically released when pausing
            mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
            mediaPlayer.seekTo(position);
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    playerLock.lock();
                    try {
                        stateManager.setState(PlayerStates.PLAYBACK_COMPLETED);
                    } finally {
                        playerLock.unlock();
                    }
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


    private void updateGUI() {
        if (stateManager.getState() != PlayerStates.DEAD) {
            if (playerLock.tryLock()) {
                try {
                    stateManager.setTime(mediaPlayer.getCurrentPosition());
                    Intent i = new Intent(GUI);
                    // i.putExtra(GUI_BOOK, book);
                    // i.putExtra(GUI_ALL_MEDIA, allMedia);
                    i.putExtra(GUI_MEDIA, media);
                    bcm.sendBroadcast(i);
                } finally {
                    playerLock.unlock();
                }
            }
        }
    }

    private void sleepTimer(int sleepTime) {
        Timer sandman = new Timer();
        TimerTask sleepSand = new TimerTask() {
            @Override
            public void run() {
                playerLock.lock();
                try {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Good night everyone");
                    if (stateManager.getState() != PlayerStates.DEAD)
                        mediaPlayer.reset();
                    mediaPlayer.release();
                    registerAsPlaying(false);
                    stateManager.setState(PlayerStates.DEAD);
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
        PlayerStates state = stateManager.getState();
        if (state == PlayerStates.STARTED || state == PlayerStates.PREPARED || state == PlayerStates.PAUSED) {
            playerLock.lock();
            try {
                int position = mediaPlayer.getCurrentPosition();
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                int changeTimeAmount = sharedPref.getInt(getString(R.string.pref_change_amount), 20) * 1000;
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Fast Forward by " + changeTimeAmount);
                int newPosition = position + changeTimeAmount;
                if (newPosition >= 0) {
                    mediaPlayer.seekTo(newPosition);
                    media.setPosition(newPosition);
                    db.updateMediaAsync(media);
                }
                stateManager.setTime(mediaPlayer.getCurrentPosition());

            } finally {
                playerLock.unlock();
            }
        }
    }

    public void rewind() {
        PlayerStates state = stateManager.getState();
        if (state == PlayerStates.PLAYBACK_COMPLETED || state == PlayerStates.DEAD)
            prepare(media.getId());

        if (state == PlayerStates.STARTED || state == PlayerStates.PREPARED || state == PlayerStates.PAUSED) {
            playerLock.lock();
            try {
                int position = mediaPlayer.getCurrentPosition();
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                int changeTimeAmount = sharedPref.getInt(getString(R.string.pref_change_amount), 20) * 1000;
                int newPosition = position - changeTimeAmount;
                if (newPosition > 0) {
                    mediaPlayer.seekTo(newPosition);
                    media.setPosition(newPosition);
                    db.updateMediaAsync(media);
                }
                stateManager.setTime(mediaPlayer.getCurrentPosition());
            } finally {
                playerLock.unlock();
            }
        }
    }

    public void previousSong() {
        int currentId = media.getId();
        for (int i = 1; i < allMedia.size(); i++) { //starting at #1 to prevent change when on first song
            if (allMedia.get(i).getId() == currentId) {
                boolean wasPlaying = ((stateManager.getState() == PlayerStates.STARTED) ||
                        (stateManager.getState() == PlayerStates.PLAYBACK_COMPLETED));
                int mediaId = allMedia.get(i - 1).getId();
                prepare(mediaId);
                book.setPosition(media.getId());
                db.updateBookAsync(book);
                if (wasPlaying)
                    play();
                updateGUI();
                break;
            }
        }
    }

    public void nextSong() {
        int currentId = media.getId();
        for (int i = 0; i < allMedia.size() - 1; i++) { //-1 to prevent change when already last song reached
            if (allMedia.get(i).getId() == currentId) {
                boolean wasPlaying = ((stateManager.getState() == PlayerStates.STARTED) ||
                        (stateManager.getState() == PlayerStates.PLAYBACK_COMPLETED));
                int mediaId = allMedia.get(i + 1).getId();
                prepare(mediaId);
                book.setPosition(media.getId());
                db.updateBookAsync(book);
                if (wasPlaying)
                    play();
                updateGUI();
                break;
            }
        }

        // if at last position, remove handler and notification, audio-focus
        if (currentId == allMedia.get(allMedia.size() - 1).getId()) {
            playerLock.lock();
            try {
                if (stateManager.getState() != PlayerStates.DEAD)
                    mediaPlayer.reset();
                mediaPlayer.release();
                stateManager.setState(PlayerStates.DEAD);
                registerAsPlaying(false);
            } finally {
                playerLock.unlock();
            }
        }
    }

    public void changeBookPosition(int mediaId) {
        if (mediaId != book.getPosition()) {
            book.setPosition(mediaId);
            db.updateBookAsync(book);
            Boolean wasPlaying = (stateManager.getState() == PlayerStates.STARTED);
            prepare(mediaId);
            if (wasPlaying)
                play();
            updateGUI();
        }
    }

    @SuppressLint("NewApi")
    public void play() {
        playerLock.lock();
        try {
            switch (stateManager.getState()) {
                case PREPARED:
                case PAUSED:
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "starting MediaPlayer");
                    mediaPlayer.start();
                    stateManager.setState(PlayerStates.STARTED);
                    registerAsPlaying(true);
                    updateGUI();
                    break;
                case IDLE:
                case DEAD:
                case STOPPED:
                case PLAYBACK_COMPLETED:
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

    private void registerAsPlaying(Boolean playing) {
        if (playing) {
            //setup notification
            foreground(true);

            //starting runner to update gui
            handler.postDelayed(savePositionRunner, 10000);
            handler.postDelayed(updateSeekBarRunner, 1000);

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

            //registering receivers for information on media controls
            if (!noisyRCRegistered) {
                registerReceiver(audioBecomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
                noisyRCRegistered = true;
            }
            if (!headsetRCRegistered) {
                registerReceiver(headsetPlugReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
                headsetRCRegistered = true;
            }
        } else {
            //stop notification
            foreground(false);

            //stops runner who were updating gui frequently
            handler.removeCallbacks(savePositionRunner);
            handler.removeCallbacks(updateSeekBarRunner);

            //abandon audio-focus and disabling lock-screen controls
            audioManager.abandonAudioFocus(audioFocusChangeListener);
            audioFocus = 0;
            audioManager.unregisterMediaButtonEventReceiver(myEventReceiver);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                audioManager.unregisterRemoteControlClient(mRemoteControlClient);
                mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
            }

            //releasing control receivers because pause
            if (noisyRCRegistered) {
                unregisterReceiver(audioBecomingNoisyReceiver);
                noisyRCRegistered = false;
            }

        }
    }


    public void pause() {
        registerAsPlaying(false);
        if (stateManager.getState() == PlayerStates.STARTED) {
            playerLock.lock();
            try {
                //saves current position, then pauses
                int position = mediaPlayer.getCurrentPosition();
                if (position > 0) {
                    media.setPosition(position);
                    db.updateMediaAsync(media);
                }
                mediaPlayer.pause();
                stateManager.setState(PlayerStates.PAUSED);
            } finally {
                playerLock.unlock();
            }
        }
    }

    public void changePosition(int position) {
        playerLock.lock();
        try {
            PlayerStates state = stateManager.getState();
            if (state == PlayerStates.STARTED || state == PlayerStates.PREPARED || state == PlayerStates.PAUSED) {
                mediaPlayer.seekTo(position);
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
                    if (stateManager.getState() == PlayerStates.STARTED) {
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
            if (stateManager.getState() == PlayerStates.STARTED) {
                if (playerLock.tryLock()) {
                    try {
                        stateManager.setTime(mediaPlayer.getCurrentPosition());
                    } finally {
                        playerLock.unlock();
                    }
                }
                handler.postDelayed(updateSeekBarRunner, 1000);
            }
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

    int positionInAllMedia(int mediaId) {
        for (int i = 0; i < allMedia.size(); i++) {
            if (mediaId == allMedia.get(i).getId())
                return i + 1;
        }
        return -1;
    }

    @SuppressLint("NewApi")
    private void updateMetaData() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "editing metadata with" + media.getName());
                    RemoteControlClient.MetadataEditor editor = mRemoteControlClient.editMetadata(true);
                    editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, media.getName());
                    editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, book.getName());
                    String coverPath = book.getCover();
                    Bitmap bitmap;
                    if (coverPath == null || coverPath.equals("") || !new File(coverPath).exists() || new File(coverPath).isDirectory()) {
                        bitmap = CommonTasks.genCapital(book.getName(), 500, getResources());
                    } else {
                        bitmap = BitmapFactory.decodeFile(book.getCover());
                    }
                    editor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, bitmap);
                    editor.apply();
                }
                return null;
            }
        }.execute();
    }
}