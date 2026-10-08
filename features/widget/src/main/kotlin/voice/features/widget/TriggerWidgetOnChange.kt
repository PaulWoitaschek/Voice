package voice.features.widget

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import voice.core.initializer.AppInitializer
import voice.core.logging.api.Logger
import kotlin.time.Duration.Companion.seconds

@ContributesIntoSet(AppScope::class)
@Inject
class TriggerWidgetOnChange(
  private val context: Context,
  private val placedWidgets: PlacedWidgets,
  private val widgetData: WidgetData,
  private val scope: CoroutineScope,
) : AppInitializer {

  override fun onAppStart(application: Application) {
    placedWidgets.refresh()
    WidgetKind.entries.forEach { kind ->
      scope.launch(Dispatchers.Default) {
        placedWidgets.placed(kind)
          .flatMapLatest { placed -> if (placed) changes(kind) else emptyFlow() }
          .conflate()
          .collect { update(kind) }
      }
    }
    if (Build.VERSION.SDK_INT >= 35) {
      scope.launch(Dispatchers.Default) {
        // not urgent, and it shouldn't compete with the app starting
        delay(30.seconds)
        updatePreviews()
      }
    }
  }

  private fun changes(kind: WidgetKind): Flow<Any> = when (kind) {
    WidgetKind.NowPlaying -> widgetData.nowPlaying
    WidgetKind.Shelf -> widgetData.shelf
    WidgetKind.SleepTimer -> widgetData.sleepTimerModel
  }

  private suspend fun update(kind: WidgetKind) {
    try {
      kind.widget().updateAll(context)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Logger.w(e, "Can't update the $kind widget")
    }
  }

  /** The widget picker shows the widgets with the current book, from Android 15 on. */
  @RequiresApi(35)
  private suspend fun updatePreviews() {
    val manager = GlanceAppWidgetManager(context)
    WidgetKind.entries.forEach { kind ->
      try {
        val result = manager.setWidgetPreviews(kind.receiver.kotlin)
        if (result != GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) {
          // the system rate limits this, which is fine as the previews don't have to be current
          Logger.d("The preview of the $kind widget wasn't set: $result")
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Logger.w(e, "Can't set the preview of the $kind widget")
      }
    }
  }
}
