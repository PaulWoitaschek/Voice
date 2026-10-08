package voice.features.widget

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import kotlinx.coroutines.flow.first
import voice.core.strings.R as StringsR

class SleepTimerWidget : GlanceAppWidget() {

  override val sizeMode: SizeMode = SizeMode.Exact

  override val previewSizeMode = SizeMode.Responsive(setOf(DpSize(100.dp, 100.dp), DpSize(180.dp, 90.dp)))

  override suspend fun provideGlance(
    context: Context,
    id: GlanceId,
  ) {
    val graph = widgetGraph
    val states = graph.widgetData.sleepTimerState()
    val initial = states.first()
    provideContent {
      val state by states.collectAsState(initial)
      SleepTimerContent(state, graph.widgetImages, graph.widgetActions)
    }
  }

  override suspend fun providePreview(
    context: Context,
    widgetCategory: Int,
  ) {
    val graph = widgetGraph
    val state = graph.widgetData.sleepTimerState().first()
    provideContent {
      SleepTimerContent(state, graph.widgetImages, graph.widgetActions)
    }
  }
}

@Composable
internal fun SleepTimerContent(
  state: SleepTimerWidgetState,
  images: WidgetImages,
  actions: WidgetActions,
) {
  val context = LocalContext.current
  val sleepTimer = state.sleepTimer
  val running = sleepTimer is SleepTimerWidgetModel.Running
  val description = context.getString(
    if (running) StringsR.string.widget_sleep_timer_cancel else StringsR.string.widget_sleep_timer_start,
  )
  WidgetColors(theme = state.theme, night = running) {
    val size = LocalSize.current
    val modifier = GlanceModifier
      .fillMaxSize()
      .appWidgetBackground()
      .clickable(actions.toggleSleepTimer())
      .semantics { contentDescription = description }
    if (size.width < 130.dp || size.height >= 120.dp) {
      SleepTimerBadge(sleepTimer, images, modifier)
    } else {
      SleepTimerBar(sleepTimer, images, modifier)
    }
  }
}

/** The timer as a single shape: a cookie that's ready, or a square that's running, like the play button. */
@Composable
private fun SleepTimerBadge(
  sleepTimer: SleepTimerWidgetModel,
  images: WidgetImages,
  modifier: GlanceModifier = GlanceModifier,
) {
  val context = LocalContext.current
  val size = LocalSize.current
  val shapeSize = min(size.width, size.height) - 8.dp
  val running = sleepTimer is SleepTimerWidgetModel.Running
  val container = if (running) GlanceTheme.colors.secondaryContainer else GlanceTheme.colors.primaryContainer
  val content = if (running) GlanceTheme.colors.onSecondaryContainer else GlanceTheme.colors.onPrimaryContainer
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Box(modifier = GlanceModifier.size(shapeSize), contentAlignment = Alignment.Center) {
      Image(
        provider = ImageProvider(images.shape(if (running) WidgetShape.Square else WidgetShape.Cookie12, shapeSize)),
        contentDescription = null,
        modifier = GlanceModifier.fillMaxSize(),
        colorFilter = ColorFilter.tint(container),
      )
      if (running) {
        Stars(R.drawable.widget_stars, GlanceModifier.fillMaxSize().padding(shapeSize * 0.12F))
      }
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        WidgetIcon(icon = R.drawable.widget_ic_moon, size = shapeSize * 0.24F, color = content)
        Spacer(GlanceModifier.height(2.dp))
        Text(
          text = sleepTimer.label(context),
          style = TextStyle(
            color = content,
            fontSize = (shapeSize.value * 0.16F).coerceIn(12F, 28F).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          ),
          maxLines = 2,
        )
      }
    }
  }
}

@Composable
private fun SleepTimerBar(
  sleepTimer: SleepTimerWidgetModel,
  images: WidgetImages,
  modifier: GlanceModifier = GlanceModifier,
) {
  val context = LocalContext.current
  val size = LocalSize.current
  val running = sleepTimer is SleepTimerWidgetModel.Running
  Box(modifier = modifier.widgetSurface(GlanceTheme.colors.widgetBackground, pill = true)) {
    if (running) {
      Stars(R.drawable.widget_stars_wide, GlanceModifier.fillMaxSize())
    }
    Row(modifier = GlanceModifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
      ShapeButton(
        shape = if (running) WidgetShape.Square else WidgetShape.Cookie12,
        size = (size.height - 16.dp).coerceIn(36.dp, 64.dp),
        icon = R.drawable.widget_ic_moon,
        contentDescription = null,
        images = images,
        onClick = null,
        container = if (running) GlanceTheme.colors.secondaryContainer else GlanceTheme.colors.primaryContainer,
        content = if (running) GlanceTheme.colors.onSecondaryContainer else GlanceTheme.colors.onPrimaryContainer,
      )
      Spacer(GlanceModifier.width(10.dp))
      Column(modifier = GlanceModifier.defaultWeight()) {
        when (sleepTimer) {
          is SleepTimerWidgetModel.Ready -> {
            TitleText(text = context.getString(StringsR.string.widget_sleep_timer_label), fontSize = 15.sp)
            SubtitleText(text = context.formatDuration(sleepTimer.duration))
          }
          is SleepTimerWidgetModel.Running -> {
            TitleText(text = sleepTimer.label(context), fontSize = 15.sp)
            SubtitleText(
              text = context.getString(
                if (sleepTimer.end.endOfChapter && sleepTimer.end.at != null) {
                  StringsR.string.sleep_timer_end_of_chapter
                } else {
                  StringsR.string.widget_sleep_timer_cancel_hint
                },
              ),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun Stars(
  @DrawableRes drawable: Int,
  modifier: GlanceModifier = GlanceModifier,
) {
  Image(
    provider = ImageProvider(drawable),
    contentDescription = null,
    modifier = modifier,
    contentScale = ContentScale.FillBounds,
    colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
  )
}

private fun SleepTimerWidgetModel.label(context: Context): String = when (this) {
  is SleepTimerWidgetModel.Ready -> context.formatDuration(duration)
  is SleepTimerWidgetModel.Running -> end.at?.let(context::formatClockTime)
    ?: context.getString(StringsR.string.sleep_timer_end_of_chapter)
}
