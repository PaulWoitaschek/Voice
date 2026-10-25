@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookmark.history

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import voice.core.data.ListeningEvent.Source
import voice.core.data.ListeningEvent.Type
import voice.core.strings.R
import voice.core.ui.VoiceTheme
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.plus
import voice.features.bookmark.dayLabelText
import voice.features.bookmark.durationText
import voice.features.bookmark.kioskModeHistoryViewState
import voice.features.bookmark.timeText

private val NodeSize = 32.dp
private val DotSize = 8.dp
private val LineColumnWidth = 32.dp
private val Gap = 8.dp
private val RowTop = 4.dp

@Composable
internal fun HistoryList(
  viewState: HistoryViewState,
  contentPadding: PaddingValues,
  onGoTo: (HistorySpot) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = contentPadding + PaddingValues(top = 8.dp, bottom = 24.dp),
  ) {
    if (viewState.days.isEmpty()) {
      item(key = "empty") {
        HistoryEmpty(Modifier.padding(top = 32.dp))
      }
    }
    viewState.days.forEach { day ->
      item(key = "day-${day.label}") {
        Text(
          text = dayLabelText(day.label),
          style = MaterialTheme.typography.titleSmallEmphasized,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier
            .animateItem()
            .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 6.dp)
            .semantics { heading() },
        )
      }
      items(day.sessions, key = { "session-${it.key}" }) { session ->
        Session(
          session = session,
          onGoTo = onGoTo,
          modifier = Modifier
            .animateItem()
            .padding(bottom = 8.dp),
        )
      }
    }
  }
}

@Composable
private fun Session(
  session: HistorySession,
  onGoTo: (HistorySpot) -> Unit,
  modifier: Modifier = Modifier,
) {
  val rows = session.rows
  val times = rows.map { timeText(it.at) }
  val timeStyle = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum")
  val measurer = rememberTextMeasurer()
  val density = LocalDensity.current
  // the line has to be straight, so every time takes the room of the widest one
  val timeWidth = remember(times, timeStyle, density) {
    with(density) { times.maxOf { measurer.measure(it, timeStyle).size.width }.toDp() }
  }
  Surface(
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
      rows.forEachIndexed { index, row ->
        val newer = rows.getOrNull(index - 1)
        TimelineRow(
          row = row,
          time = times[index],
          timeStyle = timeStyle,
          timeWidth = timeWidth,
          above = if (newer == null) Line.None else line(newer.playingBefore),
          below = if (index == rows.lastIndex) Line.None else line(row.playingBefore),
          onGoTo = onGoTo,
        )
      }
    }
  }
}

private enum class Line { None, Playing, Paused }

private fun line(playing: Boolean) = if (playing) Line.Playing else Line.Paused

@Composable
private fun TimelineRow(
  row: HistoryRow,
  time: String,
  timeStyle: TextStyle,
  timeWidth: Dp,
  above: Line,
  below: Line,
  onGoTo: (HistorySpot) -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  val quiet = row !is HistoryEvent
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .timeline(
        x = timeWidth + Gap + LineColumnWidth / 2,
        nodeSize = if (quiet) DotSize else NodeSize,
        above = above,
        below = below,
        playing = colors.primary,
        paused = colors.outline,
      )
      .padding(top = RowTop, bottom = if (quiet) RowTop else 0.dp)
      .semantics(mergeDescendants = true) {},
    horizontalArrangement = Arrangement.spacedBy(Gap),
  ) {
    Box(
      modifier = Modifier
        .width(timeWidth)
        .heightIn(min = NodeSize),
      contentAlignment = Alignment.CenterEnd,
    ) {
      Text(
        text = time,
        style = timeStyle,
        color = colors.onSurfaceVariant,
      )
    }
    Box(
      modifier = Modifier
        .width(LineColumnWidth)
        .height(NodeSize),
      contentAlignment = Alignment.Center,
    ) {
      if (row is HistoryEvent) {
        EventNode(row.type)
      } else {
        Box(
          Modifier
            .size(DotSize)
            .background(colors.onSurfaceVariant, CircleShape),
        )
      }
    }
    Column(Modifier.weight(1F)) {
      Box(
        modifier = Modifier.heightIn(min = NodeSize),
        contentAlignment = Alignment.CenterStart,
      ) {
        Text(
          text = rowText(row),
          style = if (quiet) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
          color = if (quiet) colors.onSurfaceVariant else colors.onSurface,
        )
      }
      if (row is HistoryEvent) {
        Places(row, onGoTo)
      }
    }
  }
}

private fun Modifier.timeline(
  x: Dp,
  nodeSize: Dp,
  above: Line,
  below: Line,
  playing: Color,
  paused: Color,
): Modifier = drawBehind {
  val lineX = if (layoutDirection == LayoutDirection.Rtl) size.width - x.toPx() else x.toPx()
  val center = (RowTop + NodeSize / 2).toPx()
  val radius = (nodeSize / 2).toPx()
  segment(above, lineX, 0F, center - radius, playing, paused)
  segment(below, lineX, center + radius, size.height, playing, paused)
}

private fun DrawScope.segment(
  line: Line,
  x: Float,
  from: Float,
  to: Float,
  playing: Color,
  paused: Color,
) {
  if (line == Line.None) return
  val dotted = line == Line.Paused
  drawLine(
    color = if (dotted) paused else playing,
    start = Offset(x, from),
    end = Offset(x, to),
    strokeWidth = 2.dp.toPx(),
    cap = if (dotted) StrokeCap.Round else StrokeCap.Butt,
    pathEffect = if (dotted) PathEffect.dashPathEffect(floatArrayOf(0.01F, 6.dp.toPx())) else null,
  )
}

@Composable
private fun EventNode(type: Type) {
  val colors = MaterialTheme.colorScheme
  val (container, content) = when (type) {
    Type.Play -> colors.primaryContainer to colors.onPrimaryContainer
    Type.Pause -> colors.surfaceContainerHighest to colors.onSurfaceVariant
    Type.SleepTimerSet, Type.SleepTimerStarted, Type.SleepTimerEnded, Type.SleepTimerExtended ->
      colors.tertiaryContainer to colors.onTertiaryContainer
    Type.Seek, Type.SkipBack, Type.SkipForward, Type.ChapterChange, Type.BookmarkJump, Type.JumpBack ->
      colors.secondaryContainer to colors.onSecondaryContainer
  }
  Box(
    modifier = Modifier
      .size(NodeSize)
      .background(container, CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = type.icon(),
      contentDescription = null,
      tint = content,
      modifier = Modifier.size(18.dp),
    )
  }
}

@Composable
private fun Places(
  event: HistoryEvent,
  onGoTo: (HistorySpot) -> Unit,
) {
  FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    itemVerticalAlignment = Alignment.CenterVertically,
  ) {
    PlaceButton(event.place, onGoTo)
    val to = event.to
    if (to != null) {
      // the arrow wraps with where the jump went, so a second line still reads as its end
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (LocalLayoutDirection.current == LayoutDirection.Rtl) "←" else "→",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.clearAndSetSemantics {},
        )
        PlaceButton(to, onGoTo)
      }
    }
  }
}

@Composable
private fun PlaceButton(
  place: HistoryPlace,
  onGoTo: (HistorySpot) -> Unit,
) {
  val size = ButtonDefaults.ExtraSmallContainerHeight
  FilledTonalButton(
    onClick = { onGoTo(place.spot) },
    contentPadding = ButtonDefaults.contentPaddingFor(size),
    shapes = ButtonDefaults.shapesFor(size),
    modifier = Modifier.heightIn(size),
  ) {
    Text(
      text = locationText(place.location),
      style = ButtonDefaults.textStyleFor(size).copy(fontFeatureSettings = "tnum"),
    )
  }
}

@Composable
private fun rowText(row: HistoryRow): String = when (row) {
  is HistoryEvent -> eventText(row)
  is HistorySkips -> pluralStringResource(
    if (row.back) R.plurals.history_event_rewound else R.plurals.history_event_skipped_ahead,
    row.count,
    row.count,
  )
  is HistoryInterruption -> stringResource(R.string.history_event_interrupted, durationText(row.duration))
}

@Composable
private fun eventText(event: HistoryEvent): String {
  val text = stringResource(
    when (event.type) {
      Type.Play -> R.string.history_event_played
      Type.Pause -> R.string.history_event_paused
      Type.SleepTimerSet, Type.SleepTimerStarted -> R.string.history_event_sleep_timer_on
      Type.SleepTimerEnded -> R.string.history_event_sleep_timer_ended
      Type.SleepTimerExtended -> R.string.history_event_shook
      Type.ChapterChange -> R.string.history_event_chapter_change
      Type.BookmarkJump -> R.string.history_event_bookmark_jump
      Type.JumpBack -> R.string.history_event_jump_back
      Type.Seek, Type.SkipBack, Type.SkipForward -> R.string.history_event_moved
    },
  )
  // playing on after a call isn't an interruption, it's the end of one
  val source = if (event.source == Source.AudioFocus && event.type == Type.Play) null else sourceLabel(event.source)
  return if (source != null) "$text · $source" else text
}

@Composable
private fun locationText(location: HistoryLocation): String {
  val chapterNumber = location.chapterNumber
  return if (chapterNumber != null) {
    stringResource(R.string.history_location_chapter, chapterNumber, location.time)
  } else {
    location.time
  }
}

@Composable
private fun sourceLabel(source: Source): String? = stringResource(
  when (source) {
    Source.App, Source.Unknown, Source.SleepTimer -> return null
    Source.Widget -> R.string.history_source_widget
    Source.Notification -> R.string.history_source_notification
    Source.Headset -> R.string.history_source_headset
    Source.Bluetooth -> R.string.history_source_bluetooth
    Source.Car -> R.string.history_source_car
    Source.Watch -> R.string.history_source_watch
    Source.OtherApp -> R.string.history_source_other_app
    Source.AudioFocus -> R.string.history_source_audio_focus
    Source.Unplugged -> R.string.history_source_unplugged
    Source.OtherDevice -> R.string.history_source_other_device
  },
)

private fun Type.icon(): ImageVector = when (this) {
  Type.Play -> VoiceIcons.PlayArrow
  Type.Pause -> VoiceIcons.Pause
  Type.SleepTimerSet, Type.SleepTimerStarted -> VoiceIcons.Bedtime
  Type.SleepTimerEnded -> VoiceIcons.BedtimeOff
  Type.SleepTimerExtended -> VoiceIcons.Vibration
  Type.ChapterChange -> VoiceIcons.SkipNext
  Type.BookmarkJump -> VoiceIcons.Bookmark
  Type.JumpBack -> VoiceIcons.Undo
  Type.SkipBack -> VoiceIcons.FastRewind
  Type.SkipForward -> VoiceIcons.FastForward
  Type.Seek -> VoiceIcons.SwapHoriz
}

@Composable
private fun HistoryEmpty(modifier: Modifier = Modifier) {
  val colors = MaterialTheme.colorScheme
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier
        .size(120.dp)
        .background(colors.secondaryContainer, MaterialShapes.Cookie12Sided.toShape()),
      contentAlignment = Alignment.Center,
    ) {
      Icon(VoiceIcons.History, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(48.dp))
    }
    Spacer(Modifier.height(24.dp))
    Text(
      text = stringResource(R.string.history_empty_title),
      style = MaterialTheme.typography.headlineSmallEmphasized,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = stringResource(R.string.history_empty_message),
      style = MaterialTheme.typography.bodyLarge,
      color = colors.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Preview(heightDp = 1600)
@Preview(heightDp = 1600, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "dark")
@Preview(heightDp = 1600, locale = "ar", name = "rtl")
@Preview(heightDp = 1800, fontScale = 2F, name = "font scale 2")
@Composable
private fun HistoryListPreview() {
  VoiceTheme {
    Surface {
      HistoryList(viewState = kioskModeHistoryViewState(), contentPadding = PaddingValues(horizontal = 16.dp), onGoTo = {})
    }
  }
}
