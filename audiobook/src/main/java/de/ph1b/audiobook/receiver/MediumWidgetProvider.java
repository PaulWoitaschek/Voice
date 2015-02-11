package de.ph1b.audiobook.receiver;


import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.widget.RemoteViews;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.content.Book;

public class MediumWidgetProvider extends BaseWidgetProvider {


    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        Book book = getCurrentBook(context);

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.medium_widget);
        for (int appWidgetId : appWidgetIds) {
            initButtons(remoteViews, context, book);
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }
}
