package de.ph1b.audiobook.receiver;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.widget.RemoteViews;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.activity.BookChoose;
import de.ph1b.audiobook.activity.BookPlay;
import de.ph1b.audiobook.content.Book;
import de.ph1b.audiobook.content.DataBaseHelper;
import de.ph1b.audiobook.service.PlayerStates;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.service.StateManager;
import de.ph1b.audiobook.utils.Prefs;

public class WidgetProvider extends AppWidgetProvider {

    private void initButtons(RemoteViews remoteViews, Context context) {
        Intent playPauseI = ServiceController.getPlayPauseIntent(context);
        PendingIntent playPausePI = PendingIntent.getService(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, playPauseI, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.playPause, playPausePI);

        Intent fastForwardI = ServiceController.getFastForwardIntent(context);
        PendingIntent fastForwardPI = PendingIntent.getService(context, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, fastForwardI, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.fast_forward, fastForwardPI);

        Intent rewindI = ServiceController.getRewindIntent(context);
        PendingIntent rewindPI = PendingIntent.getService(context, KeyEvent.KEYCODE_MEDIA_REWIND, rewindI, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.rewind, rewindPI);

        if (StateManager.getInstance(context).getState() == PlayerStates.PLAYING) {
            remoteViews.setImageViewResource(R.id.playPause, R.drawable.ic_pause_white_48dp);
        } else {
            remoteViews.setImageViewResource(R.id.playPause, R.drawable.ic_play_arrow_white_48dp);
        }
    }

    private void initBookRelated(RemoteViews remoteViews, Context context, Book book) {
        // if we have any book, init the views and have a click on the whole widget start BookPlay.
        // if we have no book, simply have a click on the whole widget start BookChoose.
        if (book != null) {
            //   Picasso.with(context).load(new File(book.getCover())).into(remoteViews, R.id.imageView, appWidgetIds);
            remoteViews.setTextViewText(R.id.title, book.getName());

            String name = book.getContainingMedia().get(book.getPosition()).getName();
            remoteViews.setTextViewText(R.id.summary, name);

            Intent wholeWidgetClickI = new Intent(context, BookPlay.class);
            wholeWidgetClickI.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent wholeWidgetClickPI = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), wholeWidgetClickI, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.imageView, wholeWidgetClickPI);
        } else {
            Intent wholeWidgetClickI = new Intent(context, BookChoose.class);
            wholeWidgetClickI.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent wholeWidgetClickPI = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), wholeWidgetClickI, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.imageView, wholeWidgetClickPI);
        }
    }

    private Book getCurrentBook(Context context) {
        // get book from database
        DataBaseHelper db = DataBaseHelper.getInstance(context);
        Prefs prefs = new Prefs(context);
        long bookId = prefs.getCurrentBookId();
        Book book = db.getBook(bookId);

        // if there is no current book, take the first one from all
        if (book == null) {
            ArrayList<Book> books = db.getAllBooks();
            if (books.size() > 0) {
                book = books.get(0);
                prefs.setCurrentBookId(book.getId());
            }
        }
        return book;
    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {


        Book book = getCurrentBook(context);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);

        for (int appWidgetId : appWidgetIds) {
            initButtons(remoteViews, context);
            initBookRelated(remoteViews, context, book);
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }

        if (book != null) {
            Picasso.with(context).load(new File(book.getCover())).into(remoteViews, R.id.imageView, appWidgetIds);
        }
    }
}




