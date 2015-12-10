package de.ph1b.audiobook.receiver

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle

import de.ph1b.audiobook.playback.WidgetUpdateService

class BaseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        context.startService(Intent(context, WidgetUpdateService::class.java))
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            context.startService(Intent(context, WidgetUpdateService::class.java))
            super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        }
    }
}