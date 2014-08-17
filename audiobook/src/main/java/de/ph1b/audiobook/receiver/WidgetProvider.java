package de.ph1b.audiobook.receiver;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.ArrayList;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.helper.BookDetail;
import de.ph1b.audiobook.helper.DataBaseHelper;
import de.ph1b.audiobook.service.AudioPlayerService;

public class WidgetProvider extends AppWidgetProvider {

    private static final String TAG = "de.ph1b.audiobook.receiver.WidgetProvider";
    private static final String PLAY_CLICK = TAG + ".PLAY_CLICK";


    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {

        if (BuildConfig.DEBUG)
            Log.d(TAG, "onUpdate called!, lenght: " + appWidgetIds.length);
        for (int appWidgetId : appWidgetIds) {

            final RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.widget);
            remoteViews.setOnClickPendingIntent(R.id.widgetPlayButton, getPendingSelfIntent(context, PLAY_CLICK));
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);
        if (BuildConfig.DEBUG)
            Log.d(TAG, "onReceive called!, intent is :" + intent.getAction());
        if (intent.getAction().equals(PLAY_CLICK)) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "clicked play!");

            SharedPreferences settings = context.getSharedPreferences(de.ph1b.audiobook.activity.MediaView.SHARED_PREFS, 0);
            int position = settings.getInt(de.ph1b.audiobook.activity.MediaView.SHARED_PREFS_CURRENT, -1);

            DataBaseHelper db = DataBaseHelper.getInstance(context);
            final BookDetail b = db.getBook(position);

            if (b != null) {
                Intent i = new Intent(context, AudioPlayerService.class);
                i.putExtra(AudioPlayerService.BOOK_ID, b.getId());
                context.startService(i);

                LocalBroadcastManager bcm = LocalBroadcastManager.getInstance(context);
                bcm.sendBroadcast(new Intent(AudioPlayerService.CONTROL_PLAY_PAUSE));
            } else {
                ArrayList <BookDetail> books = db.getAllBooks();


            }
        }
    }

}
