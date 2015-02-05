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
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.widget.RemoteViews;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BookPlay;
import de.ph1b.audiobook.content.Book;
import de.ph1b.audiobook.content.DataBaseHelper;
import de.ph1b.audiobook.content.Media;
import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.receiver.RemoteControlReceiver;
import de.ph1b.audiobook.utils.ImageHelper;
import de.ph1b.audiobook.utils.L;
import de.ph1b.audiobook.utils.Prefs;

public class AudioPlayerService extends Service implements StateManager.ChangeListener, AudioManager.OnAudioFocusChangeListener {


    private static final String TAG = "AudioPlayerService";
    private static final int NOTIFICATION_ID = 42;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private NotificationManager notificationManager;
    private Prefs prefs;
    private MediaPlayerController controller = null;
    private DataBaseHelper db;
    private AudioManager audioManager;
    private StateManager stateManager;
    private NotificationCompat.Builder notificationBuilder;
    @SuppressWarnings("deprecation")
    private RemoteControlClient remoteControlClient = null;
    private volatile boolean pauseBecauseLossTransient = false;
    private volatile boolean pauseBecauseHeadset = false;

    private final BroadcastReceiver audioBecomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (stateManager.getState() == PlayerStates.PLAYING) {
                pauseBecauseHeadset = true;
                controller.pause();
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

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onCreate() {
        super.onCreate();

        prefs = new Prefs(this);
        db = DataBaseHelper.getInstance(this);
        notificationBuilder = new NotificationCompat.Builder(this);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        stateManager = StateManager.getInstance(this);

        if (Build.VERSION.SDK_INT >= 14) {
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

        registerReceiver(audioBecomingNoisyReceiver, new IntentFilter(
                AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        registerReceiver(headsetPlugReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

        stateManager.addChangeListener(this);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onDestroy() {
        super.onDestroy();

        stateManager.removeChangeListener(this);

        unregisterReceiver(audioBecomingNoisyReceiver);
        unregisterReceiver(headsetPlugReceiver);

        if (Build.VERSION.SDK_INT >= 14) {
            //noinspection deprecation
            audioManager.unregisterRemoteControlClient(remoteControlClient);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleIntent(final Intent intent) {
        if (intent != null && intent.getAction() != null) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    L.v(TAG, "handling intent action:" + intent.getAction());
                    switch (intent.getAction()) {
                        case Intent.ACTION_MEDIA_BUTTON:
                            int keyCode = intent.getIntExtra(Intent.EXTRA_KEY_EVENT, -1);
                            L.v(TAG, "handling keyCode: " + keyCode);
                            switch (keyCode) {
                                case KeyEvent.KEYCODE_MEDIA_PLAY:
                                    controller.play();
                                    break;
                                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                                    controller.pause();
                                    break;
                                case KeyEvent.KEYCODE_HEADSETHOOK:
                                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                                    if (stateManager.getState() == PlayerStates.PLAYING) {
                                        controller.pause();
                                    } else {
                                        controller.play();
                                    }
                                    break;
                                case KeyEvent.KEYCODE_MEDIA_STOP:
                                    controller.release();
                                    break;
                                case KeyEvent.KEYCODE_MEDIA_NEXT:
                                    controller.next();
                                    break;
                                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                                    controller.skip(MediaPlayerController.Direction.FORWARD);
                                    break;
                                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                                    controller.previous();
                                    break;
                                case KeyEvent.KEYCODE_MEDIA_REWIND:
                                    controller.skip(MediaPlayerController.Direction.BACKWARD);
                                    break;
                            }
                            break;
                        case ServiceController.CONTROL_SET_PLAYBACK_SPEED:
                            float speed = intent.getFloatExtra(ServiceController.CONTROL_SET_PLAYBACK_SPEED_EXTRA_SPEED, 1);
                            controller.setPlaybackSpeed(speed);
                            break;
                        case ServiceController.CONTROL_CHANGE_BOOK_POSITION:
                            int position = intent.getIntExtra(ServiceController.CONTROL_CHANGE_BOOK_POSITION_EXTRA_MEDIA_POSITION, 0);
                            int time = intent.getIntExtra(ServiceController.CONTROL_CHANGE_BOOK_POSITION_EXTRA_MEDIA_TIME, 0);
                            controller.changeBookPosition(position);
                            if (time != 0) {
                                controller.changeTime(time);
                            }
                            break;
                        case ServiceController.CONTROL_TOGGLE_SLEEP_SAND:
                            controller.toggleSleepSand();
                            break;
                        case ServiceController.CONTROL_CHANGE_TIME:
                            int time1 = intent.getIntExtra(ServiceController.CONTROL_CHANGE_TIME_EXTRA, 0);
                            controller.changeTime(time1);
                            break;
                        default:
                            break;
                    }
                }
            });
        }
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        long defaultBookId = prefs.getCurrentBookId();
        L.d(TAG, "onStartCommand with defaultBookId: " + defaultBookId);

        if (defaultBookId == -1) {
            stopSelf();
            return Service.START_NOT_STICKY;
        }

        if (controller == null || defaultBookId != controller.getBook().getId()) {
            if (controller != null) {
                controller.release();
                pauseBecauseLossTransient = false;
            }
            Book book = db.getBook(defaultBookId);
            L.d(TAG, "init new book:" + book.getName());
            controller = new MediaPlayerController(book, this);
        }

        handleIntent(intent);

        return Service.START_STICKY;
    }

    @Override
    public void onTimeChanged(int time) {

    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onStateChanged(PlayerStates state) {
        if (controller != null) {
            switch (state) {
                case PLAYING:
                    audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

                    startForeground(NOTIFICATION_ID, getNotification());

                    if (Build.VERSION.SDK_INT >= 14) {
                        //noinspection deprecation
                        remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
                        updateRemoteControlClient();
                    }
                    break;
                case PAUSED:
                    // audioManager.abandonAudioFocus(this);
                    notificationManager.notify(NOTIFICATION_ID, getNotification());
                    stopForeground(false);
                    if (Build.VERSION.SDK_INT >= 14) {
                        //noinspection deprecation
                        remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
                    }
                    break;
                case STOPPED:
                    audioManager.abandonAudioFocus(this);
                    notificationManager.cancel(NOTIFICATION_ID);
                    stopForeground(true);
                    if (Build.VERSION.SDK_INT >= 14) {
                        //noinspection deprecation
                        remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
                    }
                    break;
            }
        }
    }

    private Notification getNotification() {
        Book book = controller.getBook();
        Media media = book.getContainingMedia().get(book.getPosition());

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

        Intent rewindIntent = ServiceController.getRewindIntent(this);
        PendingIntent rewindPI = PendingIntent.getService(getApplicationContext(), KeyEvent.KEYCODE_MEDIA_REWIND, rewindIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent playPauseIntent = ServiceController.getPlayPauseIntent(this);
        if (stateManager.getState() == PlayerStates.PLAYING) {
            smallViewRemote.setImageViewResource(R.id.playPause, R.drawable.ic_pause_white_48dp);
            bigViewRemote.setImageViewResource(R.id.playPause, R.drawable.ic_pause_white_48dp);
        } else {
            smallViewRemote.setImageViewResource(R.id.playPause, R.drawable.ic_play_arrow_white_48dp);
            bigViewRemote.setImageViewResource(R.id.playPause, R.drawable.ic_play_arrow_white_48dp);
        }
        PendingIntent playPausePI = PendingIntent.getService(this, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent fastForwardIntent = ServiceController.getFastForwardIntent(this);
        PendingIntent fastForwardPI = PendingIntent.getService(AudioPlayerService.this, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, fastForwardIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent stopIntent = ServiceController.getStopIntent(this);
        PendingIntent stopPI = PendingIntent.getService(this, KeyEvent.KEYCODE_MEDIA_STOP, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

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
        PendingIntent pendingIntent = android.support.v4.app.TaskStackBuilder.create(AudioPlayerService.this)
                .addNextIntentWithParentStack(bookPlayIntent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContent(smallViewRemote)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        Notification notification = notificationBuilder.build();

        if (Build.VERSION.SDK_INT >= 16) {
            notification.bigContentView = bigViewRemote;
        }

        return notification;
    }

    @Override
    public void onSleepTimerSet(boolean sleepTimerActive) {

    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void updateRemoteControlClient() {
        Book book = controller.getBook();

        String coverPath = book.getCover();
        Bitmap bitmap;
        if (coverPath == null || !new File(coverPath).exists() || new File(coverPath).isDirectory()) {
            bitmap = ImageHelper.genCapital(book.getName(), getApplication(), ImageHelper.TYPE_COVER);
        } else {
            bitmap = ImageHelper.genBitmapFromFile(coverPath, AudioPlayerService.this, ImageHelper.TYPE_COVER);
        }
        @SuppressWarnings("deprecation") RemoteControlClient.MetadataEditor editor = remoteControlClient.editMetadata(true);
        editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, book.getContainingMedia().get(book.getPosition()).getName());
        editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, book.getName());
        //noinspection deprecation
        editor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, bitmap);
        editor.apply();
    }

    @Override
    public void onPositionChanged(int position) {
        if (controller != null) {
            if (Build.VERSION.SDK_INT >= 14) {
                updateRemoteControlClient();
            }
        }
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
                } else if (stateManager.getState() == PlayerStates.PLAYING) {
                    L.d(TAG, "increasing volume because of regain focus from transient-can-duck");
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                L.d(TAG, "paused by audioFocus loss");
                controller.release();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (stateManager.getState() == PlayerStates.PLAYING) {
                    L.d(TAG, "Paused by audio-focus loss transient.");
                    controller.pause();
                    pauseBecauseLossTransient = true;
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (stateManager.getState() == PlayerStates.PLAYING) {
                    if (prefs.pauseOnTransientAudioFocusLoss()) {
                        L.d(TAG, "pausing because of transient loss");
                        controller.pause();
                        pauseBecauseLossTransient = true;
                    } else {
                        L.d(TAG, "lowering volume because of af loss transient can duck");
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
                        pauseBecauseLossTransient = false;
                    }
                }
                break;
            default:
                break;
        }
    }
}