package de.ph1b.audiobook.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BookActivity;
import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Chapter;
import de.ph1b.audiobook.persistence.DataBaseHelper;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.receiver.RemoteControlReceiver;
import de.ph1b.audiobook.uitools.CoverReplacement;
import de.ph1b.audiobook.uitools.ImageHelper;
import de.ph1b.audiobook.utils.App;
import de.ph1b.audiobook.utils.Communication;
import de.ph1b.audiobook.utils.L;


/**
 * Service that hosts the longtime playback and handles its controls.
 *
 * @author Paul Woitaschek
 */
public class BookReaderService extends Service implements AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = BookReaderService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 42;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ExecutorService playerExecutor = new ThreadPoolExecutor(
            1, 1, // single thread
            2, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(3), // queue capacity
            new ThreadPoolExecutor.DiscardOldestPolicy()
    );
    private final PlaybackStateCompat.Builder playbackStateBuilder = new PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    | PlaybackStateCompat.ACTION_REWIND
                    | PlaybackStateCompat.ACTION_PLAY
                    | PlaybackStateCompat.ACTION_PAUSE
                    | PlaybackStateCompat.ACTION_STOP
                    | PlaybackStateCompat.ACTION_FAST_FORWARD
                    | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    | PlaybackStateCompat.ACTION_SEEK_TO);
    private final MediaMetadataCompat.Builder mediaMetaDataBuilder = new MediaMetadataCompat.Builder();
    @Inject Communication communication;
    @Inject PrefsManager prefs;
    @Inject MediaPlayerController controller;
    @Inject DataBaseHelper db;
    private NotificationManager notificationManager;
    private AudioManager audioManager;
    private volatile boolean pauseBecauseLossTransient = false;
    private volatile boolean pauseBecauseHeadset = false;
    private final BroadcastReceiver audioBecomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (controller.getPlayState() == MediaPlayerController.PlayState.PLAYING) {
                pauseBecauseHeadset = true;
                controller.pause(true);
            }
        }
    };
    private final BroadcastReceiver headsetPlugReceiver = new BroadcastReceiver() {
        private static final int PLUGGED = 1;
        private static final int UNPLUGGED = 0;

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(Intent.ACTION_HEADSET_PLUG)) {
                if (intent.getIntExtra("state", UNPLUGGED) == PLUGGED) {
                    if (pauseBecauseHeadset) {
                        if (prefs.resumeOnReplug()) {
                            controller.play();
                        }
                        pauseBecauseHeadset = false;
                    }
                }
            }
        }
    };
    private MediaSessionCompat mediaSession;
    /**
     * The last file the {@link #notifyChange(BookReaderService.ChangeType)} has used to update the metadata.
     */
    private volatile File lastFileForMetaData = new File("");
    private final Communication.SimpleBookCommunication listener = new Communication.SimpleBookCommunication() {


        @Override
        public void onBookContentChanged(@NonNull Book book) {
            if (book.id() == prefs.getCurrentBookId()) {
                controller.updateBook(db.getBook(prefs.getCurrentBookId()));
                notifyChange(ChangeType.METADATA);
            }
        }

        @Override
        public void onPlayStateChanged() {
            final MediaPlayerController.PlayState state = controller.getPlayState();
            L.d(TAG, "onPlayStateChanged:" + state);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    L.d(TAG, "onPlayStateChanged executed:" + state);
                    Book controllerBook = controller.getBook();
                    if (controllerBook != null) {
                        switch (state) {
                            case PLAYING:
                                audioManager.requestAudioFocus(BookReaderService.this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

                                mediaSession.setActive(true);

                                startForeground(NOTIFICATION_ID, getNotification(controllerBook));

                                break;
                            case PAUSED:
                                stopForeground(false);
                                notificationManager.notify(NOTIFICATION_ID, getNotification(controllerBook));

                                break;
                            case STOPPED:
                                mediaSession.setActive(false);

                                audioManager.abandonAudioFocus(BookReaderService.this);
                                notificationManager.cancel(NOTIFICATION_ID);
                                stopForeground(true);

                                break;
                        }

                        notifyChange(ChangeType.PLAYSTATE);
                    }
                }
            });
        }

        @Override
        public void onCurrentBookIdChanged(long oldId) {
            Book book = db.getBook(prefs.getCurrentBookId());
            if (book != null && (controller.getBook() == null || controller.getBook().id() != book.id())) {
                reInitController(book);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        App.getComponent().inject(this);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        ComponentName eventReceiver = new ComponentName(BookReaderService.this.getPackageName(), RemoteControlReceiver.class.getName());
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(eventReceiver);
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0);
        mediaSession = new MediaSessionCompat(this, TAG, eventReceiver, mediaPendingIntent);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                KeyEvent keyEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        && keyEvent.getRepeatCount() == 0) {
                    int keyCode = keyEvent.getKeyCode();
                    L.d(TAG, "onMediaButtonEvent Received command=" + keyEvent);
                    return handleKeyCode(keyCode);
                } else {
                    return super.onMediaButtonEvent(mediaButtonEvent);
                }
            }
        });
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        registerReceiver(audioBecomingNoisyReceiver, new IntentFilter(
                AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        registerReceiver(headsetPlugReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

        controller.setPlayState(MediaPlayerController.PlayState.STOPPED);

        communication.addBookCommunicationListener(listener);

        Book book = db.getBook(prefs.getCurrentBookId());
        if (book != null) {
            L.d(TAG, "onCreated initialized book=" + book);
            reInitController(book);
        }
    }

    private boolean handleKeyCode(int keyCode) {
        L.v(TAG, "handling keyCode: " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (controller.getPlayState() == MediaPlayerController.PlayState.PLAYING) {
                    controller.pause(true);
                } else {
                    controller.play();
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                controller.stop();
                return true;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                controller.skip(MediaPlayerController.Direction.FORWARD);
                return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                controller.skip(MediaPlayerController.Direction.BACKWARD);
                return true;
            default:
                return false;
        }
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        L.v(TAG, "onStartCommand,intent=" + intent + ", flags=" + flags + ", startId=" + startId);
        if (intent != null && intent.getAction() != null) {
            playerExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    L.v(TAG, "handling intent action:" + intent.getAction());
                    switch (intent.getAction()) {
                        case Intent.ACTION_MEDIA_BUTTON:
                            KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                            if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                                    && keyEvent.getRepeatCount() == 0) {
                                int keyCode = keyEvent.getKeyCode();
                                handleKeyCode(keyCode);
                            }
                            break;
                        case ServiceController.CONTROL_SET_PLAYBACK_SPEED:
                            float speed = intent.getFloatExtra(ServiceController.CONTROL_SET_PLAYBACK_SPEED_EXTRA_SPEED, 1);
                            controller.setPlaybackSpeed(speed);
                            break;
                        case ServiceController.CONTROL_TOGGLE_SLEEP_SAND:
                            controller.toggleSleepSand();
                            break;
                        case ServiceController.CONTROL_CHANGE_POSITION:
                            int newTime = intent.getIntExtra(ServiceController.CONTROL_CHANGE_POSITION_EXTRA_TIME, 0);
                            File file = (File) intent.getSerializableExtra(ServiceController.CONTROL_CHANGE_POSITION_EXTRA_FILE);
                            controller.changePosition(newTime, file);
                            break;
                        case ServiceController.CONTROL_NEXT:
                            controller.next();
                            break;
                        case ServiceController.CONTROL_PREVIOUS:
                            controller.previous(true);
                            break;
                        default:
                            break;
                    }
                }
            });
        }

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        L.v(TAG, "onDestroy called");
        controller.stop();
        controller.onDestroy();

        communication.removeBookCommunicationListener(listener);

        controller.setPlayState(MediaPlayerController.PlayState.STOPPED);

        try {
            unregisterReceiver(audioBecomingNoisyReceiver);
            unregisterReceiver(headsetPlugReceiver);
        } catch (IllegalArgumentException ignored) {
        }

        mediaSession.release();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void reInitController(@NonNull Book book) {
        controller.stop();
        controller.init(book);

        pauseBecauseHeadset = false;
        pauseBecauseLossTransient = false;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        final int callState = (tm != null) ? tm.getCallState() : TelephonyManager.CALL_STATE_IDLE;
        L.d(TAG, "Call state is: " + callState);
        if (callState != TelephonyManager.CALL_STATE_IDLE) {
            focusChange = AudioManager.AUDIOFOCUS_LOSS;
            // if there is an incoming call, we pause permanently. (tricking switch condition)
        }
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                L.d(TAG, "started by audioFocus gained");
                if (pauseBecauseLossTransient) {
                    controller.play();
                    pauseBecauseLossTransient = false;
                } else {
                    L.d(TAG, "increasing volume");
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                L.d(TAG, "paused by audioFocus loss");
                controller.stop();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (!prefs.pauseOnTempFocusLoss()) {
                    if (controller.getPlayState() == MediaPlayerController.PlayState.PLAYING) {
                        L.d(TAG, "lowering volume");
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
                        pauseBecauseHeadset = false;
                    }

                    /*
                      Only break here. if we should pause, AUDIO_FOCUS_LOSS_TRANSIENT will handle
                      that for us.
                     */
                    break;
                }
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (controller.getPlayState() == MediaPlayerController.PlayState.PLAYING) {
                    L.d(TAG, "Paused by audio-focus loss transient.");
                    // Only rewind if loss is transient. When we only pause temporary, don't rewind
                    // automatically.
                    controller.pause(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT);
                    pauseBecauseLossTransient = true;
                }
                break;
            default:
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private Notification getNotification(@NonNull Book book) {

        // cover
        int width = ImageHelper.getSmallerScreenSize(this);
        int height = ImageHelper.getSmallerScreenSize(this);
        Bitmap cover = null;
        try {
            File coverFile = book.coverFile();
            if (!book.useCoverReplacement() && coverFile.exists() && coverFile.canRead()) {
                cover = Picasso.with(BookReaderService.this).load(coverFile).resize(width, height).get();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (cover == null) {
            cover = ImageHelper.drawableToBitmap(new CoverReplacement(
                    book.name(),
                    this), width, height);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        Chapter chapter = book.currentChapter();

        List<Chapter> chapters = book.chapters();
        if (chapters.size() > 1) {
            // we need the current chapter title and number only if there is more than one chapter.
            notificationBuilder.setContentInfo((chapters.indexOf(chapter) + 1) + "/" +
                    chapters.size());
            notificationBuilder.setContentText(chapter.name());
        }

        // rewind
        Intent rewindIntent = ServiceController.getRewindIntent(this);
        PendingIntent rewindPI = PendingIntent.getService(getApplicationContext(), KeyEvent.KEYCODE_MEDIA_REWIND, rewindIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.addAction(R.drawable.ic_fast_rewind, getString(R.string.rewind), rewindPI);

        // play/pause
        Intent playPauseIntent = ServiceController.getPlayPauseIntent(this);
        PendingIntent playPausePI = PendingIntent.getService(this, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (controller.getPlayState() == MediaPlayerController.PlayState.PLAYING) {
            notificationBuilder.addAction(R.drawable.ic_pause, getString(R.string.pause), playPausePI);
        } else {
            notificationBuilder.addAction(R.drawable.ic_play_arrow, getString(R.string.play), playPausePI);
        }

        // fast forward
        Intent fastForwardIntent = ServiceController.getFastForwardIntent(this);
        PendingIntent fastForwardPI = PendingIntent.getService(getApplicationContext(), KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, fastForwardIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.addAction(R.drawable.ic_fast_forward, getString(R.string.fast_forward), fastForwardPI);

        // stop intent
        Intent stopIntent = ServiceController.getStopIntent(this);
        PendingIntent stopPI = PendingIntent.getService(this, KeyEvent.KEYCODE_MEDIA_STOP, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // content click
        Intent contentIntent = BookActivity.goToBookIntent(this, book.id());
        PendingIntent contentPI = PendingIntent.getActivity(this, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return notificationBuilder.setStyle(
                new NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1)
                        .setCancelButtonIntent(stopPI)
                        .setShowCancelButton(true)
                        .setMediaSession(mediaSession.getSessionToken()))
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentPI)
                .setContentTitle(book.name())
                .setSmallIcon(R.drawable.ic_notification)
                .setWhen(0)
                .setDeleteIntent(stopPI)
                .setAutoCancel(true)
                .setLargeIcon(cover)
                .build();
    }

    private void notifyChange(final ChangeType what) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                L.d(TAG, "updateRemoteControlClient called");

                Book book = controller.getBook();
                if (book != null) {
                    Chapter c = book.currentChapter();
                    MediaPlayerController.PlayState playState = controller.getPlayState();

                    String bookName = book.name();
                    String chapterName = c.name();
                    String author = book.author();
                    int position = book.time();

                    Intent i = new Intent(what.intentUrl);
                    i.putExtra("id", 1);
                    if (author != null) {
                        i.putExtra("artist", author);
                    }
                    i.putExtra("album", bookName);
                    i.putExtra("track", chapterName);
                    i.putExtra("playing", playState == MediaPlayerController.PlayState.PLAYING);
                    i.putExtra("position", book.time());
                    sendBroadcast(i);

                    if (what == ChangeType.PLAYSTATE) {
                        int state;
                        if (playState == MediaPlayerController.PlayState.PLAYING) {
                            state = PlaybackStateCompat.STATE_PLAYING;
                        } else if (playState == MediaPlayerController.PlayState.PAUSED) {
                            state = PlaybackStateCompat.STATE_PAUSED;
                        } else {
                            state = PlaybackStateCompat.STATE_STOPPED;
                        }
                        playbackStateBuilder.setState(state, position, controller.getPlaybackSpeed());
                        mediaSession.setPlaybackState(playbackStateBuilder.build());
                    } else if ((what == ChangeType.METADATA) && !lastFileForMetaData.equals(book.currentFile())) {
                        // this check is necessary. Else the lockscreen controls will flicker due to
                        // an updated picture
                        Bitmap bitmap = null;
                        File coverFile = book.coverFile();
                        if (!book.useCoverReplacement() && coverFile.exists() && coverFile.canRead()) {
                            try {
                                bitmap = Picasso.with(BookReaderService.this).load(coverFile).get();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (bitmap == null) {
                            Drawable replacement = new CoverReplacement(
                                    book.name(),
                                    BookReaderService.this);
                            L.d(TAG, "replacement dimen: " + replacement.getIntrinsicWidth() + ":" + replacement.getIntrinsicHeight());
                            bitmap = ImageHelper.drawableToBitmap(
                                    replacement,
                                    ImageHelper.getSmallerScreenSize(BookReaderService.this),
                                    ImageHelper.getSmallerScreenSize(BookReaderService.this));
                        }
                        // we make a copy because we do not want to use picassos bitmap, since
                        // MediaSessionCompat recycles our bitmap eventually which would make
                        // picassos cached bitmap useless.
                        bitmap = bitmap.copy(bitmap.getConfig(), true);
                        mediaMetaDataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
                                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, c.duration())
                                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, (book.chapters().indexOf(book.currentChapter()) + 1))
                                .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, book.chapters().size())
                                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, chapterName)
                                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, bookName)
                                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, author)
                                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, author)
                                .putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, author)
                                .putString(MediaMetadataCompat.METADATA_KEY_COMPOSER, author)
                                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "Audiobook");
                        mediaSession.setMetadata(mediaMetaDataBuilder.build());

                        lastFileForMetaData = book.currentFile();
                    }
                }
            }
        });
    }

    private enum ChangeType {
        METADATA("com.android.music.metachanged"),
        PLAYSTATE("com.android.music.playstatechange");

        public final String intentUrl;

        ChangeType(String intentUrl) {
            this.intentUrl = intentUrl;
        }
    }
}
