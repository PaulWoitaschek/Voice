package de.ph1b.audiobook.features.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import de.ph1b.audiobook.injection.App
import javax.inject.Inject

class BaseWidgetProvider : AppWidgetProvider() {

  @Inject
  lateinit var widgetUpdater: WidgetUpdater

  override fun onReceive(context: Context, intent: Intent?) {
    App.component.inject(this)
    super.onReceive(context, intent)
  }

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
  ) {
    widgetUpdater.update()
  }

  override fun onAppWidgetOptionsChanged(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    newOptions: Bundle
  ) {
    widgetUpdater.update()
  }
}
