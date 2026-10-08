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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import voice.core.data.ListeningEvent.Source
import voice.core.data.ListeningEvent.Type
import voice.core.strings.R
import voice.core.ui.BookmarkBadge
import voice.core.ui.formatTime
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.segmentedShape
import voice.features.bookmark.dayLabelText
import voice.features.bookmark.durationText
import voice.features.bookmark.timeText
import java.text.NumberFormat
import java.time.LocalTime
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun HistoryList(
  viewState: HistoryViewState,
  contentPadding: PaddingValues,
  onFilterClick: (HistoryFilter?) -> Unit,
  onSourceChange: (Source?) -> Unit,
  onActionClick: (HistoryAction) -> Unit,
  onSuggestionBack: (HistorySuggestion) -> Unit,
  onSuggestionKeep: (HistorySuggestion) -> Unit,
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
    val suggestion = viewState.suggestion
    if (suggestion != null) {
      item(key = "suggestion") {
        SuggestionCard(
          suggestion = suggestion,
          onBack = { onSuggestionBack(suggestion) },
          onKeep = { onSuggestionKeep(suggestion) },
          modifier = Modifier
            .animateItem()
            .padding(bottom = 16.dp),
        )
      }
    }
    if (viewState.hasEvents) {
      item(key = "filters") {
        HistoryFilters(
          filters = viewState.filters,
          selected = viewState.selectedFilter,
          sources = viewState.sources,
          selectedSource = viewState.selectedSource,
          onFilterClick = onFilterClick,
          onSourceChange = onSourceChange,
        )
      }
    } else {
      item(key = "empty") {
        HistoryEmpty(Modifier.padding(top = 32.dp))
      }
    }
    if (viewState.hasEvents && viewState.sessions.isEmpty()) {
      item(key = "filtered-empty") {
        Text(
          text = stringResource(R.string.history_empty_filtered),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        )
      }
    }
    viewState.sessions.forEach { session ->
      item(key = "session-${session.key}") {
        SessionHeader(session, Modifier.animateItem())
      }
      itemsIndexed(session.entries, key = { _, entry -> "entry-${entry.key}" }) { entryIndex, entry ->
        HistoryRow(
          entry = entry,
          shape = segmentedShape(entryIndex, session.entries.size),
          onActionClick = onActionClick,
          modifier = Modifier.animateItem(),
        )
      }
    }
  }
}

@Composable
private fun SuggestionCard(
  suggestion: HistorySuggestion,
  onBack: () -> Unit,
  onKeep: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Surface(
    color = colors.tertiaryContainer,
    contentColor = colors.onTertiaryContainer,
    shape = RoundedCornerShape(28.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(20.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .background(colors.tertiary, MaterialShapes.Cookie9Sided.toShape()),
          contentAlignment = Alignment.Center,
        ) {
          val icon = when (suggestion) {
            is HistorySuggestion.StartedBy -> suggestion.source.icon()
            is HistorySuggestion.Jumped -> VoiceIcons.SwapHoriz
          }
          Icon(icon, contentDescription = null, tint = colors.onTertiary)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1F)) {
          val (title, message) = when (suggestion) {
            is HistorySuggestion.StartedBy -> {
              startedByTitle(suggestion.source, timeText(suggestion.at)) to
                stringResource(R.string.history_suggestion_played, durationText(suggestion.played))
            }
            is HistorySuggestion.Jumped -> {
              stringResource(R.string.history_suggestion_jumped_title) to
                stringResource(R.string.history_suggestion_jumped_message, timeText(suggestion.at))
            }
          }
          Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
          Text(message, style = MaterialTheme.typography.bodyMedium)
        }
      }
      Spacer(Modifier.height(16.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(onClick = onKeep) {
          Text(stringResource(R.string.history_suggestion_keep))
        }
        Spacer(Modifier.width(8.dp))
        Button(
          onClick = onBack,
          shapes = ButtonDefaults.shapes(),
          colors = ButtonDefaults.buttonColors(containerColor = colors.tertiary, contentColor = colors.onTertiary),
        ) {
          Icon(VoiceIcons.Undo, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
          Spacer(Modifier.width(ButtonDefaults.IconSpacing))
          Text(backToText(suggestion.back.location))
        }
      }
    }
  }
}

@Composable
private fun startedByTitle(
  source: Source,
  time: String,
): String = stringResource(
  when (source) {
    Source.Car -> R.string.history_suggestion_started_by_car
    Source.Watch -> R.string.history_suggestion_started_by_watch
    Source.Headset -> R.string.history_suggestion_started_by_headset
    Source.Bluetooth -> R.string.history_suggestion_started_by_bluetooth
    else -> R.string.history_suggestion_started_by_other_app
  },
  time,
)

@Composable
private fun backToText(location: HistoryLocation): String {
  val chapterNumber = location.chapterNumber
  return if (chapterNumber != null) {
    stringResource(R.string.playback_jump_back_chapter, chapterNumber, location.time)
  } else {
    stringResource(R.string.playback_jump_back, location.time)
  }
}

@Composable
private fun HistoryFilters(
  filters: List<HistoryFilter>,
  selected: HistoryFilter?,
  sources: List<Source>,
  selectedSource: Source?,
  onFilterClick: (HistoryFilter?) -> Unit,
  onSourceChange: (Source?) -> Unit,
) {
  LazyRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(bottom = 8.dp),
  ) {
    if (filters.size >= 2) {
      item(key = "all") {
        FilterChip(
          selected = selected == null,
          onClick = { onFilterClick(null) },
          label = { Text(stringResource(R.string.history_filter_all)) },
        )
      }
      items(filters, key = { it }) { filter ->
        FilterChip(
          selected = selected == filter,
          onClick = { onFilterClick(filter) },
          label = { Text(filterLabel(filter)) },
          leadingIcon = {
            Icon(filter.icon(), contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
          },
        )
      }
    }
    if (sources.size >= 2) {
      item(key = "source") {
        SourcePicker(sources = sources, selected = selectedSource, onChange = onSourceChange)
      }
    }
  }
}

@Composable
private fun SourcePicker(
  sources: List<Source>,
  selected: Source?,
  onChange: (Source?) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    FilterChip(
      selected = selected != null,
      onClick = { expanded = true },
      label = { Text(if (selected == null) stringResource(R.string.history_filter_any_source) else sourceLabel(selected)) },
      leadingIcon = selected?.let {
        { Icon(it.icon(), contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
      },
      trailingIcon = {
        Icon(VoiceIcons.ArrowDropDown, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
      },
    )
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      DropdownMenuItem(
        text = { Text(stringResource(R.string.history_filter_any_source)) },
        onClick = {
          expanded = false
          onChange(null)
        },
      )
      sources.forEach { source ->
        DropdownMenuItem(
          text = { Text(sourceLabel(source)) },
          leadingIcon = { Icon(source.icon(), contentDescription = null) },
          onClick = {
            expanded = false
            onChange(source)
          },
        )
      }
    }
  }
}

@Composable
private fun SessionHeader(
  session: HistorySession,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(
      Modifier
        .weight(1F)
        .semantics(mergeDescendants = true) { heading() },
    ) {
      Text(
        text = dayLabelText(session.label),
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = colors.primary,
      )
      Text(
        text = "${timeText(session.start)} – ${timeText(session.end)} · ${durationText(session.listened)}",
        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
        color = colors.onSurfaceVariant,
      )
    }
    Spacer(Modifier.width(12.dp))
    SessionBar(session.barStart, session.barEnd)
  }
}

/** Which stretch of the book a session covered. */
@Composable
private fun SessionBar(
  start: Float,
  end: Float,
) {
  val colors = MaterialTheme.colorScheme
  Spacer(
    Modifier
      .width(72.dp)
      .height(6.dp)
      .drawBehind {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(color = colors.primary.copy(alpha = 0.18F), cornerRadius = radius)
        val left = start.coerceIn(0F, 1F) * size.width
        val width = ((end - start).coerceIn(0F, 1F) * size.width).coerceAtLeast(size.height)
        drawRoundRect(
          color = colors.primary,
          topLeft = Offset(left.coerceAtMost(size.width - width), 0F),
          size = Size(width, size.height),
          cornerRadius = radius,
        )
      },
  )
}

@Composable
private fun HistoryRow(
  entry: HistoryEntry,
  shape: Shape,
  onActionClick: (HistoryAction) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Surface(
    shape = shape,
    color = colors.surfaceContainerHigh,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      EntryTime(entry.at)
      EntryBadge(entry)
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1F)) {
        Text(
          text = entryTitle(entry),
          style = MaterialTheme.typography.bodyLarge,
          color = colors.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        val where = entryWhere(entry)
        if (where != null) {
          Text(
            text = where,
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      val action = entry.action
      if (action != null) {
        EntryAction(action, onClick = { onActionClick(action) })
      } else {
        Spacer(Modifier.width(12.dp))
      }
    }
  }
}

/**
 * Sized to the widest time of the day, so the rows line up in 24-hour and 12-hour formats.
 */
@Composable
private fun EntryTime(time: LocalTime) {
  val style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
  Box(Modifier.padding(end = 8.dp)) {
    Text(
      text = timeText(LocalTime.of(22, 58)),
      style = style,
      maxLines = 1,
      modifier = Modifier
        .alpha(0F)
        .clearAndSetSemantics {},
    )
    Text(
      text = timeText(time),
      style = style,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
    )
  }
}

@Composable
private fun EntryBadge(entry: HistoryEntry) {
  val colors = MaterialTheme.colorScheme
  val detail = entry.detail
  if (detail is HistoryDetail.BookmarkInfo) {
    BookmarkBadge(kind = detail.kind, setBySleepTimer = detail.setBySleepTimer, size = 36.dp)
    return
  }
  val sleepy = entry.type == Type.SleepTimerEnded
  Box(
    modifier = Modifier
      .size(36.dp)
      .background(
        color = when {
          entry.lastTouch -> colors.tertiaryContainer
          sleepy -> colors.surfaceContainerHighest
          entry.type in jumpRowTypes -> colors.secondaryContainer
          else -> colors.primaryContainer
        },
        shape = CircleShape,
      ),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = entry.icon(),
      contentDescription = null,
      tint = when {
        entry.lastTouch -> colors.onTertiaryContainer
        sleepy -> colors.onSurfaceVariant
        entry.type in jumpRowTypes -> colors.onSecondaryContainer
        else -> colors.onPrimaryContainer
      },
      modifier = Modifier.size(20.dp),
    )
  }
}

private val jumpRowTypes = setOf(Type.Seek, Type.SkipBack, Type.SkipForward, Type.ChapterChange, Type.BookmarkJump, Type.JumpBack)

@Composable
private fun EntryAction(
  action: HistoryAction,
  onClick: () -> Unit,
) {
  when (action) {
    is HistoryAction.GoThere -> TextButton(onClick = onClick) {
      Text(stringResource(R.string.history_action_go_there))
    }
    is HistoryAction.JumpBack -> FilledTonalIconButton(onClick = onClick) {
      Icon(VoiceIcons.Undo, contentDescription = backToText(action.location))
    }
    is HistoryAction.Pin -> IconButton(onClick = onClick) {
      Icon(VoiceIcons.PushPin, contentDescription = stringResource(R.string.history_action_pin))
    }
    is HistoryAction.Restore -> TextButton(onClick = onClick) {
      Text(stringResource(R.string.history_action_restore))
    }
    is HistoryAction.ChangeBack -> TextButton(onClick = onClick) {
      Text(stringResource(R.string.history_action_change_back))
    }
  }
}

@Composable
private fun entryTitle(entry: HistoryEntry): String {
  val detail = entry.detail
  val count = if (entry.count > 1) " ×${entry.count}" else ""
  val base = when (entry.type) {
    Type.Play -> withSource(stringResource(R.string.history_event_played), entry.source)
    Type.Pause -> {
      val paused = if (detail is HistoryDetail.PausedFor) {
        stringResource(R.string.history_event_paused_for, durationText(detail.duration))
      } else {
        stringResource(R.string.history_event_paused)
      }
      withSource(paused, entry.source)
    }
    Type.Seek -> stringResource(R.string.history_event_moved) + count
    Type.SkipBack -> stringResource(R.string.history_event_skipped_back) + count
    Type.SkipForward -> stringResource(R.string.history_event_skipped_forward) + count
    Type.ChapterChange -> stringResource(R.string.history_event_chapter_change) + count
    Type.BookmarkJump -> stringResource(R.string.history_event_bookmark_jump)
    Type.JumpBack -> stringResource(R.string.history_event_jump_back)
    Type.SpeedChanged -> {
      val speed = detail as? HistoryDetail.Speed
      stringResource(R.string.history_event_speed, change(speed?.from, speed?.to, ::speedText))
    }
    Type.VolumeBoostChanged -> {
      val boost = detail as? HistoryDetail.VolumeBoost
      stringResource(R.string.history_event_volume_boost, change(boost?.from, boost?.to, ::decibelText))
    }
    Type.SkipSilenceChanged -> stringResource(
      if ((detail as? HistoryDetail.SkipSilence)?.enabled == false) {
        R.string.history_event_skip_silence_off
      } else {
        R.string.history_event_skip_silence_on
      },
    )
    Type.SleepTimerSet -> {
      val minutes = (detail as? HistoryDetail.SleepTimer)?.minutes
      val duration = if (minutes != null) {
        durationText(minutes.minutes)
      } else {
        stringResource(R.string.sleep_timer_end_of_chapter)
      }
      "${stringResource(R.string.history_event_sleep_timer_set)} · $duration"
    }
    Type.SleepTimerEnded -> stringResource(R.string.history_event_dozed_off)
    Type.SleepTimerExtended -> stringResource(R.string.history_event_sleep_timer_extended)
    Type.BookmarkAdded, Type.BookmarkDeleted -> {
      val added = entry.type == Type.BookmarkAdded
      val note = (detail as? HistoryDetail.BookmarkInfo)?.note
      when {
        note != null && added -> stringResource(R.string.history_event_bookmark_added_note, note)
        note != null -> stringResource(R.string.history_event_bookmark_deleted_note, note)
        added -> stringResource(R.string.history_event_bookmark_added)
        else -> stringResource(R.string.history_event_bookmark_deleted)
      }
    }
  }
  return if (entry.lastTouch) "${stringResource(R.string.history_event_last_touch)} · $base" else base
}

@Composable
private fun withSource(
  text: String,
  source: Source,
): String = if (source == Source.App || source == Source.Unknown) text else "$text · ${sourceLabel(source)}"

private fun <T : Any> change(
  from: T?,
  to: T?,
  format: (T) -> String,
): String = when {
  to == null -> ""
  from == null -> format(to)
  else -> "${format(from)} → ${format(to)}"
}

private fun speedText(speed: Float): String {
  val format = NumberFormat.getNumberInstance().apply {
    minimumFractionDigits = 1
    maximumFractionDigits = 2
  }
  return "${format.format(speed)}×"
}

private fun decibelText(gain: Float): String {
  val format = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }
  val sign = if (gain > 0F) "+" else ""
  return "$sign${format.format(gain)} dB"
}

@Composable
private fun entryWhere(entry: HistoryEntry): String? {
  val where = entry.where?.let { locationText(it) } ?: return null
  val to = entry.to
  val detail = entry.detail
  return when {
    to != null && entry.type in jumpRowTypes -> {
      val toText = if (to.chapterNumber == entry.where.chapterNumber) to.time else locationText(to)
      "$where → $toText"
    }
    detail is HistoryDetail.Skipped -> {
      val sign = if (entry.type == Type.SkipBack) "−" else "+"
      "$where · $sign${formatTime(detail.seconds.absoluteValue * 1000L)}"
    }
    else -> where
  }
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
private fun filterLabel(filter: HistoryFilter): String = stringResource(
  when (filter) {
    HistoryFilter.Jumps -> R.string.history_filter_jumps
    HistoryFilter.PlayPause -> R.string.history_filter_play_pause
    HistoryFilter.Sleep -> R.string.history_filter_sleep
    HistoryFilter.Bookmarks -> R.string.history_filter_bookmarks
    HistoryFilter.Settings -> R.string.history_filter_settings
  },
)

private fun HistoryFilter.icon(): ImageVector = when (this) {
  HistoryFilter.Jumps -> VoiceIcons.SwapHoriz
  HistoryFilter.PlayPause -> VoiceIcons.PlayArrow
  HistoryFilter.Sleep -> VoiceIcons.Bedtime
  HistoryFilter.Bookmarks -> VoiceIcons.Bookmark
  HistoryFilter.Settings -> VoiceIcons.Speed
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
  },
)

private fun Source.icon(): ImageVector = when (this) {
  Source.App -> VoiceIcons.Smartphone
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
  Source.Unknown -> VoiceIcons.Help
}

private fun HistoryEntry.icon(): ImageVector {
  if (lastTouch) return VoiceIcons.TouchApp
  return when (type) {
    Type.Play -> if (source == Source.App || source == Source.Unknown) VoiceIcons.PlayArrow else source.icon()
    Type.Pause -> if (source == Source.App || source == Source.Unknown) VoiceIcons.Pause else source.icon()
    Type.Seek -> VoiceIcons.SwapHoriz
    Type.SkipBack -> VoiceIcons.FastRewind
    Type.SkipForward -> VoiceIcons.FastForward
    Type.ChapterChange -> VoiceIcons.SkipNext
    Type.BookmarkJump -> VoiceIcons.Bookmark
    Type.JumpBack -> VoiceIcons.Undo
    Type.SpeedChanged -> VoiceIcons.Speed
    Type.SkipSilenceChanged -> VoiceIcons.ContentCut
    Type.VolumeBoostChanged -> VoiceIcons.VolumeUp
    Type.SleepTimerSet, Type.SleepTimerExtended -> VoiceIcons.Timer
    Type.SleepTimerEnded -> VoiceIcons.Bedtime
    Type.BookmarkAdded -> VoiceIcons.BookmarkAdd
    Type.BookmarkDeleted -> VoiceIcons.Delete
  }
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
