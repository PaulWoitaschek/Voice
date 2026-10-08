package voice.features.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import voice.app.features.widget.BaseWidgetProvider
import voice.core.logging.api.Logger

internal enum class WidgetKind(val receiver: Class<out GlanceAppWidgetReceiver>) {
  NowPlaying(BaseWidgetProvider::class.java),
  Shelf(ShelfWidgetReceiver::class.java),
  SleepTimer(SleepTimerWidgetReceiver::class.java),
  ;

  fun widget(): GlanceAppWidget = when (this) {
    NowPlaying -> NowPlayingWidget()
    Shelf -> ShelfWidget()
    SleepTimer -> SleepTimerWidget()
  }
}

/** Which widgets are on a home screen, so nothing is watched for widgets nobody placed. */
@SingleIn(AppScope::class)
@Inject
class PlacedWidgets(
  private val context: Context,
  private val scope: CoroutineScope,
) {

  private val placed = MutableStateFlow<Set<WidgetKind>?>(null)

  // one at a time and in order, so a refresh that read the widgets earlier can't overwrite a newer one
  private val refreshDispatcher = Dispatchers.IO.limitedParallelism(1)

  internal fun placed(kind: WidgetKind): Flow<Boolean> = placed
    .filterNotNull()
    .map { kind in it }
    .distinctUntilChanged()

  fun refresh() {
    scope.launch(refreshDispatcher) {
      placed.value = try {
        val manager = AppWidgetManager.getInstance(context)
        WidgetKind.entries
          .filter { manager.getAppWidgetIds(ComponentName(context, it.receiver)).isNotEmpty() }
          .toSet()
      } catch (e: RuntimeException) {
        // the widget service is missing on some devices
        Logger.w(e, "Can't read the placed widgets")
        emptySet()
      }
    }
  }
}
