package de.ph1b.audiobook.service;

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
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;

import com.aocate.media.MediaPlayer;

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
import de.ph1b.audiobook.content.BookDetail;
import de.ph1b.audiobook.content.DataBaseHelper;
import de.ph1b.audiobook.content.MediaDetail;
import de.ph1b.audiobook.interfaces.OnStateChangedListener;
import de.ph1b.audiobook.receiver.RemoteControlReceiver;
import de.ph1b.audiobook.receiver.WidgetProvider;
import de.ph1b.audiobook.utils.ImageHelper;

public class AudioPlayerService extends Service {


    private static final int NOTIFICATION_ID = 1;

    private float playbackSpeed = 1;

    private DataBaseHelper db;
    private LocalBroadcastManager bcm;

    private final IBinder mBinder = new LocalBinder();

    private static final String TAG = "de.ph1b.audiobook.AudioPlayerService";

    public static final String CONTROL_PLAY_PAUSE = TAG + ".CONTROL_PLAY_PAUSE";
    public static final String CONTROL_CHANGE_MEDIA_POSITION = TAG + ".CONTROL_CHANGE_MEDIA_POSITION";
    public static final String CONTROL_SLEEP = TAG + ".CONTROL_SLEEP";

    public static final String GUI = TAG + ".GUI";
    public static final String GUI_BOOK_ID = TAG + ".GUI_BOOK_ID";
    public static final String GUI_MEDIA = TAG + ".GUI_MEDIA";

    private AudioManager audioManager;
    private MediaPlayer mediaPlayer;

    private boolean pauseBecauseHeadset = false;

    private int lastState;

    private MediaDetail media;
    private ArrayList<MediaDetail> allMedia;
    private BookDetail book;
    private Handler handler;

    @SuppressWarnings("deprecation")
    private RemoteControlClient mRemoteControlClient;

    private ComponentName myEventReceiver;

    private int audioFocus = 0;

    private MediaSession mediaSession;

    private final ReentrantLock playerLock = new ReentrantLock();

    private ComponentName widgetComponentName;
    private RemoteViews remoteViews;

    public StateManager stateManager;

    private final OnStateChangedListener onStateChangedListener = new OnStateChangedListener() {
        @Override
        public void onStateChanged(PlayerStates state) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "state changed called: " + state);
            if (state == PlayerStates.STARTED) {
                remoteViews.setImageViewResource(R.id.widgetPlayButton, R.drawable.ic_pause_black_36dp);
            } else {
                remoteViews.setImageViewResource(R.id.widgetPlayButton, R.drawable.ic_play_arrow_black_36dp);
            }
            AppWidgetManager.getInstance(getApplicationContext()).updateAppWidget(widgetComponentName, remoteViews);
        }
    };


    public class LocalBinder extends Binder {
        public AudioPlayerService getService() {
            return AudioPlayerService.this;
        }
    }

    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    public void setPlaybackSpeed(float playbackSpeed) {
        this.playbackSpeed = playbackSpeed;
        mediaPlayer.setPlaybackSpeed(playbackSpeed);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public boolean variablePlaybackSpeedIsAvailable() {
        return mediaPlayer.canSetSpeed();
    }


    @Override
    public void onCreate() {
        super.onCreate();

        bcm = LocalBroadcastManager.getInstance(this);
        db = DataBaseHelper.getInstance(this);
        mediaPlayer = new MediaPlayer(AudioPlayerService.this);
        stateManager = new StateManager();
        stateManager.setState(PlayerStates.IDLE);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        handler = new Handler();

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        ComponentName nmm = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());
        mediaButtonIntent.setComponent(nmm);
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);

        if (Build.VERSION.SDK_INT >= 21) {
            mediaSession = new MediaSession(AudioPlayerService.this, TAG);
            mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

            //callback
            mediaSession.setCallback(new MediaSession.Callback() {
                @Override
                public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                    String action = mediaButtonIntent.getAction();
                    if (BuildConfig.DEBUG) Log.d(TAG, "onMediaButtonEvent was called");
                    if (action.equals(Intent.ACTION_MEDIA_BUTTON)) {
                        KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                        if (event.getAction() == KeyEvent.ACTION_DOWN) {
                            int keyCode = event.getKeyCode();
                            return handleKeyCode(keyCode);
                        }
                    }
                    return false;
                }
            });
            mediaSession.setActive(true);
        }

        if (Build.VERSION.SDK_INT < 21) {
            myEventReceiver = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());

            //noinspection deprecation
            mRemoteControlClient = new RemoteControlClient(mediaPendingIntent);
            //noinspection deprecation
            mRemoteControlClient.setTransportControlFlags(
                    RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                            RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE |
                            RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                            RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                            RemoteControlClient.FLAG_KEY_MEDIA_REWIND |
                            RemoteControlClient.FLAG_KEY_MEDIA_FAST_FORWARD);

        }

        registerReceiver(audioBecomingNoisyReceiver, new IntentFilter
                (AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        registerReceiver(headsetPlugReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

        //state manager to update widget
        widgetComponentName = new ComponentName(this, WidgetProvider.class);
        remoteViews = new RemoteViews(this.getPackageName(), R.layout.widget);

        stateManager.addStateChangeListener(onStateChangedListener);
    }

    private void handleAction(Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            if (action.equals(CONTROL_PLAY_PAUSE)) {
                if (stateManager.getState() == PlayerStates.STARTED) {
                    // no need to stop foreground, pause does that
                    pause();
                } else {
                    play();
                }
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

    private boolean handleKeyCode(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                if (stateManager.getState() == PlayerStates.STARTED) {
                    pause();
                    stopForeground(true);
                } else {
                    play();
                }
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
                handleAction(intent);
                int keyCode = intent.getIntExtra(Intent.EXTRA_KEY_EVENT, -1);
                handleKeyCode(keyCode);
                updateGUI();
            }
        }

        return Service.START_STICKY;
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
            String coverPath = book.getCover();

            if (coverPath == null || coverPath.equals("") || !new File(coverPath).exists() || new File(coverPath).isDirectory()) {
                return ImageHelper.genCapital(book.getName(), getApplicationContext(), ImageHelper.TYPE_NOTIFICATION);
            } else {
                return ImageHelper.genBitmapFromFile(coverPath, getApplicationContext(), ImageHelper.TYPE_NOTIFICATION);
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            Intent bookPlayIntent = new Intent(AudioPlayerService.this, BookPlay.class);
            bookPlayIntent.putExtra(GUI_BOOK_ID, book.getId());
            PendingIntent pendingIntent = android.support.v4.app.TaskStackBuilder.create(AudioPlayerService.this)
                    .addNextIntentWithParentStack(bookPlayIntent)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            Notification.Builder builder = new Notification.Builder(AudioPlayerService.this);
            builder.setContentTitle(book.getName())
                    .setContentText(media.getName())
                    .setLargeIcon(result)
                    .setSmallIcon(R.drawable.notification)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setAutoCancel(true);

            int pos = allMedia.indexOf(media);
            if (allMedia.size() > 1 && pos != -1) {
                builder.setContentInfo(String.valueOf(pos + 1) + "/" + String.valueOf(allMedia.size()));
            }

            if (Build.VERSION.SDK_INT >= 16) {
                Intent rewindIntent = new Intent(AudioPlayerService.this, AudioPlayerService.class);
                rewindIntent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.KEYCODE_MEDIA_REWIND);
                PendingIntent rewindPI = PendingIntent.getService(getApplicationContext(), 0, rewindIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                builder.addAction(R.drawable.ic_fast_rewind_grey600_36dp, "", rewindPI);

                Intent pauseIntent = new Intent(AudioPlayerService.this, AudioPlayerService.class);
                pauseIntent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.KEYCODE_MEDIA_PAUSE);
                PendingIntent pausePI = PendingIntent.getService(AudioPlayerService.this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.addAction(R.drawable.ic_pause_grey600_36dp, "", pausePI);

                Intent fastForwardIntent = new Intent(AudioPlayerService.this, AudioPlayerService.class);
                fastForwardIntent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
                PendingIntent fastForwardPI = PendingIntent.getService(AudioPlayerService.this, 2, fastForwardIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.addAction(R.drawable.ic_fast_forward_grey600_36dp, "", fastForwardPI);

                builder.setPriority(NotificationCompat.PRIORITY_HIGH);
            }

            if (Build.VERSION.SDK_INT >= 17) {
                builder.setShowWhen(false);
            }

            if (Build.VERSION.SDK_INT >= 21) {
                builder.setCategory(NotificationCompat.CATEGORY_TRANSPORT);
                builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

                Notification.MediaStyle mediaStyle = new Notification.MediaStyle();
                mediaStyle.setShowActionsInCompactView(0, 1, 2);
                mediaStyle.setMediaSession(mediaSession.getSessionToken());
                builder.setStyle(mediaStyle);
            }

            builder.setWhen(0);

            @SuppressWarnings("deprecation") Notification notification = builder.getNotification();
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopForeground(true);

        if (stateManager.getState() != PlayerStates.DEAD)
            mediaPlayer.reset();
        mediaPlayer.release();
        stateManager.setState(PlayerStates.DEAD);

        unregisterReceiver(audioBecomingNoisyReceiver);
        unregisterReceiver(headsetPlugReceiver);

        //noinspection deprecation
        audioManager.unregisterMediaButtonEventReceiver(myEventReceiver);
        //noinspection deprecation
        audioManager.unregisterRemoteControlClient(mRemoteControlClient);
    }


    /**
     * If audio is becoming noisy, pause the player.
     */
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
                mediaPlayer = new MediaPlayer(AudioPlayerService.this);
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

            String path = media.getPath();

            //setting position to 0 if last file was reached
            if (position == media.getDuration())
                position = 0;

            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            try {
                mediaPlayer.setDataSource(this, Uri.parse(path));
                stateManager.setState(PlayerStates.INITIALIZED);
                mediaPlayer.prepare();
                stateManager.setState(PlayerStates.PREPARED);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (Build.VERSION.SDK_INT >= 21) {
                // metadata
                MediaMetadata.Builder builder = new MediaMetadata.Builder();
                String coverPath = book.getCover();
                Bitmap bitmap;
                if (coverPath == null || !new File(coverPath).exists() || new File(coverPath).isDirectory()) {
                    bitmap = ImageHelper.genCapital(book.getName(), getApplication(), ImageHelper.TYPE_COVER);
                } else {
                    bitmap = ImageHelper.genBitmapFromFile(coverPath, AudioPlayerService.this, ImageHelper.TYPE_COVER);
                }
                builder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, bitmap);
                builder.putLong(MediaMetadata.METADATA_KEY_DURATION, media.getDuration());
                builder.putString(MediaMetadata.METADATA_KEY_TITLE, media.getName());
                builder.putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, book.getName());
                mediaSession.setMetadata(builder.build());
            }
            if (Build.VERSION.SDK_INT < 21) {
                //updates metadata for proper lock screen information
                @SuppressWarnings("deprecation") RemoteControlClient.MetadataEditor editor = mRemoteControlClient.editMetadata(true);
                editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, media.getName());
                editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, book.getName());
                String coverPath = book.getCover();
                Bitmap bitmap;
                if (coverPath == null || coverPath.equals("") || !new File(coverPath).exists() || new File(coverPath).isDirectory()) {
                    bitmap = ImageHelper.genCapital(book.getName(), getApplication(), ImageHelper.TYPE_COVER);
                } else {
                    bitmap = ImageHelper.genBitmapFromFile(coverPath, AudioPlayerService.this, ImageHelper.TYPE_COVER);
                }
                //noinspection deprecation
                editor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, bitmap);
                editor.apply();
            }

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

                    //setting book to last position
                    book.setCurrentMediaPosition(media.getDuration());
                    updateBookAsync(book);

                    boolean wasPlaying = ((stateManager.getState() == PlayerStates.STARTED) ||
                            (stateManager.getState() == PlayerStates.PLAYBACK_COMPLETED));

                    int index = allMedia.indexOf(media);
                    // play next one if there is any
                    if (index < allMedia.size() - 1) {//-1 to prevent change when already last song reached
                        prepare(allMedia.get(index + 1).getId());
                        if (wasPlaying)
                            play();
                        updateGUI();
                    } else { //else unregister as playing
                        playerLock.lock();
                        try {
                            pause();
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
                db.updateBook(book);
            }
        }).run();
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


    /**
     * Pauses the player after specified time.
     *
     * @param sleepTime The amount in ms after the book should pause.
     */
    private void sleepTimer(int sleepTime) {
        Timer sandman = new Timer();
        TimerTask sleepSand = new TimerTask() {
            @Override
            public void run() {
                playerLock.lock();
                try {
                    pause();
                    stopForeground(true);
                    if (BuildConfig.DEBUG) Log.d(TAG, "Good night everyone");
                    if (stateManager.getState() != PlayerStates.DEAD)
                        mediaPlayer.reset();
                    mediaPlayer.release();
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
                int newPosition = position + changeTimeAmount;
                if (newPosition > 0) {
                    if (newPosition > media.getDuration())
                        newPosition = media.getDuration();
                    mediaPlayer.seekTo(newPosition);
                    book.setCurrentMediaPosition(newPosition);
                    updateBookAsync(book);
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
                if (newPosition < 0)
                    newPosition = 0;
                mediaPlayer.seekTo(newPosition);
                book.setCurrentMediaPosition(newPosition);
                updateBookAsync(book);
                stateManager.setTime(mediaPlayer.getCurrentPosition());
            } finally {
                playerLock.unlock();
            }
        }
    }


    /**
     * Changes the current song in book and prepares the media. If the book was playing, it will play
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

                    //starting runner to update gui
                    handler.post(timeChangedRunner);

                    //requesting audio-focus and setting up lock-screen-controls
                    if (audioFocus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        audioFocus = audioManager.requestAudioFocus(audioFocusChangeListener,
                                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                        if (Build.VERSION.SDK_INT < 21) {
                            if (BuildConfig.DEBUG)
                                Log.d(TAG, "Setting up remote control client");
                            //noinspection deprecation
                            audioManager.registerMediaButtonEventReceiver(myEventReceiver);
                            //noinspection deprecation
                            audioManager.registerRemoteControlClient(mRemoteControlClient);
                            //noinspection deprecation
                            mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
                        }
                    }

                    new StartNotificationAsync().execute();
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
    public void pause() {
        if (BuildConfig.DEBUG) Log.d(TAG, "pause called!");

        stopForeground(true);
        if (stateManager.getState() == PlayerStates.STARTED) {
            playerLock.lock();
            try {
                //saves current position, then pauses
                int position = mediaPlayer.getCurrentPosition();
                if (position > 0) {
                    book.setCurrentMediaPosition(position);
                    updateBookAsync(book);
                }

                //stop notification
                stopForeground(false);

                //stops runner who were updating gui frequently
                handler.removeCallbacks(timeChangedRunner);

                //abandon audio-focus and disabling lock-screen controls
                audioManager.abandonAudioFocus(audioFocusChangeListener);
                audioFocus = 0;

                mediaPlayer.pause();
                stateManager.setState(PlayerStates.PAUSED);
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
                            if (BuildConfig.DEBUG)
                                Log.d(TAG, "increasing volume because of regain focus from tcanduck");
                            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
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
                        Log.d(TAG, "lowering volume because of af loss transcient can duck");
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
                    lastState = focusChange;
                    break;
                default:
                    break;
            }
        }
    };
}