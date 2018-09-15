package de.ph1b.audiobook.features.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class BaseWidgetProvider : AppWidgetProvider(), KoinComponent {

  private val widgetUpdater: WidgetUpdater by inject()

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
