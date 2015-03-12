package de.ph1b.audiobook.service;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.TaskStackBuilder;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RemoteViews;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BookPlayActivity;
import de.ph1b.audiobook.activity.BookShelfActivity;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.receiver.BaseWidgetProvider;
import de.ph1b.audiobook.utils.BaseApplication;
import de.ph1b.audiobook.utils.BaseApplication.PlayState;
import de.ph1b.audiobook.utils.L;

public class WidgetUpdateService extends Service implements BaseApplication.OnPositionChangedListener, BaseApplication.OnCurrentBookChangedListener, BaseApplication.OnPlayStateChangedListener {
    private static final String TAG = WidgetUpdateService.class.getSimpleName();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private BaseApplication baseApplication;
    private AppWidgetManager appWidgetManager;

    private void updateWidget() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                L.v(TAG, "updateWidget called");
                boolean isPortrait = isPortrait();
                int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(WidgetUpdateService.this, BaseWidgetProvider.class));
                Book book = baseApplication.getCurrentBook();

                for (int widgetId : ids) {
                    RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.widget);
                    initElements(remoteViews, book);

                    if (Build.VERSION.SDK_INT >= 16) {
                        Bundle opts = appWidgetManager.getAppWidgetOptions(widgetId);
                        int minHeight = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
                        int maxHeight = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
                        int minWidth = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
                        int maxWidth = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);

                        int useWidth;
                        int useHeight;

                        if (isPortrait) {
                            useWidth = minWidth;
                            useHeight = maxHeight;
                        } else {
                            useWidth = maxWidth;
                            useHeight = minHeight;
                        }
                        if (useWidth > 0 && useHeight > 0) {
                            hideElements(remoteViews, useWidth, useHeight);
                        }

                        appWidgetManager.updateAppWidget(widgetId, remoteViews);
                    }
                }
            }

        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        baseApplication = (BaseApplication) getApplication();
        baseApplication.addOnPlayStateChangedListener(this);
        baseApplication.addOnCurrentBookChangedListener(this);
        baseApplication.addOnPositionChangedListener(this);
        appWidgetManager = AppWidgetManager.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWidget();
        return Service.START_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newCfg) {
        int oldOrientation = this.getResources().getConfiguration().orientation;
        int newOrientation = newCfg.orientation;

        if (newOrientation != oldOrientation) {
            updateWidget();
        }
    }

    /**
     * Returning if the current orientation is portrait. If it is unknown, measure the display-spec
     * and return accordingly.
     *
     * @return if the current orientation is portrait
     */
    private boolean isPortrait() {
        int orientation = getResources().getConfiguration().orientation;
        WindowManager window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = window.getDefaultDisplay();

        @SuppressWarnings("deprecation") int displayWidth = display.getWidth();
        @SuppressWarnings("deprecation") int displayHeight = display.getHeight();

        return orientation != Configuration.ORIENTATION_LANDSCAPE && (orientation == Configuration.ORIENTATION_PORTRAIT || displayWidth == displayHeight || displayWidth < displayHeight);
    }

    private int intToDp(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }

    private void hideElements(RemoteViews remoteViews, int width, int coverSize) {
        L.v(TAG, "hideElements called");
        int rewindButtonSize = intToDp(4 + 36 + 4);
        int playButtonSize = intToDp(4 + 48 + 4);
        int fastForwardButtonSize = intToDp(4 + 36 + 4);

        remoteViews.setViewVisibility(R.id.imageView, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.rewind, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.fast_forward, View.VISIBLE);

        if (coverSize + rewindButtonSize + playButtonSize + fastForwardButtonSize < width) {
            return;
        }
        remoteViews.setViewVisibility(R.id.imageView, View.GONE);
        if (rewindButtonSize + playButtonSize + fastForwardButtonSize < width) {
            return;
        }
        remoteViews.setViewVisibility(R.id.fast_forward, View.GONE);
        if (playButtonSize + rewindButtonSize < width) {
            return;
        }
        remoteViews.setViewVisibility(R.id.rewind, View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        baseApplication.removeOnPlayStateChangedListener(this);
        baseApplication.removeOnCurrentBookChangedListener(this);
        baseApplication.removeOnPositionChangedListener(this);
        executor.shutdown();
    }

    private void initElements(RemoteViews remoteViews, Book book) {
        Intent playPauseI = ServiceController.getPlayPauseIntent(this);
        PendingIntent playPausePI = PendingIntent.getService(this, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, playPauseI, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.playPause, playPausePI);

        Intent fastForwardI = ServiceController.getFastForwardIntent(this);
        PendingIntent fastForwardPI = PendingIntent.getService(this, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, fastForwardI, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.fast_forward, fastForwardPI);

        Intent rewindI = ServiceController.getRewindIntent(this);
        PendingIntent rewindPI = PendingIntent.getService(this, KeyEvent.KEYCODE_MEDIA_REWIND, rewindI, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.rewind, rewindPI);

        if (baseApplication.getPlayState() == PlayState.PLAYING) {
            remoteViews.setImageViewResource(R.id.playPause, R.drawable.ic_pause_white_48dp);
        } else {
            remoteViews.setImageViewResource(R.id.playPause, R.drawable.ic_play_arrow_white_48dp);
        }

        // if we have any book, init the views and have a click on the whole widget start BookPlay.
        // if we have no book, simply have a click on the whole widget start BookChoose.
        PendingIntent wholeWidgetClickPI;
        if (book != null) {
            Intent wholeWidgetClickI = new Intent(this, BookPlayActivity.class);
            remoteViews.setTextViewText(R.id.title, book.getName());
            String name = book.getCurrentChapter().getName();

            remoteViews.setTextViewText(R.id.summary, name);

            // building back-stack.
            wholeWidgetClickI.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addParentStack(BookPlayActivity.class);
            stackBuilder.addNextIntent(wholeWidgetClickI);
            wholeWidgetClickPI = stackBuilder.getPendingIntent((int) System.currentTimeMillis(), PendingIntent.FLAG_UPDATE_CURRENT);

            try {
                Bitmap cover = Picasso.with(WidgetUpdateService.this).load(new File(book.getCover())).get();
                remoteViews.setImageViewBitmap(R.id.imageView, cover);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // directly going back to bookChoose
            Intent wholeWidgetClickI = new Intent(this, BookShelfActivity.class);
            wholeWidgetClickPI = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), wholeWidgetClickI, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        remoteViews.setOnClickPendingIntent(R.id.wholeWidget, wholeWidgetClickPI);
    }

    @Override
    public void onPositionChanged() {
        updateWidget();
    }

    @Override
    public void onCurrentBookChanged(Book book) {
        updateWidget();
    }

    @Override
    public void onPlayStateChanged(PlayState state) {
        updateWidget();
    }
}
