package de.ph1b.audiobook.receiver;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.KeyEvent;
import android.widget.RemoteViews;

import java.io.File;
import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BookChoose;
import de.ph1b.audiobook.activity.BookPlay;
import de.ph1b.audiobook.content.BookDetail;
import de.ph1b.audiobook.content.DataBaseHelper;
import de.ph1b.audiobook.content.MediaDetail;
import de.ph1b.audiobook.service.AudioPlayerService;
import de.ph1b.audiobook.utils.ImageHelper;
import de.ph1b.audiobook.utils.Prefs;

public class WidgetProvider extends AppWidgetProvider {


    @Override
    public void onUpdate(final Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);

            Intent playPauseI = new Intent(context, AudioPlayerService.class);
            playPauseI.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            PendingIntent playPausePI = PendingIntent.getService(context, 0, playPauseI, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.playPause, playPausePI);

            Intent fastForwardI = new Intent(context, AudioPlayerService.class);
            fastForwardI.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
            PendingIntent fastForwardPI = PendingIntent.getService(context, 1, fastForwardI, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.fast_forward, fastForwardPI);

            Intent rewindI = new Intent(context, AudioPlayerService.class);
            rewindI.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.KEYCODE_MEDIA_REWIND);
            PendingIntent rewindPI = PendingIntent.getService(context, 2, rewindI, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.rewind, rewindPI);

            // get book from database
            DataBaseHelper db = DataBaseHelper.getInstance(context);
            int bookId = Prefs.getCurrentBookId(context);
            BookDetail book = db.getBook(bookId);

            // if there is no current book, take the first one from all
            if (book == null) {
                ArrayList<BookDetail> books = db.getAllBooks();
                if (books.size() > 0) {
                    book = books.get(0);
                    Prefs.setCurrentBookId(book.getId(), context);
                }
            }


            // if we have any book, init the views and have a click on the whole widget start BookPlay.
            // if we have no book, simply have a click on the whole widget start BookChoose.
            if (book != null) {
                Bitmap cover;
                if (book.getCover() != null && new File(book.getCover()).exists()) {
                    cover = ImageHelper.genBitmapFromFile(book.getCover(), context, ImageHelper.TYPE_NOTIFICATION_SMALL);
                } else {
                    cover = ImageHelper.genCapital(book.getName(), context, ImageHelper.TYPE_NOTIFICATION_SMALL);
                }

                remoteViews.setImageViewBitmap(R.id.imageView, cover);
                remoteViews.setTextViewText(R.id.title, book.getName());

                ArrayList<MediaDetail> allMedia = db.getMediaFromBook(bookId);
                for (MediaDetail m : allMedia) {
                    if (m.getId() == book.getCurrentMediaId()) {
                        remoteViews.setTextViewText(R.id.summary, m.getName());
                    }
                }

                Intent wholeWidgetClickI = new Intent(context, BookPlay.class);
                wholeWidgetClickI.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                wholeWidgetClickI.putExtra(AudioPlayerService.GUI_BOOK_ID, book.getId());
                PendingIntent wholeWidgetClickPI = PendingIntent.getActivity(context, 3, wholeWidgetClickI, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.whole_widget, wholeWidgetClickPI);
            } else {
                Intent wholeWidgetClickI = new Intent(context, BookChoose.class);
                wholeWidgetClickI.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent wholeWidgetClickPI = PendingIntent.getActivity(context, 3, wholeWidgetClickI, PendingIntent.FLAG_UPDATE_CURRENT);
                remoteViews.setOnClickPendingIntent(R.id.whole_widget, wholeWidgetClickPI);
            }
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }
}




