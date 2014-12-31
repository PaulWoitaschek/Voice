package de.ph1b.audiobook.receiver;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.widget.RemoteViews;

import java.io.File;
import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BookChoose;
import de.ph1b.audiobook.activity.BookPlay;
import de.ph1b.audiobook.content.BookDetail;
import de.ph1b.audiobook.content.DataBaseHelper;
import de.ph1b.audiobook.service.AudioPlayerService;
import de.ph1b.audiobook.utils.ImageHelper;

public class WidgetProvider extends AppWidgetProvider {


    private static final String START_BOOK_PLAY = "startBookPlay";

    @Override
    public void onUpdate(final Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
            int number = 0;

            Intent intent = new Intent(context, getClass());
            intent.setAction(START_BOOK_PLAY);
            PendingIntent wholeWidgetClick = PendingIntent.getBroadcast
                    (context, number++, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.whole_widget, wholeWidgetClick);

            remoteViews.setOnClickPendingIntent(R.id.rewind, getMediaButtonSelfIntent
                    (context, number++, KeyEvent.KEYCODE_MEDIA_REWIND));

            remoteViews.setOnClickPendingIntent(R.id.playPause, getMediaButtonSelfIntent
                    (context, number++, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));

            //noinspection UnusedAssignment
            remoteViews.setOnClickPendingIntent(R.id.fast_forward, getMediaButtonSelfIntent
                    (context, number++, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD));


            DataBaseHelper db = DataBaseHelper.getInstance(context);
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            int bookId = settings.getInt(BookChoose.SHARED_PREFS_CURRENT, -1);
            BookDetail book = db.getBook(bookId);
            if (book != null) {
                Bitmap cover;
                if (book.getCover() != null && new File(book.getCover()).exists()) {
                    cover = ImageHelper.genBitmapFromFile(book.getCover(), context, ImageHelper.TYPE_NOTIFICATION_SMALL);
                } else {
                    cover = ImageHelper.genCapital(book.getName(), context, ImageHelper.TYPE_NOTIFICATION_SMALL);
                }

                remoteViews.setImageViewBitmap(R.id.imageView, cover);
                remoteViews.setTextViewText(R.id.title, book.getName());
            }

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }


    private PendingIntent getMediaButtonSelfIntent(Context context, int number, int keyEvent) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(Intent.ACTION_MEDIA_BUTTON);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
        return PendingIntent.getBroadcast(context, number, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    @Override
    public void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
        super.onReceive(context, intent);
        final String action = intent.getAction();

        new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                int bookId = settings.getInt(BookChoose.SHARED_PREFS_CURRENT, -1);
                int keyEvent = intent.getIntExtra(Intent.EXTRA_KEY_EVENT, -1);
                DataBaseHelper db = DataBaseHelper.getInstance(context);
                BookDetail book = db.getBook(bookId);
                if (book == null) {
                    ArrayList<BookDetail> books = db.getAllBooks();
                    if (books.size() > 0) {
                        book = books.get(0);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putInt(BookChoose.SHARED_PREFS_CURRENT, book.getId());
                        editor.apply();
                    }
                }

                if (action.equals(Intent.ACTION_MEDIA_BUTTON) && book != null) {
                    Intent i = new Intent(context, AudioPlayerService.class);
                    i.putExtra(AudioPlayerService.GUI_BOOK_ID, book.getId());
                    i.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
                    context.startService(i);
                } else if (action.equals(START_BOOK_PLAY)) {
                    if (book == null) {
                        Intent i = new Intent(context, BookChoose.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(i);
                    } else {
                        Intent i = new Intent(context, BookPlay.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        i.putExtra(AudioPlayerService.GUI_BOOK_ID, book.getId());
                        context.startActivity(i);
                    }
                }
            }
        }).start();
    }
}




