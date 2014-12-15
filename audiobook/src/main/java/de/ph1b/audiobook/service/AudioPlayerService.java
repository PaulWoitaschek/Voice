package de.ph1b.audiobook.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BookChoose;
import de.ph1b.audiobook.activity.BookPlay;
import de.ph1b.audiobook.content.BookDetail;
import de.ph1b.audiobook.content.DataBaseHelper;
import de.ph1b.audiobook.content.MediaDetail;
import de.ph1b.audiobook.interfaces.OnStateChangedListener;
import de.ph1b.audiobook.receiver.RemoteControlReceiver;
import de.ph1b.audiobook.receiver.WidgetProvider;
import de.ph1b.audiobook.utils.ImageHelper;
import de.ph1b.audiobook.utils.MediaPlayerWrapper;

public class AudioPlayerService extends Service {


    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "de.ph1b.audiobook.AudioPlayerService";
    public static final String GUI = TAG + ".GUI";
    public static final String GUI_BOOK_ID = TAG + ".GUI_BOOK_ID";
    public static final String GUI_MEDIA = TAG + ".GUI_MEDIA";
    private final IBinder mBinder = new LocalBinder();

    private final ReentrantLock playerLock = new ReentrantLock();
    private final ScheduledExecutorService sandMan = Executors.newSingleThreadScheduledExecutor();
    public StateManager stateManager;
    private DataBaseHelper db;
    private LocalBroadcastManager bcm;
    private AudioManager audioManager;
    private MediaPlayerWrapper mediaPlayer;
    private volatile boolean pauseBecauseHeadset = false;

    private PlaybackStateCompat.Builder stateBuilder;

    /**
     * If audio is becoming noisy, pause the player.
     */
    private final BroadcastReceiver audioBecomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (stateManager.getState() == PlayerStates.STARTED)
                pauseBecauseHeadset = true;
            pause(false);
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
                                boolean resumeOnReplug = sharedPref.getBoolean(context.getString(R.string.pref_key_resume_on_replug), true);
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
    private NotificationCompat.Builder notificationBuilder = null;
    private boolean pauseBecauseLossTransient = false;
    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if (stateManager.getState() == PlayerStates.STARTED) {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Paused by audio-focus loss transient.");
                        pause(false);
                        pauseBecauseLossTransient = true;
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "started by audioFocus gained");

                    if (pauseBecauseLossTransient) {
                        play();
                    } else if (stateManager.getState() == PlayerStates.STARTED) {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "increasing volume because of regain focus from transient-can-duck");
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "paused by audioFocus loss");
                    pause(true);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    if (stateManager.getState() == PlayerStates.STARTED) {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "lowering volume because of af loss transcient can duck");
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
                    }
                    break;
                default:
                    break;
            }
        }
    };
    private MediaDetail media;
    private ArrayList<MediaDetail> allMedia;
    private BookDetail book;
    private Handler handler;
    @SuppressWarnings("deprecation")
    private RemoteControlClient remoteControlClient;
    private MediaSessionCompat mediaSession;
    private ComponentName widgetComponentName;
    private RemoteViews widgetRemoteViews;
    private final OnStateChangedListener onStateChangedListener = new OnStateChangedListener() {
        @Override
        public void onStateChanged(PlayerStates state) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "state changed called: " + state);
            if (state == PlayerStates.STARTED) {
                widgetRemoteViews.setImageViewResource(R.id.playPause, R.drawable.ic_pause_white_48dp);
            } else {
                widgetRemoteViews.setImageViewResource(R.id.playPause, R.drawable.ic_play_arrow_white_48dp);
            }
            AppWidgetManager.getInstance(getApplicationContext()).updateAppWidget(widgetComponentName, widgetRemoteViews);
        }
    };
    /**
     * If <code>true</code>, the current track will be played to the end after the sleep timer triggers.
     */
    private boolean stopAfterCurrentTrack = false;
    private ScheduledFuture<?> sleepSand;
    private OnSleepStateChangedListener onSleepStateChangedListener;

    public AudioPlayerService() {
    }

    public float getPlaybackSpeed() {
        return mediaPlayer.getPlaybackSpeed();
    }

    public void setPlaybackSpeed(float playbackSpeed) {
        mediaPlayer.setPlaybackSpeed(playbackSpeed);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void initRemoteControlClient() {
        ComponentName eventReceiver = new ComponentName(AudioPlayerService.this.getPackageName(), RemoteControlReceiver.class.getName());
        //noinspection deprecation
        audioManager.registerMediaButtonEventReceiver(eventReceiver);

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(eventReceiver);
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0);
        //noinspection deprecation
        remoteControlClient = new RemoteControlClient(mediaPendingIntent);
        //noinspection deprecation
        remoteControlClient.setTransportControlFlags(
                RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                        RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE |
                        RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                        RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                        RemoteControlClient.FLAG_KEY_MEDIA_REWIND |
                        RemoteControlClient.FLAG_KEY_MEDIA_FAST_FORWARD);
        //noinspection deprecation
        audioManager.registerRemoteControlClient(remoteControlClient);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        bcm = LocalBroadcastManager.getInstance(this);
        db = DataBaseHelper.getInstance(this);
        mediaPlayer = new MediaPlayerWrapper(this);
        stateManager = new StateManager();
        stateManager.setState(PlayerStates.IDLE);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        handler = new Handler();

        mediaSession = new MediaSessionCompat(AudioPlayerService.this, TAG);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        //callback
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                String action = mediaButtonEvent.getAction();
                if (BuildConfig.DEBUG) Log.d(TAG, "onMediaButtonEvent was called");
                if (action.equals(Intent.ACTION_MEDIA_BUTTON)) {
                    KeyEvent event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        int keyCode = event.getKeyCode();
                        return handleKeyCode(keyCode);
                    }
                }
                return false;
            }
        });

        mediaSession.setActive(true);

        //init builder
        stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_FAST_FORWARD |
                PlaybackStateCompat.ACTION_REWIND | PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_STOP);

        if (Build.VERSION.SDK_INT < 21 && Build.VERSION.SDK_INT > 14) {
            initRemoteControlClient();
        }

        registerReceiver(audioBecomingNoisyReceiver, new IntentFilter
                (AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        registerReceiver(headsetPlugReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

        //state manager to update widget
        widgetComponentName = new ComponentName(this, WidgetProvider.class);
        widgetRemoteViews = new RemoteViews(this.getPackageName(), R.layout.widget);

        stateManager.addStateChangeListener(onStateChangedListener);
    }

    private boolean handleKeyCode(int keyCode) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "handling keycode, state is: " + stateManager.getState() + "keycode is: " + keyCode);
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                play();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                pause(false);
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (stateManager.getState() == PlayerStates.STARTED) {
                    pause(false);
                } else {
                    play();
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                pause(true);
                finish();
                return true;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                fastForward();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                rewind();
                return true;
            default:
                return false;
        }
    }

    private void initBook(int bookId) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Initializing book with ID: " + bookId);
        book = db.getBook(bookId);
        allMedia = db.getMediaFromBook(bookId);
        stopForeground(true);
        prepare(book.getCurrentMediaId());
        pauseBecauseLossTransient = false;

        updateWidget();
        AppWidgetManager.getInstance(getApplicationContext()).updateAppWidget(widgetComponentName, widgetRemoteViews);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.d(TAG, "onStartCommand was called!");
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        int defaultBookId = settings.getInt(BookChoose.SHARED_PREFS_CURRENT, -1);

        /**
         * If intent is <code>null</code>, it means, that the service has been started because of
         * <code>START_STICKY</code>. So we will prepare the current book defines in
         * SharedPreferences and leave it as it is.
         */
        if (intent == null) {
            if (defaultBookId != -1) {
                initBook(defaultBookId);
            } else {
                if (BuildConfig.DEBUG) Log.d(TAG, "We found no default book. Stop self!");
                stopSelf();
            }
        } else {
            /**
             * If the <code>intent != null</code>, we will handle the <code>intent</code> or prepare
             * the new book if its id is different from the one already there.
             */
            int newBookId = intent.getIntExtra(GUI_BOOK_ID, -1);
            if (book == null || (newBookId != -1 && (book.getId() != newBookId))) {
                if (newBookId == -1 && defaultBookId != -1) {
                    initBook(defaultBookId);
                } else if (newBookId != -1)
                    initBook(newBookId);
            }
            if (book != null) {
                int keyCode = intent.getIntExtra(Intent.EXTRA_KEY_EVENT, -1);
                handleKeyCode(keyCode);
                updateGUI();
            }
        }

        return Service.START_STICKY;
    }

    private void updateWidget() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (allMedia.size() > 1) {
                    widgetRemoteViews.setTextViewText(R.id.summary, media.getName());
                } else {
                    widgetRemoteViews.setTextViewText(R.id.summary, "");
                }

                Bitmap cover;
                if (book.getCover() != null && new File(book.getCover()).exists()) {
                    cover = ImageHelper.genBitmapFromFile(book.getCover(), AudioPlayerService.this, ImageHelper.TYPE_NOTIFICATION_SMALL);
                } else {
                    cover = ImageHelper.genCapital(book.getName(), AudioPlayerService.this, ImageHelper.TYPE_NOTIFICATION_SMALL);
                }

                widgetRemoteViews.setImageViewBitmap(R.id.imageView, cover);
                widgetRemoteViews.setTextViewText(R.id.title, book.getName());
            }
        }).start();
    }

    private Notification getNotification() {
        if (notificationBuilder == null) {
            notificationBuilder = new NotificationCompat.Builder(AudioPlayerService.this);
        }

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(BookChoose.SHARED_PREFS_CURRENT, book.getId());
        editor.apply();

        RemoteViews smallViewRemote = new RemoteViews(getPackageName(), R.layout.notification_small);
        RemoteViews bigViewRemote = new RemoteViews(getPackageName(), R.layout.notification_big);

        String coverPath = book.getCover();
        Bitmap smallCover;
        Bitmap bigCover;
        if (coverPath == null || coverPath.equals("") || !new File(coverPath).exists() || new File(coverPath).isDirectory()) {
            smallCover = ImageHelper.genCapital(book.getName(), this, ImageHelper.TYPE_NOTIFICATION_SMALL);
            bigCover = ImageHelper.genCapital(book.getName(), this, ImageHelper.TYPE_NOTIFICATION_BIG);
        } else {
            smallCover = ImageHelper.genBitmapFromFile(coverPath, this, ImageHelper.TYPE_NOTIFICATION_SMALL);
            bigCover = ImageHelper.genBitmapFromFile(coverPath, this, ImageHelper.TYPE_NOTIFICATION_BIG);
        }

        Intent rewindIntent = new Intent(AudioPlayerService.this, AudioPlayerService.class);
        rewindIntent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.KEYCODE_MEDIA_REWIND);
        PendingIntent rewindPI = PendingIntent.getService(getApplicationContext(), 0, rewindIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent playPauseIntent = new Intent(AudioPlayerService.this, AudioPlayerService.class);
        if (stateManager.getState() == PlayerStates.STARTED) {
            playPauseIntent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            smallViewRemote.setImageViewResource(R.id.playPause, R.drawable.ic_pause_white_48dp);
            bigViewRemote.setImageViewResource(R.id.playPause, R.drawable.ic_pause_white_48dp);
        } else {
            playPauseIntent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            smallViewRemote.setImageViewResource(R.id.playPause, R.drawable.ic_play_arrow_white_48dp);
            bigViewRemote.setImageViewResource(R.id.playPause, R.drawable.ic_play_arrow_white_48dp);
        }
        PendingIntent playPausePI = PendingIntent.getService(AudioPlayerService.this, 1, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent fastForwardIntent = new Intent(AudioPlayerService.this, AudioPlayerService.class);
        fastForwardIntent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
        PendingIntent fastForwardPI = PendingIntent.getService(AudioPlayerService.this, 2, fastForwardIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent stopIntent = new Intent(AudioPlayerService.this, AudioPlayerService.class);
        stopIntent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.KEYCODE_MEDIA_STOP);
        PendingIntent stopPI = PendingIntent.getService(AudioPlayerService.this, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        smallViewRemote.setImageViewBitmap(R.id.imageView, smallCover);
        smallViewRemote.setTextViewText(R.id.title, book.getName());
        smallViewRemote.setTextViewText(R.id.summary, media.getName());

        smallViewRemote.setOnClickPendingIntent(R.id.rewind, rewindPI);
        smallViewRemote.setOnClickPendingIntent(R.id.playPause, playPausePI);
        smallViewRemote.setOnClickPendingIntent(R.id.fast_forward, fastForwardPI);
        smallViewRemote.setOnClickPendingIntent(R.id.closeButton, stopPI);

        bigViewRemote.setImageViewBitmap(R.id.imageView, bigCover);
        bigViewRemote.setTextViewText(R.id.title, book.getName());
        bigViewRemote.setTextViewText(R.id.summary, media.getName());

        bigViewRemote.setOnClickPendingIntent(R.id.rewind, rewindPI);
        bigViewRemote.setOnClickPendingIntent(R.id.playPause, playPausePI);
        bigViewRemote.setOnClickPendingIntent(R.id.fast_forward, fastForwardPI);
        bigViewRemote.setOnClickPendingIntent(R.id.closeButton, stopPI);

        Intent bookPlayIntent = new Intent(AudioPlayerService.this, BookPlay.class);
        bookPlayIntent.putExtra(GUI_BOOK_ID, book.getId());
        PendingIntent pendingIntent = android.support.v4.app.TaskStackBuilder.create(AudioPlayerService.this)
                .addNextIntentWithParentStack(bookPlayIntent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContent(smallViewRemote)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        //noinspection deprecation
        Notification notification = notificationBuilder.getNotification();

        if (Build.VERSION.SDK_INT >= 16) {
            notification.bigContentView = bigViewRemote;
        }

        return notification;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void finish() {
        stopForeground(true);

        playerLock.lock();
        try {
            if (stateManager.getState() != PlayerStates.DEAD)
                mediaPlayer.reset();
            mediaPlayer.release();
            stateManager.setState(PlayerStates.DEAD);
        } finally {
            playerLock.unlock();
        }

        try {
            unregisterReceiver(audioBecomingNoisyReceiver);
            unregisterReceiver(headsetPlugReceiver);
        } catch (IllegalArgumentException ignored) {
        }

        stopAfterCurrentTrack = false;
        if (onSleepStateChangedListener != null) {
            onSleepStateChangedListener.onSleepStateChanged(false);
        }

        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 21) {
            //noinspection deprecation
            audioManager.unregisterRemoteControlClient(remoteControlClient);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        finish();
    }

    /**
     * Prepares a new media. Also handles the onCompletion and updates the database. After preparing,
     * state should be PlayerStates.PREPARED
     *
     * @param mediaId The mediaId to be prepared for playback
     */
    private void prepare(int mediaId) {
        if (BuildConfig.DEBUG) Log.d(TAG, "prepare() mediaId: " + mediaId);
        playerLock.lock();
        try {
            //if no proper current song is found, use the first
            media = null;
            for (MediaDetail m : allMedia) {
                if (m.getId() == mediaId) {
                    media = m;
                    break;
                }
            }
            if (media == null)
                media = allMedia.get(0);

            if (stateManager.getState() == PlayerStates.DEAD)
                mediaPlayer = new MediaPlayerWrapper(this);
            else
                mediaPlayer.reset();
            stateManager.setState(PlayerStates.IDLE);

            int position;
            //setting up new file depending on if it is the latest media in book
            if (book.getCurrentMediaId() != media.getId()) {
                book.setCurrentMediaId(media.getId());
                position = 0;
                book.setCurrentMediaPosition(0);
                updateBookAsync(book);
            } else {
                position = book.getCurrentMediaPosition();
            }

            updateWidget();


            String path = media.getPath();

            //setting position to 0 if last file was reached
            if (position == media.getDuration())
                position = 0;

            mediaPlayer.setDataSource(path);
            stateManager.setState(PlayerStates.INITIALIZED);
            mediaPlayer.prepare();
            stateManager.setState(PlayerStates.PREPARED);

            // metadata
            MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
            String coverPath = book.getCover();
            Bitmap bitmap;
            if (coverPath == null || !new File(coverPath).exists() || new File(coverPath).isDirectory()) {
                bitmap = ImageHelper.genCapital(book.getName(), getApplication(), ImageHelper.TYPE_COVER);
            } else {
                bitmap = ImageHelper.genBitmapFromFile(coverPath, AudioPlayerService.this, ImageHelper.TYPE_COVER);
            }
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap);
            builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, media.getDuration());
            builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, media.getName());
            builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, book.getName());
            mediaSession.setMetadata(builder.build());

            if (Build.VERSION.SDK_INT < 21 && Build.VERSION.SDK_INT > 14) {
                updateRemoteControlValues(bitmap);
            }

            //requests wake-mode which is automatically released when pausing
            mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE);
            mediaPlayer.seekTo(position);
            mediaPlayer.setOnCompletionListener(new MediaPlayerWrapper.OnCompletionListener() {

                @Override
                public void onCompletion() {
                    if (stopAfterCurrentTrack) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Stopping service because oncompletion has been triggered");
                        }
                        finish();
                    } else {
                        playerLock.lock();
                        try {
                            stateManager.setState(PlayerStates.PLAYBACK_COMPLETED);
                        } finally {
                            playerLock.unlock();
                        }
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Next Song by onCompletion");

                        //setting book to last position
                        book.setCurrentMediaPosition(media.getDuration());
                        updateBookAsync(book);

                        boolean wasPlaying = ((stateManager.getState() == PlayerStates.STARTED) ||
                                (stateManager.getState() == PlayerStates.PLAYBACK_COMPLETED));

                        int index = allMedia.indexOf(media);
                        // start next one if there is any
                        if (index < allMedia.size() - 1) {//-1 to prevent change when already last song reached
                            prepare(allMedia.get(index + 1).getId());
                            if (wasPlaying)
                                play();
                            updateGUI();
                        } else { //else unregister as playing
                            playerLock.lock();
                            try {
                                pause(true);
                                stopForeground(true);
                                if (stateManager.getState() != PlayerStates.DEAD)
                                    mediaPlayer.reset();
                                mediaPlayer.release();
                                stateManager.setState(PlayerStates.DEAD);
                            } finally {
                                playerLock.unlock();
                            }
                        }
                    }
                }
            });
        } finally {
            playerLock.unlock();
        }
    }

    /**
     * Updates the book Async via a new Thread.
     *
     * @param book The book to be updated
     */
    private void updateBookAsync(final BookDetail book) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (playerLock.tryLock()) {
                    try {
                        db.updateBook(book);
                    } finally {
                        playerLock.unlock();
                    }
                }
            }
        }).start();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void updateRemoteControlValues(Bitmap bitmap) {
        //updates metadata for proper lock screen information
        @SuppressWarnings("deprecation") RemoteControlClient.MetadataEditor editor = remoteControlClient.editMetadata(true);
        editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, media.getName());
        editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, book.getName());
        //noinspection deprecation
        editor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, bitmap);
        editor.apply();
    }

    /**
     * Sends a broadcast signaling that there are changes affecting the gui and sends the current
     * media as parcelable.
     *
     * @see de.ph1b.audiobook.fragment.BookPlayFragment#updateGUIReceiver
     */
    public void updateGUI() {
        if (stateManager.getState() != PlayerStates.DEAD) {
            if (playerLock.tryLock()) {
                try {
                    stateManager.setTime(mediaPlayer.getCurrentPosition());
                    Intent i = new Intent(GUI);
                    i.putExtra(GUI_MEDIA, media);
                    bcm.sendBroadcast(i);
                } finally {
                    playerLock.unlock();
                }
            }
        }
    }

    public void setOnSleepStateChangedListener(OnSleepStateChangedListener onSleepStateChangedListener) {
        this.onSleepStateChangedListener = onSleepStateChangedListener;
    }

    public boolean sleepSandActive() {
        return sleepSand != null && !sleepSand.isCancelled() && !sleepSand.isDone();
    }

    public void toggleSleepSand() {
        if (sleepSandActive()) {
            sleepSand.cancel(false);
            stopAfterCurrentTrack = false;
            if (onSleepStateChangedListener != null) {
                onSleepStateChangedListener.onSleepStateChanged(false);
            }

            Toast sleepToast = Toast.makeText(this, R.string.sleep_timer_canceled, Toast.LENGTH_SHORT);
            sleepToast.show();
        } else {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            int minutes = sp.getInt(getString(R.string.pref_key_sleep_time), 20);
            stopAfterCurrentTrack = sp.getBoolean(getString(R.string.pref_key_track_to_end), false);

            String message = getString(R.string.sleep_timer_started) + minutes + " " + getString(R.string.minutes);
            Toast sleepToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
            sleepToast.show();

            sleepSand = sandMan.schedule(new Runnable() {
                @Override
                public void run() {
                    if (!stopAfterCurrentTrack) {
                        playerLock.lock();
                        try {
                            pause(true);
                            stopForeground(true);
                            if (BuildConfig.DEBUG) Log.d(TAG, "Good night everyone");
                            if (stateManager.getState() != PlayerStates.DEAD)
                                mediaPlayer.reset();
                            mediaPlayer.release();
                            stateManager.setState(PlayerStates.DEAD);
                            if (onSleepStateChangedListener != null) {
                                onSleepStateChangedListener.onSleepStateChanged(false);
                            }
                        } finally {
                            playerLock.unlock();
                        }
                    } else if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Sandman: We are not stopping right now. We stop after this track.");
                    }
                }
            }, minutes, TimeUnit.MINUTES);
            if (onSleepStateChangedListener != null)
                onSleepStateChangedListener.onSleepStateChanged(true);
        }
    }

    public void fastForward() {
        PlayerStates state = stateManager.getState();
        if (state == PlayerStates.STARTED || state == PlayerStates.PREPARED || state == PlayerStates.PAUSED) {
            playerLock.lock();
            try {
                int position = mediaPlayer.getCurrentPosition();
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                int changeTimeAmount = sharedPref.getInt(getString(R.string.pref_key_seek_time), 20) * 1000;
                int newPosition = position + changeTimeAmount;
                if (newPosition > 0) {
                    if (newPosition > media.getDuration())
                        newPosition = media.getDuration();
                    mediaPlayer.seekTo(newPosition);
                    book.setCurrentMediaPosition(newPosition);
                    updateBookAsync(book);
                }
                stateManager.setTime(newPosition);

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
                int changeTimeAmount = sharedPref.getInt(getString(R.string.pref_key_seek_time), 20) * 1000;
                int newPosition = position - changeTimeAmount;
                if (newPosition < 0)
                    newPosition = 0;
                mediaPlayer.seekTo(newPosition);
                book.setCurrentMediaPosition(newPosition);
                updateBookAsync(book);
                stateManager.setTime(newPosition);
            } finally {
                playerLock.unlock();
            }
        }
    }

    /**
     * Changes the current song in book and prepares the media. If the book was playing, it will start
     * again after being prepared and it always calls {@link #updateGUI()}
     *
     * @param mediaId The new chosen mediaId
     */
    public void changeBookPosition(int mediaId) {
        if (mediaId != book.getCurrentMediaId()) {
            boolean wasPlaying = (stateManager.getState() == PlayerStates.STARTED);
            prepare(mediaId);
            if (wasPlaying)
                play();
            updateGUI();
        }
    }

    /**
     * Plays the current file. Also sets up the controls. If state is {@link de.ph1b.audiobook.service.PlayerStates#PLAYBACK_COMPLETED}
     * then the latest medium is being prepared again.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void play() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "start() was called");
        }
        playerLock.lock();
        try {
            switch (stateManager.getState()) {
                case PREPARED:
                case PAUSED:
                    pauseBecauseLossTransient = false;

                    //requesting audio-focus and setting up lock-screen-controls
                    audioManager.requestAudioFocus(audioFocusChangeListener,
                            AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

                    if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 21) {
                        //noinspection deprecation
                        remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
                    }

                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "starting MediaPlayer");
                    }
                    mediaPlayer.start();
                    stateManager.setState(PlayerStates.STARTED);
                    /**
                     * startForeground has to be called after state was set, so the right buttons
                     * will be set up.
                     */
                    startForeground(NOTIFICATION_ID, getNotification());

                    //starting runner to update gui
                    handler.post(timeChangedRunner);
                    stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1);
                    mediaSession.setPlaybackState(stateBuilder.build());
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

    /**
     * Pauses the book. Does not stopForeground.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void pause(boolean removeNotification) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "pause called! with removeNotification: " + removeNotification);
        }

        if (stateManager.getState() == PlayerStates.STARTED) {
            playerLock.lock();
            try {
                //saves current position, then pauses
                int position = mediaPlayer.getCurrentPosition();
                if (position > 0) {
                    book.setCurrentMediaPosition(position);
                    updateBookAsync(book);
                }

                //stops runner who were updating gui frequently
                handler.removeCallbacks(timeChangedRunner);

                mediaPlayer.pause();
                stateManager.setState(PlayerStates.PAUSED);

                //stop notification if wished
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                stopForeground(removeNotification);
                if (removeNotification) {
                    notificationManager.cancel(NOTIFICATION_ID);
                } else {
                    notificationManager.notify(NOTIFICATION_ID, getNotification());
                }
                if (Build.VERSION.SDK_INT < 21 && Build.VERSION.SDK_INT >= 14) {
                    //noinspection deprecation
                    remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
                }
                stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
                mediaSession.setPlaybackState(stateBuilder.build());
            } finally {
                playerLock.unlock();
            }
        }
    }


    /**
     * Changes the position in the current track. Also updates the database accordingly.
     * Calls {@link #updateGUI()}
     *
     * @param position The position in the current track to jump to
     */
    public void changePosition(int position) {
        playerLock.lock();
        try {
            PlayerStates state = stateManager.getState();
            if (state == PlayerStates.STARTED || state == PlayerStates.PREPARED || state == PlayerStates.PAUSED) {
                mediaPlayer.seekTo(position);
                book.setCurrentMediaPosition(position);
                updateBookAsync(book);
            }
            updateGUI();
        } finally {
            playerLock.unlock();
        }
    }


    public interface OnSleepStateChangedListener {
        public void onSleepStateChanged(boolean active);
    }

    public class LocalBinder extends Binder {
        public AudioPlayerService getService() {
            return AudioPlayerService.this;
        }
    }

    /**
     * Sets the {@link #stateManager} to the current position in the media. Also updates the book calling
     * {@link #updateBookAsync(de.ph1b.audiobook.content.BookDetail)} and repeats itself via the {@link #handler}
     * after a certain time.
     */
    private final Runnable timeChangedRunner = new Runnable() {
        @Override
        public void run() {
            if (playerLock.tryLock()) {
                try {
                    if (stateManager.getState() == PlayerStates.STARTED) {
                        int position = mediaPlayer.getCurrentPosition();
                        stateManager.setTime(position);
                        if (position > 0) {
                            book.setCurrentMediaPosition(position);
                            updateBookAsync(book);
                        }
                    }
                } finally {
                    playerLock.unlock();
                }
            }
            handler.postDelayed(timeChangedRunner, 100);
        }
    };
}