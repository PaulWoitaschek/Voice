@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookmark.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import voice.core.data.ListeningEvent.Source
import voice.core.data.ListeningEvent.Type
import voice.core.strings.R
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.segmentedShape
import voice.features.bookmark.dayLabelText
import voice.features.bookmark.durationText
import voice.features.bookmark.timeText

@Composable
internal fun HistoryList(
  viewState: HistoryViewState,
  contentPadding: PaddingValues,
  onGoTo: (HistorySpot) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = 16.dp,
      end = 16.dp,
      top = contentPadding.calculateTopPadding() + 8.dp,
      bottom = contentPadding.calculateBottomPadding() + 24.dp,
    ),
    verticalArrangement = Arrangement.spacedBy(2.dp),
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
      itemsIndexed(
        items = day.rows,
        key = { _, row ->
          when (row) {
            is HistoryStretch -> "stretch-${row.key}"
            is HistoryJump -> "jump-${row.key}"
          }
        },
      ) { index, row ->
        val shape = segmentedShape(index, day.rows.size)
        when (row) {
          is HistoryStretch -> StretchRow(
            stretch = row,
            shape = shape,
            onGoTo = onGoTo,
            modifier = Modifier.animateItem(),
          )
          is HistoryJump -> JumpRow(
            jump = row,
            shape = shape,
            onGoTo = onGoTo,
            modifier = Modifier.animateItem(),
          )
        }
      }
    }
  }
}

@Composable
private fun StretchRow(
  stretch: HistoryStretch,
  shape: Shape,
  onGoTo: (HistorySpot) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Surface(
    shape = shape,
    color = colors.surfaceContainerHigh,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      StretchBadge(stretch.cause)
      Spacer(Modifier.width(16.dp))
      Column(Modifier.weight(1F)) {
        Text(
          text = whereText(stretch.from, stretch.to),
          style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
          color = colors.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = stretchWhen(stretch),
          style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
          color = colors.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        val cause = causeText(stretch.cause)
        if (cause != null) {
          Text(
            text = cause,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        val goTo = stretch.goTo
        if (goTo != null) {
          RowAction(
            text = goToText(goTo.location),
            icon = null,
            onClick = { onGoTo(goTo.spot) },
          )
        }
      }
    }
  }
}

/** Slimmer and quieter than a stretch, since it only connects two of them. */
@Composable
private fun JumpRow(
  jump: HistoryJump,
  shape: Shape,
  onGoTo: (HistorySpot) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Surface(
    shape = shape,
    color = colors.surfaceContainer,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(contentAlignment = Alignment.Center, modifier = Modifier.width(40.dp)) {
        Icon(
          imageVector = jump.type.jumpIcon(),
          contentDescription = null,
          tint = colors.onSurfaceVariant,
          modifier = Modifier.size(20.dp),
        )
      }
      Spacer(Modifier.width(16.dp))
      Column(Modifier.weight(1F)) {
        Text(
          text = whereText(jump.from, jump.to),
          style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
          color = colors.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${timeText(jump.at)} · ${jumpText(jump.type, jump.source)}",
          style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
          color = colors.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        RowAction(
          text = backText(jump.back.location),
          icon = VoiceIcons.Undo,
          onClick = { onGoTo(jump.back.spot) },
        )
      }
    }
  }
}

/** The one thing a row does, named with where it goes, so a tap never moves the position by surprise. */
@Composable
private fun RowAction(
  text: String,
  icon: ImageVector?,
  onClick: () -> Unit,
) {
  TextButton(
    onClick = onClick,
    contentPadding = PaddingValues(horizontal = 12.dp),
    // lines the label up with the text above, while the ripple keeps its padding
    modifier = Modifier.offset(x = (-12).dp),
  ) {
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(Modifier.width(8.dp))
    }
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun goToText(location: HistoryLocation): String {
  val chapterNumber = location.chapterNumber
  return if (chapterNumber != null) {
    stringResource(R.string.history_action_continue_from_chapter, chapterNumber, location.time)
  } else {
    stringResource(R.string.history_action_continue_from, location.time)
  }
}

@Composable
private fun backText(location: HistoryLocation): String {
  val chapterNumber = location.chapterNumber
  return if (chapterNumber != null) {
    stringResource(R.string.playback_jump_back_chapter, chapterNumber, location.time)
  } else {
    stringResource(R.string.playback_jump_back, location.time)
  }
}

@Composable
private fun StretchBadge(cause: StretchCause) {
  val colors = MaterialTheme.colorScheme
  val (container: Color, content: Color) = when (cause) {
    StretchCause.SleepTimerEnded, StretchCause.SleepTimerStarted -> colors.surfaceContainerHighest to colors.onSurfaceVariant
    is StretchCause.Jumped -> colors.secondaryContainer to colors.onSecondaryContainer
    is StretchCause.Played -> colors.primaryContainer to colors.onPrimaryContainer
  }
  Box(
    modifier = Modifier
      .size(40.dp)
      .background(color = container, shape = CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = cause.icon(),
      contentDescription = null,
      tint = content,
      modifier = Modifier.size(20.dp),
    )
  }
}

@Composable
private fun whereText(
  from: HistoryLocation,
  to: HistoryLocation?,
): String {
  val fromText = locationText(from)
  if (to == null) return fromText
  val toText = if (to.chapterNumber == from.chapterNumber) to.time else locationText(to)
  val arrow = if (LocalLayoutDirection.current == LayoutDirection.Rtl) "←" else "→"
  return "$fromText $arrow $toText"
}

@Composable
private fun stretchWhen(stretch: HistoryStretch): String {
  val end = stretch.end
  val listened = stretch.listened
  return if (end == null || listened == null) {
    timeText(stretch.start)
  } else {
    "${timeText(stretch.start)} – ${timeText(end)} · ${durationText(listened)}"
  }
}

@Composable
private fun causeText(cause: StretchCause): String? = when (cause) {
  is StretchCause.Played -> if (cause.source == Source.AudioFocus) {
    // playback going on after a call isn't interrupted, it's what follows the interruption
    stringResource(R.string.history_event_played)
  } else {
    withSource(stringResource(R.string.history_event_played), cause.source)
  }
  is StretchCause.Jumped -> if (cause.hasRow) null else jumpText(cause.type, cause.source)
  StretchCause.SleepTimerEnded -> stringResource(R.string.history_event_dozed_off)
  StretchCause.SleepTimerStarted -> stringResource(R.string.history_event_sleep_timer_set)
}

@Composable
private fun jumpText(
  type: Type,
  source: Source,
): String = withSource(
  stringResource(
    when (type) {
      Type.ChapterChange -> R.string.history_event_chapter_change
      Type.BookmarkJump -> R.string.history_event_bookmark_jump
      Type.JumpBack -> R.string.history_event_jump_back
      else -> R.string.history_event_moved
    },
  ),
  source,
)

@Composable
private fun withSource(
  text: String,
  source: Source,
): String = if (source == Source.App || source == Source.Unknown) text else "$text · ${sourceLabel(source)}"

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
private fun sourceLabel(source: Source): String = stringResource(
  when (source) {
    Source.App -> R.string.history_source_app
    Source.Widget -> R.string.history_source_widget
    Source.Notification -> R.string.history_source_notification
    Source.Headset -> R.string.history_source_headset
    Source.Bluetooth -> R.string.history_source_bluetooth
    Source.Car -> R.string.history_source_car
    Source.Watch -> R.string.history_source_watch
    Source.OtherApp -> R.string.history_source_other_app
    Source.AudioFocus -> R.string.history_source_audio_focus
    Source.Unplugged -> R.string.history_source_unplugged
    Source.SleepTimer -> R.string.history_source_sleep_timer
    Source.Unknown -> R.string.history_source_unknown
    Source.OtherDevice -> R.string.history_source_other_device
  },
)

private fun StretchCause.icon(): ImageVector = when (this) {
  StretchCause.SleepTimerEnded, StretchCause.SleepTimerStarted -> VoiceIcons.Bedtime
  is StretchCause.Jumped -> type.jumpIcon()
  is StretchCause.Played -> if (source == Source.AudioFocus) VoiceIcons.PlayArrow else source.icon()
}

private fun Type.jumpIcon(): ImageVector = when (this) {
  Type.ChapterChange -> VoiceIcons.SkipNext
  Type.BookmarkJump -> VoiceIcons.Bookmark
  Type.JumpBack -> VoiceIcons.Undo
  else -> VoiceIcons.SwapHoriz
}

private fun Source.icon(): ImageVector = when (this) {
  Source.App, Source.Unknown -> VoiceIcons.PlayArrow
  Source.Widget -> VoiceIcons.Widgets
  Source.Notification -> VoiceIcons.Notifications
  Source.Headset -> VoiceIcons.Headphones
  Source.Bluetooth -> VoiceIcons.Devices
  Source.Car -> VoiceIcons.DirectionsCar
  Source.Watch -> VoiceIcons.Watch
  Source.OtherApp -> VoiceIcons.Devices
  Source.AudioFocus -> VoiceIcons.Call
  Source.Unplugged -> VoiceIcons.Headphones
  Source.SleepTimer -> VoiceIcons.Bedtime
  Source.OtherDevice -> VoiceIcons.Devices
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
