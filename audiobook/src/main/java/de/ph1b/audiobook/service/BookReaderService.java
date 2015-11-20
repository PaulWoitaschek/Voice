package de.ph1b.audiobook.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
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
import de.ph1b.audiobook.persistence.BookShelf;
import de.ph1b.audiobook.persistence.PrefsManager;
import de.ph1b.audiobook.uitools.CoverReplacement;
import de.ph1b.audiobook.uitools.ImageHelper;
import de.ph1b.audiobook.utils.App;
import de.ph1b.audiobook.utils.BookVendor;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static com.google.common.base.Preconditions.checkNotNull;


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
            new LinkedBlockingQueue<>(3), // queue capacity
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
    private final CompositeSubscription subscriptions = new CompositeSubscription();
    @Inject PrefsManager prefs;
    @Inject MediaPlayerController controller;
    @Inject BookShelf db;
    @Inject NotificationManager notificationManager;
    @Inject AudioManager audioManager;
    @Inject BookVendor bookVendor;
    private volatile boolean pauseBecauseLossTransient = false;
    private volatile boolean pauseBecauseHeadset = false;
    private final BroadcastReceiver audioBecomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (controller.getPlayState().getValue() == MediaPlayerController.PlayState.PLAYING) {
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

    public BookReaderService() {
        App.getComponent().inject(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mediaSession = new MediaSessionCompat(this, TAG);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                KeyEvent keyEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        && keyEvent.getRepeatCount() == 0) {
                    int keyCode = keyEvent.getKeyCode();
                    Timber.d("onMediaButtonEvent Received command=%s", keyEvent);
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

        Book book = bookVendor.byId(prefs.getCurrentBookId().getValue());
        if (book != null) {
            Timber.d("onCreated initialized book=%s", book);
            reInitController(book);
        }

        subscriptions.add(prefs.getCurrentBookId()
                .map(bookVendor::byId)
                .subscribe(newBook -> {
                    if (newBook != null && (controller.getBook() == null || controller.getBook().id() != newBook.id())) {
                        reInitController(newBook);
                    }
                }));

        subscriptions.add(db.updateObservable()
                .filter(book1 -> book1.id() == prefs.getCurrentBookId().getValue())
                .subscribe(book1 -> {
                    controller.updateBook(book1);
                    notifyChange(ChangeType.METADATA);
                }));

        subscriptions.add(controller.getPlayState().subscribe(playState -> {
            Timber.d("onPlayStateChanged:%s", playState);
            executor.execute(() -> {
                Timber.d("onPlayStateChanged executed:%s", playState);
                Book controllerBook = controller.getBook();
                if (controllerBook != null) {
                    switch (playState) {
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

                    notifyChange(ChangeType.PLAY_STATE);
                }
            });
        }));
    }

    private boolean handleKeyCode(int keyCode) {
        Timber.v("handling keyCode: %s", keyCode);
        boolean handled = false;
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (controller.getPlayState().getValue() == MediaPlayerController.PlayState.PLAYING) {
                    controller.pause(true);
                } else {
                    controller.play();
                }
                handled = true;
                break;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                controller.stop();
                handled = true;
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                controller.skip(MediaPlayerController.Direction.FORWARD);
                handled = true;
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                controller.skip(MediaPlayerController.Direction.BACKWARD);
                handled = true;
                break;
            default:
                return false;
        }
        if (handled)
            notifyChange(ChangeType.PLAYSTATE);
        return handled;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        Timber.v("onStartCommand, intent=%s, flags=%d, startId=%d", intent, flags, startId);
        if (intent != null && intent.getAction() != null) {
            playerExecutor.execute(() -> {
                Timber.v("handling intent action:%s", intent.getAction());
                switch (intent.getAction()) {
                    case Intent.ACTION_MEDIA_BUTTON:
                        MediaButtonReceiver.handleIntent(mediaSession, intent);
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
            });
        }

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Timber.v("onDestroy called");
        controller.stop();
        controller.onDestroy();

        controller.setPlayState(MediaPlayerController.PlayState.STOPPED);

        try {
            unregisterReceiver(audioBecomingNoisyReceiver);
            unregisterReceiver(headsetPlugReceiver);
        } catch (IllegalArgumentException ignored) {
        }

        mediaSession.release();

        subscriptions.unsubscribe();

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
        Timber.d("Call state is: %s", callState);
        if (callState != TelephonyManager.CALL_STATE_IDLE) {
            focusChange = AudioManager.AUDIOFOCUS_LOSS;
            // if there is an incoming call, we pause permanently. (tricking switch condition)
        }
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                Timber.d("started by audioFocus gained");
                if (pauseBecauseLossTransient) {
                    controller.play();
                    pauseBecauseLossTransient = false;
                } else {
                    Timber.d("increasing volume");
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                Timber.d("paused by audioFocus loss");
                controller.stop();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (!prefs.pauseOnTempFocusLoss()) {
                    if (controller.getPlayState().getValue() == MediaPlayerController.PlayState.PLAYING) {
                        Timber.d("lowering volume");
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
                if (controller.getPlayState().getValue() == MediaPlayerController.PlayState.PLAYING) {
                    Timber.d("Paused by audio-focus loss transient.");
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
        if (controller.getPlayState().getValue() == MediaPlayerController.PlayState.PLAYING) {
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

        return notificationBuilder
                .setStyle(new NotificationCompat.MediaStyle()
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
        executor.execute(() -> {
            Timber.d("updateRemoteControlClient called");

            Book book = controller.getBook();
            if (book != null) {
                Chapter c = book.currentChapter();
                MediaPlayerController.PlayState playState = controller.getPlayState().getValue();

                String bookName = book.name();
                String chapterName = c.name();
                String author = book.author();
                int position = book.time();

                sendBroadcast(what.broadcastIntent(author, bookName, chapterName, playState, position));

                //noinspection ResourceType
                playbackStateBuilder.setState(playState.playbackStateCompat(), position, controller.getPlaybackSpeed());
                mediaSession.setPlaybackState(playbackStateBuilder.build());

                if (what == ChangeType.METADATA && !lastFileForMetaData.equals(book.currentFile())) {
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
                        Timber.d("replacement dimen: %d:%d", replacement.getIntrinsicWidth(), replacement.getIntrinsicHeight());
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
        });
    }

    private enum ChangeType {
        METADATA("com.android.music.metachanged"),
        PLAY_STATE("com.android.music.playstatechange");

        private final String intentUrl;

        ChangeType(String intentUrl) {
            this.intentUrl = intentUrl;
        }

        public Intent broadcastIntent(@Nullable String author,
                                      @NonNull String bookName,
                                      @NonNull String chapterName,
                                      @NonNull MediaPlayerController.PlayState playState,
                                      int time) {
            checkNotNull(bookName);
            checkNotNull(chapterName);
            checkNotNull(playState);

            Intent i = new Intent(intentUrl);
            i.putExtra("id", 1);
            if (author != null) {
                i.putExtra("artist", author);
            }
            i.putExtra("album", bookName);
            i.putExtra("track", chapterName);
            i.putExtra("playing", playState == MediaPlayerController.PlayState.PLAYING);
            i.putExtra("position", time);
            return i;
        }
    }
}
