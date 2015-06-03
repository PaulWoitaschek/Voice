package de.ph1b.audiobook.service;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
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
import de.ph1b.audiobook.activity.BookActivity;
import de.ph1b.audiobook.fragment.BookPlayFragment;
import de.ph1b.audiobook.mediaplayer.MediaPlayerController;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.DataBaseHelper;
import de.ph1b.audiobook.receiver.BaseWidgetProvider;
import de.ph1b.audiobook.uitools.CoverReplacement;
import de.ph1b.audiobook.uitools.ImageHelper;
import de.ph1b.audiobook.utils.Communication;
import de.ph1b.audiobook.utils.PrefsManager;

public class WidgetUpdateService extends Service {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private DataBaseHelper db;
    private PrefsManager prefs;
    private final BroadcastReceiver onCurrentBookChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWidget();
        }
    };
    private final BroadcastReceiver onBookSetChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWidget();
        }
    };
    private final BroadcastReceiver onPlayStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWidget();
        }
    };
    private LocalBroadcastManager bcm;

    @Override
    public void onCreate() {
        super.onCreate();

        db = DataBaseHelper.getInstance(this);
        prefs = PrefsManager.getInstance(this);
        bcm = LocalBroadcastManager.getInstance(this);
        bcm.registerReceiver(onBookSetChanged, new IntentFilter(Communication.BOOK_SET_CHANGED));
        bcm.registerReceiver(onCurrentBookChanged, new IntentFilter(Communication.CURRENT_BOOK_CHANGED));
        bcm.registerReceiver(onPlayStateChanged, new IntentFilter(Communication.PLAY_STATE_CHANGED));
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        updateWidget();
        return Service.START_STICKY;
    }

    /**
     * Asynchronously updates the widget
     */
    private void updateWidget() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(WidgetUpdateService.this);
                Book book = db.getBook(prefs.getCurrentBookId());
                boolean isPortrait = isPortrait();
                int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(
                        WidgetUpdateService.this, BaseWidgetProvider.class));

                for (int widgetId : ids) {
                    RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.widget);

                    if (book != null) {
                        initElements(remoteViews, book);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            Bundle opts = appWidgetManager.getAppWidgetOptions(widgetId);
                            int minHeight = dpToPx(opts.getInt(
                                    AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));
                            int maxHeight = dpToPx(opts.getInt(
                                    AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));
                            int minWidth = dpToPx(opts.getInt(
                                    AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH));
                            int maxWidth = dpToPx(opts.getInt(
                                    AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH));

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
                                setVisibilities(remoteViews, useWidth, useHeight,
                                        book.getChapters().size() == 1);
                            }
                        }
                    } else {
                        // directly going back to bookChoose
                        Intent wholeWidgetClickI = BookActivity.bookScreenIntent
                                (WidgetUpdateService.this);
                        PendingIntent wholeWidgetClickPI = PendingIntent.getActivity
                                (WidgetUpdateService.this, (int) System.currentTimeMillis(),
                                        wholeWidgetClickI, PendingIntent.FLAG_UPDATE_CURRENT);
                        //noinspection deprecation
                        remoteViews.setImageViewBitmap(R.id.imageView,
                                ImageHelper.drawableToBitmap(
                                        getResources().getDrawable(R.drawable.icon_108dp),
                                        ImageHelper.getSmallerScreenSize(WidgetUpdateService.this),
                                        ImageHelper.getSmallerScreenSize(
                                                WidgetUpdateService.this)));
                        remoteViews.setOnClickPendingIntent(R.id.wholeWidget, wholeWidgetClickPI);
                    }

                    appWidgetManager.updateAppWidget(widgetId, remoteViews);
                }
            }
        });
    }

    /**
     * Returning if the current orientation is portrait. If it is unknown, measure the display-spec
     * and return accordingly.
     *
     * @return true if the current orientation is portrait
     */
    private boolean isPortrait() {
        int orientation = getResources().getConfiguration().orientation;
        WindowManager window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = window.getDefaultDisplay();

        @SuppressWarnings("deprecation") int displayWidth = display.getWidth();
        @SuppressWarnings("deprecation") int displayHeight = display.getHeight();

        return orientation != Configuration.ORIENTATION_LANDSCAPE &&
                (orientation == Configuration.ORIENTATION_PORTRAIT || displayWidth == displayHeight
                        || displayWidth < displayHeight);
    }

    /**
     * Initializes the elements of the widgets with a book
     *
     * @param remoteViews The Widget RemoteViews
     * @param book        The book to be initalized
     */
    private void initElements(@NonNull final RemoteViews remoteViews, @NonNull final Book book) {
        Intent playPauseI = ServiceController.getPlayPauseIntent(this);
        PendingIntent playPausePI = PendingIntent.getService(this,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, playPauseI, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.playPause, playPausePI);

        Intent fastForwardI = ServiceController.getFastForwardIntent(this);
        PendingIntent fastForwardPI = PendingIntent.getService(this,
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, fastForwardI,
                PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.fastForward, fastForwardPI);

        Intent rewindI = ServiceController.getRewindIntent(this);
        PendingIntent rewindPI = PendingIntent.getService(this, KeyEvent.KEYCODE_MEDIA_REWIND,
                rewindI, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.rewind, rewindPI);

        if (MediaPlayerController.getPlayState() == MediaPlayerController.PlayState.PLAYING) {
            remoteViews.setImageViewResource(R.id.playPause, R.drawable.ic_pause_white_36dp);
        } else {
            remoteViews.setImageViewResource(R.id.playPause, R.drawable.ic_play_arrow_white_36dp);
        }

        // if we have any book, init the views and have a click on the whole widget start BookPlay.
        // if we have no book, simply have a click on the whole widget start BookChoose.

        remoteViews.setTextViewText(R.id.title, book.getName());
        String name = book.getCurrentChapter().getName();

        remoteViews.setTextViewText(R.id.summary, name);

        Intent wholeWidgetClickI = new Intent(this, BookActivity.class);
        wholeWidgetClickI.putExtra(BookActivity.TARGET_FRAGMENT, BookPlayFragment.TAG);
        PendingIntent wholeWidgetClickPI = PendingIntent.getActivity
                (WidgetUpdateService.this, (int) System.currentTimeMillis(), wholeWidgetClickI,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap cover = null;
        try {
            File coverFile = book.getCoverFile();
            if (!book.isUseCoverReplacement() && coverFile.exists() && coverFile.canRead()) {
                cover = Picasso.with(WidgetUpdateService.this).load(coverFile).get();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (cover == null) {
            cover = ImageHelper.drawableToBitmap(new CoverReplacement(
                            book.getName(),
                            WidgetUpdateService.this), ImageHelper.getSmallerScreenSize(this),
                    ImageHelper.getSmallerScreenSize(this));
        }
        remoteViews.setImageViewBitmap(R.id.imageView, cover);
        remoteViews.setOnClickPendingIntent(R.id.wholeWidget, wholeWidgetClickPI);
    }

    /**
     * Converts dp to px
     *
     * @param dp the dp to be converted
     * @return the px the dp represent
     */
    private int dpToPx(final int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }

    /**
     * Sets visibilities on widgets element, depending on the size of the widget
     *
     * @param remoteViews   the widget the widget RemoteViews
     * @param width         the width of the widget
     * @param height        the height of the widget
     * @param singleChapter if true if the book has only one chapter
     */
    private void setVisibilities(@NonNull final RemoteViews remoteViews, final int width,
                                 final int height, final boolean singleChapter) {
        setXVisibility(remoteViews, width, height);
        setYVisibility(remoteViews, height, singleChapter);
    }

    /**
     * Set visibilities dependent on widget width.
     *
     * @param remoteViews the widget RemoteViews
     * @param widgetWidth The widget width
     * @param coverSize   The cover size
     */
    private void setXVisibility(@NonNull final RemoteViews remoteViews, final int widgetWidth,
                                final int coverSize) {
        int singleButtonSize = dpToPx(8 + 36 + 8);
        // widget height because cover is square
        int summarizedItemWidth = 3 * singleButtonSize + coverSize;

        // set all views visible
        remoteViews.setViewVisibility(R.id.imageView, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.rewind, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.fastForward, View.VISIBLE);

        // hide cover if we need space
        if (summarizedItemWidth > widgetWidth) {
            remoteViews.setViewVisibility(R.id.imageView, View.GONE);
            summarizedItemWidth -= coverSize;
        }

        // hide fast forward if we need space
        if (summarizedItemWidth > widgetWidth) {
            remoteViews.setViewVisibility(R.id.fastForward, View.GONE);
            summarizedItemWidth -= singleButtonSize;
        }

        // hide rewind if we need space
        if (summarizedItemWidth > widgetWidth) {
            remoteViews.setViewVisibility(R.id.rewind, View.GONE);
        }
    }

    /**
     * Sets visibilities dependent on widget height.
     *
     * @param remoteViews   The Widget RemoteViews
     * @param widgetHeight  The widget height
     * @param singleChapter true if the book has only one chapter
     */
    private void setYVisibility(@NonNull final RemoteViews remoteViews, final int widgetHeight,
                                final boolean singleChapter) {
        int buttonSize = dpToPx(8 + 36 + 8);
        int titleSize = getResources().getDimensionPixelSize(R.dimen.list_text_primary_size);
        int summarySize = getResources().getDimensionPixelSize(R.dimen.list_text_secondary_size);

        int summarizedItemsHeight = buttonSize + titleSize + summarySize;

        // first setting all views visible
        remoteViews.setViewVisibility(R.id.summary, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.title, View.VISIBLE);

        // when we are in a single chapter or we are to high, hide summary
        if (singleChapter || widgetHeight < summarizedItemsHeight) {
            remoteViews.setViewVisibility(R.id.summary, View.GONE);
            summarizedItemsHeight -= summarySize;
        }

        // if we ar still to high, hide title
        if (summarizedItemsHeight > widgetHeight) {
            remoteViews.setViewVisibility(R.id.title, View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        bcm.unregisterReceiver(onCurrentBookChanged);
        bcm.unregisterReceiver(onBookSetChanged);
        bcm.unregisterReceiver(onPlayStateChanged);
    }

    @Override
    public void onConfigurationChanged(final Configuration newCfg) {
        int oldOrientation = this.getResources().getConfiguration().orientation;
        int newOrientation = newCfg.orientation;

        if (newOrientation != oldOrientation) {
            updateWidget();
        }
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }
}
