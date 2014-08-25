package de.ph1b.audiobook.receiver;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.ArrayList;

import de.ph1b.audiobook.BuildConfig;
import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BookChoose;
import de.ph1b.audiobook.utils.BookDetail;
import de.ph1b.audiobook.utils.DataBaseHelper;
import de.ph1b.audiobook.service.AudioPlayerService;

public class WidgetProvider extends AppWidgetProvider {

    private static final String TAG = "de.ph1b.audiobook.receiver.WidgetProvider";
    private static final String PLAY_CLICK = TAG + ".PLAY_CLICK";


    private PendingIntent getPendingSelfIntent(Context context, String action) {
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

            SharedPreferences settings = context.getSharedPreferences(BookChoose.SHARED_PREFS, 0);
            int position = settings.getInt(BookChoose.SHARED_PREFS_CURRENT, -1);

            DataBaseHelper db = DataBaseHelper.getInstance(context);
            final BookDetail b = db.getBook(position);

            Intent i = new Intent(context, AudioPlayerService.class);

            if (b != null) { // if a valid book is found start service
                i.putExtra(AudioPlayerService.BOOK_ID, b.getId());
                i.setAction(AudioPlayerService.CONTROL_PLAY_PAUSE);
                context.startService(i);
            } else { //if no valid book is found search for available books
                ArrayList<BookDetail> books = db.getAllBooks();
                if (books.size() > 0) { //if there are valid books start the first one
                    int bookId = books.get(0).getId();

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putInt(BookChoose.SHARED_PREFS_CURRENT, bookId);
                    editor.apply();

                    i.putExtra(AudioPlayerService.BOOK_ID, bookId);
                    i.setAction(AudioPlayerService.CONTROL_PLAY_PAUSE);
                    context.startService(i);
                }
            }
        }
    }

}
