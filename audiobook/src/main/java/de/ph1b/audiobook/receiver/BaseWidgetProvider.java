package de.ph1b.audiobook.receiver;


import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import de.ph1b.audiobook.service.WidgetUpdateService;

public class BaseWidgetProvider extends AppWidgetProvider {


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        context.startService(new Intent(context, WidgetUpdateService.class));
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        if (Build.VERSION.SDK_INT >= 16) {
            context.startService(new Intent(context, WidgetUpdateService.class));
            super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        }
    }
}




