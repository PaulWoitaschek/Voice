package de.ph1b.audiobook.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.MediaPlay;
import de.ph1b.audiobook.helper.BookDetail;
import de.ph1b.audiobook.helper.DataBaseHelper;
import de.ph1b.audiobook.helper.MediaDetail;

public class AudioPlayerService extends Service {


    public static final String BOOK_ID = "de.ph1b.audiobook.BOOK_ID";
    private static final int NOTIFICATION_ID = 1;

    private static BookDetail book;
    private static int bookId;

    private DataBaseHelper db;
    private LocalBroadcastManager bcm;

    //indicates if service is running for MediaPlay to check on resume
    //public static boolean serviceRunning = true;

    private static final String TAG = "AudioPlayerService";

    public static final String CONTROL_PLAY_PAUSE = "de.ph1b.audiobook.CONTROL_PLAY_PAUSE";
    public static final String CONTROL_FORWARD = "de.ph1b.audiobook.CONTROL_FORWARD";
    public static final String CONTROL_FAST_FORWARD = "de.ph1b.audiobook.CONTROL_FAST_FORWARD";
    public static final String CONTROL_PREVIOUS = "de.ph1b.audiobook.CONTROL_PREVIOUS";
    public static final String CONTROL_REWIND = "de.ph1b.audiobook.CONTROL_REWIND";
    public static final String CONTROL_CHANGE_MEDIA_POSITION = "de.ph1b.audiobook.CONTROL_CHANGE_MEDIA_POSITION";
    public static final String CONTROL_SETUP_NOTIFICATION = "de.ph1b.audiobook.CONTROL_SETUP_NOTIFICATION";
    public static final String CONTROL_SLEEP = "de.ph1b.audiobook.SLEEP_TIME";
    public static final String CONTROL_POKE_UPDATE = "de.ph1b.audiobook.CONTROL_POKE_UPDATE";
    public static final String CONTROL_CHANGE_BOOK_POSITION = "de.ph1b.audiobook.CONTROL_CHANGE_BOOK_POSITION";


    private static PlaybackService playbackService;

    private final String NOTIFICATION_PAUSE = "de.ph1b.audiobook.NOTIFICATION_PAUSE";

    @Override
    public void onCreate() {
        super.onCreate();

        bcm = LocalBroadcastManager.getInstance(this);
        db = DataBaseHelper.getInstance(this);

        //registering receiver for controlling player
        IntentFilter filter = new IntentFilter();
        filter.addAction(CONTROL_PLAY_PAUSE);
        filter.addAction(CONTROL_FORWARD);
        filter.addAction(CONTROL_FAST_FORWARD);
        filter.addAction(CONTROL_PREVIOUS);
        filter.addAction(CONTROL_CHANGE_MEDIA_POSITION);
        filter.addAction(CONTROL_REWIND);
        filter.addAction(CONTROL_SETUP_NOTIFICATION);
        filter.addAction(CONTROL_SLEEP);
        filter.addAction(CONTROL_POKE_UPDATE);
        filter.addAction(CONTROL_CHANGE_BOOK_POSITION);
        bcm.registerReceiver(controlReceiver, filter);

        //registering receiver for pause from notification
        registerReceiver(notificationPauseReceiver, new IntentFilter(NOTIFICATION_PAUSE));
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (BuildConfig.DEBUG)
            Log.d(TAG, "onStartCommand was called");

        if (playbackService == null) {
            playbackService = new PlaybackService(this, intent);
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Started new playback service");
        }

        if (intent != null && intent.hasExtra(BOOK_ID)) {
            int newBookId = intent.getIntExtra(BOOK_ID, 0);
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Old bookId: " + bookId + ", new bookId: " + newBookId + ", state: " + StateManager.getState());

            if (newBookId != bookId) {
                bookId = newBookId;
                book = db.getBook(bookId);
                if (book != null) {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "book with id is != null:" + bookId);
                    MediaDetail[] allMedia = db.getMediaFromBook(bookId);
                    playbackService.initBook(book, allMedia);
                    playbackService.prepare(book.getPosition());
                } else if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Book of bookId " + bookId + " is null");
                }
            }
        }

        playbackService.updateGUI();

        //sticky to re-init when stopped
        return Service.START_REDELIVER_INTENT;
    }


    private final BroadcastReceiver notificationPauseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playbackService.pause();
        }
    };


    private final BroadcastReceiver controlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(CONTROL_PLAY_PAUSE)) {
                switch (StateManager.getState()) {
                    case PREPARED:
                    case PAUSED:
                        playbackService.play();
                        break;
                    case STARTED:
                        playbackService.pause();
                        break;
                    default:
                        break;
                }
            }
            if (action.equals(CONTROL_FORWARD)) {
                playbackService.nextSong();
            }
            if (action.equals(CONTROL_FAST_FORWARD)) {
                playbackService.fastForward();
            }
            if (action.equals(CONTROL_PREVIOUS)) {
                playbackService.previousSong();
            }
            if (action.equals(CONTROL_REWIND)) {
                playbackService.rewind();
            }
            if (action.equals(CONTROL_SETUP_NOTIFICATION)) {
                boolean setupNotification = intent.getBooleanExtra(CONTROL_SETUP_NOTIFICATION, false);
                if (setupNotification) {
                    foreground(true);
                } else {
                    foreground(false);
                }
            }
            if (action.equals(CONTROL_CHANGE_MEDIA_POSITION)) {
                int position = intent.getIntExtra(CONTROL_CHANGE_MEDIA_POSITION, -1);
                if (position != -1)
                    playbackService.changePosition(position);
            }
            if (action.equals(CONTROL_SLEEP)) {
                int sleepTime = intent.getIntExtra(CONTROL_SLEEP, 0);
                playbackService.sleepTimer(sleepTime);
            }
            if (action.equals(CONTROL_POKE_UPDATE)) {
                playbackService.updateGUI();
            }

            if (action.equals(CONTROL_CHANGE_BOOK_POSITION)) {
                int position = intent.getIntExtra(CONTROL_CHANGE_BOOK_POSITION, -1);
                if (position != -1)
                    playbackService.changeBookPosition(position);
            }

        }
    };


    private void foreground(Boolean set) {
        if (set) {
            new StartNotificationAsync().execute();
        } else {
            stopForeground(true);
        }
    }


    private class StartNotificationAsync extends AsyncTask<Void, Void, Void> {

        Bitmap thumb = null;
        final PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), MediaPlay.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        @Override
        protected Void doInBackground(Void... params) {
            String thumbPath = book.getThumb();

            Resources res = getApplicationContext().getResources();
            int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
            int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);

            if (thumbPath.equals("") || new File(thumbPath).isDirectory()) {
                thumb = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                Canvas c = new Canvas(thumb);
                Paint textPaint = new Paint();
                textPaint.setTextSize(2 * width / 3);
                textPaint.setColor(getResources().getColor(android.R.color.white));
                textPaint.setAntiAlias(true);
                textPaint.setTextAlign(Paint.Align.CENTER);
                Paint backgroundPaint = new Paint();
                backgroundPaint.setColor(getResources().getColor(R.color.file_chooser_audio));
                c.drawRect(0, 0, width, height, backgroundPaint);
                int y = (int) ((c.getHeight() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2));
                c.drawText(book.getName().substring(0, 1).toUpperCase(), width / 2, y, textPaint);
            } else {
                thumb = BitmapFactory.decodeFile(thumbPath);
                thumb = Bitmap.createScaledBitmap(thumb, width, height, false);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Intent i = new Intent(NOTIFICATION_PAUSE);
            PendingIntent pauseActionPI = PendingIntent.getBroadcast(getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification notification = new NotificationCompat.Builder(getApplicationContext())
                    .setContentTitle(book.getName())
                    .setLargeIcon(thumb)
                    .setSmallIcon(R.drawable.av_play_dark)
                    .setContentIntent(pi)
                    .addAction(R.drawable.av_pause_dark, "Pause", pauseActionPI)
                    .setAutoCancel(true)
                    .build();
            startForeground(NOTIFICATION_ID, notification);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(notificationPauseReceiver);
        bcm.unregisterReceiver(controlReceiver);
        playbackService.finish();
        playbackService = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}