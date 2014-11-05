package de.ph1b.audiobook.receiver;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BookChoose;
import de.ph1b.audiobook.content.BookDetail;
import de.ph1b.audiobook.content.DataBaseHelper;
import de.ph1b.audiobook.service.AudioPlayerService;

public class WidgetProvider extends AppWidgetProvider {

    public static final String TAG = "de.ph1b.audiobook.receiver.WidgetProvider";
    private static final String PLAY_CLICK = TAG + ".PLAY_CLICK";


    private PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
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
        if (intent.getAction().equals(PLAY_CLICK)) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            int position = settings.getInt(BookChoose.SHARED_PREFS_CURRENT, -1);

            DataBaseHelper db = DataBaseHelper.getInstance(context);
            BookDetail book = db.getBook(position);

            Intent i = new Intent(context, AudioPlayerService.class);

            if (book != null) { // if a valid book is found start service
                i.putExtra(AudioPlayerService.GUI_BOOK_ID, book.getId());
                i.setAction(AudioPlayerService.CONTROL_PLAY_PAUSE);
                context.startService(i);
            } else { //if no valid book is found search for available books
                ArrayList<BookDetail> books = db.getAllBooks();
                if (books.size() > 0) { //if there are valid books start the first one
                    book = books.get(0);

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putInt(BookChoose.SHARED_PREFS_CURRENT, book.getId());
                    editor.apply();

                    i.putExtra(AudioPlayerService.GUI_BOOK_ID, book.getId());
                    i.setAction(AudioPlayerService.CONTROL_PLAY_PAUSE);
                    context.startService(i);
                }
            }
        }
    }

}
