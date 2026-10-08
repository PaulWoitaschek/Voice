@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.bottomSheet

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import voice.core.ui.MorphShape
import voice.core.ui.ShapedIcon
import voice.core.ui.drawConfetti
import voice.core.ui.entrance
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.rememberConfettiState
import voice.core.ui.rememberCoverThumbnailRequest
import voice.core.ui.rememberEntranceState
import voice.core.ui.segmentedShape
import voice.features.bookOverview.overview.BookOverviewCategory
import voice.features.bookOverview.overview.BookOverviewItemViewState
import voice.features.bookOverview.views.CoverShape
import java.text.NumberFormat
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR

private val EditItems = listOf(BottomSheetItem.Title, BottomSheetItem.InternetCover, BottomSheetItem.FileCover)

/** The order a book moves through, so the picker reads like a timeline. */
private val StatusOrder = listOf(BookOverviewCategory.NOT_STARTED, BookOverviewCategory.CURRENT, BookOverviewCategory.FINISHED)

@Composable
internal fun BookActionsContent(
  book: BookOverviewItemViewState,
  category: BookOverviewCategory?,
  items: List<BottomSheetItem>,
  onItemClick: (BottomSheetItem) -> Unit,
  onStatusChange: (BottomSheetItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  val entrance = rememberEntranceState()
  Column(
    modifier = modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp)
      .padding(bottom = 24.dp),
  ) {
    BookHeader(
      book = book,
      modifier = Modifier
        .entrance(entrance, 0)
        .padding(horizontal = 8.dp),
    )
    if (category != null) {
      Spacer(Modifier.height(24.dp))
      StatusPicker(
        current = category,
        onSelect = { selected -> onStatusChange(selected.markAsItem()) },
        modifier = Modifier.entrance(entrance, 1),
      )
    }
    val edits = EditItems.filter { it in items }
    if (edits.isNotEmpty()) {
      Spacer(Modifier.height(20.dp))
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        edits.forEachIndexed { index, item ->
          ActionRow(
            item = item,
            style = item.style(),
            shape = segmentedShape(index, edits.size),
            onClick = { onItemClick(item) },
            modifier = Modifier.entrance(entrance, 2 + index),
          )
        }
      }
    }
    if (BottomSheetItem.DeleteBook in items) {
      Spacer(Modifier.height(12.dp))
      ActionRow(
        item = BottomSheetItem.DeleteBook,
        style = BottomSheetItem.DeleteBook.style(),
        shape = RoundedCornerShape(24.dp),
        onClick = { onItemClick(BottomSheetItem.DeleteBook) },
        modifier = Modifier.entrance(entrance, 2 + edits.size),
        titleColor = MaterialTheme.colorScheme.error,
      )
    }
  }
}

@Composable
private fun BookHeader(
  book: BookOverviewItemViewState,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val percentFormat = remember { NumberFormat.getPercentInstance() }
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    AsyncImage(
      modifier = Modifier
        .size(88.dp)
        .clip(CoverShape),
      model = rememberCoverThumbnailRequest(book.cover),
      placeholder = ColorPainter(colors.surfaceContainerHighest),
      error = painterResource(id = UiR.drawable.album_art),
      contentScale = ContentScale.Crop,
      contentDescription = null,
    )
    Spacer(Modifier.width(16.dp))
    Column(Modifier.weight(1F)) {
      Text(
        text = book.name,
        style = MaterialTheme.typography.titleLargeEmphasized,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      if (book.author != null) {
        Text(
          text = book.author,
          style = MaterialTheme.typography.bodyMedium,
          color = colors.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(Modifier.height(10.dp))
      LinearWavyProgressIndicator(
        progress = { book.progress },
        modifier = Modifier.fillMaxWidth(),
        color = colors.primary,
        trackColor = colors.primary.copy(alpha = 0.2F),
        amplitude = { 0F },
      )
      Spacer(Modifier.height(6.dp))
      Row {
        Text(
          modifier = Modifier.weight(1F),
          text = stringResource(StringsR.string.playback_book_progress_read, percentFormat.format(book.progress.toDouble())),
          style = MaterialTheme.typography.labelMediumEmphasized,
          color = colors.primary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = stringResource(StringsR.string.playback_book_progress_remaining, book.remainingTime),
          style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
          color = colors.onSurfaceVariant,
          maxLines = 1,
          softWrap = false,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

private class StatusStyle(
  val icon: ImageVector,
  val shape: RoundedPolygon,
  val tile: Color,
  val blob: Color,
  val onBlob: Color,
)

@Composable
private fun BookOverviewCategory.style(): StatusStyle {
  val colors = MaterialTheme.colorScheme
  return when (this) {
    BookOverviewCategory.NOT_STARTED -> StatusStyle(
      icon = VoiceIcons.HourglassEmpty,
      shape = MaterialShapes.Cookie6Sided,
      tile = colors.secondaryContainer,
      blob = colors.secondary,
      onBlob = colors.onSecondary,
    )
    BookOverviewCategory.CURRENT -> StatusStyle(
      icon = VoiceIcons.Headphones,
      shape = MaterialShapes.Sunny,
      tile = colors.primaryContainer,
      blob = colors.primary,
      onBlob = colors.onPrimary,
    )
    BookOverviewCategory.FINISHED -> StatusStyle(
      icon = VoiceIcons.Check,
      shape = MaterialShapes.SoftBurst,
      tile = colors.tertiaryContainer,
      blob = colors.tertiary,
      onBlob = colors.onTertiary,
    )
  }
}

private fun BookOverviewCategory.markAsItem(): BottomSheetItem = when (this) {
  BookOverviewCategory.NOT_STARTED -> BottomSheetItem.BookCategoryMarkAsNotStarted
  BookOverviewCategory.CURRENT -> BottomSheetItem.BookCategoryMarkAsCurrent
  BookOverviewCategory.FINISHED -> BottomSheetItem.BookCategoryMarkAsCompleted
}

@Composable
private fun StatusPicker(
  current: BookOverviewCategory,
  onSelect: (BookOverviewCategory) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selected by remember(current) { mutableStateOf(current) }
  val haptics = LocalHapticFeedback.current
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(IntrinsicSize.Min)
      .selectableGroup(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    StatusOrder.forEach { category ->
      StatusTile(
        category = category,
        selected = category == selected,
        onClick = {
          if (category != selected) {
            selected = category
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            onSelect(category)
          }
        },
        modifier = Modifier
          .weight(1F)
          .fillMaxHeight(),
      )
    }
  }
}

@Composable
private fun StatusTile(
  category: BookOverviewCategory,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val style = category.style()
  val morph = remember(style.shape) { Morph(MaterialShapes.Circle, style.shape) }
  val progress by animateFloatAsState(
    targetValue = if (selected) 1F else 0F,
    animationSpec = spring(dampingRatio = 0.5F, stiffness = Spring.StiffnessMediumLow),
    label = "statusMorph",
  )
  val tileColor by animateColorAsState(if (selected) style.tile else colors.surfaceContainerHigh, label = "statusTile")
  val blobColor by animateColorAsState(if (selected) style.blob else colors.surfaceContainerHighest, label = "statusBlob")
  val iconColor by animateColorAsState(if (selected) style.onBlob else colors.onSurfaceVariant, label = "statusIcon")
  val confetti = rememberConfettiState()
  val confettiColors = listOf(colors.primary, colors.secondary, colors.tertiary, colors.tertiaryContainer)
  val squish = remember { Animatable(1F) }
  val scope = rememberCoroutineScope()
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val pressScale by animateFloatAsState(
    targetValue = if (pressed) 0.94F else 1F,
    animationSpec = spring(dampingRatio = 0.5F, stiffness = Spring.StiffnessMedium),
    label = "statusPress",
  )
  Column(
    modifier = modifier
      .graphicsLayer {
        scaleX = pressScale * squish.value
        scaleY = pressScale * squish.value
      }
      .background(tileColor, RoundedCornerShape(24.dp))
      // unclipped, so the confetti of a finished book can fly beyond the tile. The squish is the feedback.
      .selectable(
        selected = selected,
        interactionSource = interactionSource,
        indication = null,
        role = Role.RadioButton,
        onClick = {
          onClick()
          if (category == BookOverviewCategory.FINISHED) confetti.burst()
          scope.launch {
            squish.snapTo(0.9F)
            squish.animateTo(1F, spring(dampingRatio = 0.3F, stiffness = Spring.StiffnessMedium))
          }
        },
      )
      .padding(horizontal = 8.dp, vertical = 14.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier
        .size(52.dp)
        .drawWithContent {
          drawContent()
          drawConfetti(
            state = confetti,
            center = center,
            radius = size.minDimension / 2,
            colors = confettiColors,
          )
        },
      contentAlignment = Alignment.Center,
    ) {
      Box(
        Modifier
          .matchParentSize()
          .graphicsLayer { rotationZ = progress * 30F }
          .background(blobColor, MorphShape(morph, progress)),
      )
      Icon(
        imageVector = style.icon,
        contentDescription = null,
        tint = iconColor,
        modifier = Modifier.size(24.dp),
      )
    }
    Spacer(Modifier.height(8.dp))
    Text(
      text = stringResource(category.nameRes),
      style = if (selected) MaterialTheme.typography.labelLargeEmphasized else MaterialTheme.typography.labelLarge,
      color = if (selected) colors.onSurface else colors.onSurfaceVariant,
      textAlign = TextAlign.Center,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

private class ActionStyle(
  val shape: RoundedPolygon,
  val container: Color,
  val content: Color,
)

@Composable
private fun BottomSheetItem.style(): ActionStyle {
  val colors = MaterialTheme.colorScheme
  return when (this) {
    BottomSheetItem.Title -> ActionStyle(MaterialShapes.Pentagon, colors.secondaryContainer, colors.onSecondaryContainer)
    BottomSheetItem.InternetCover -> ActionStyle(MaterialShapes.Clover4Leaf, colors.tertiaryContainer, colors.onTertiaryContainer)
    BottomSheetItem.FileCover -> ActionStyle(MaterialShapes.Cookie4Sided, colors.primaryContainer, colors.onPrimaryContainer)
    BottomSheetItem.DeleteBook -> ActionStyle(MaterialShapes.Cookie9Sided, colors.errorContainer, colors.onErrorContainer)
    BottomSheetItem.BookCategoryMarkAsNotStarted,
    BottomSheetItem.BookCategoryMarkAsCurrent,
    BottomSheetItem.BookCategoryMarkAsCompleted,
    -> ActionStyle(MaterialShapes.Circle, colors.surfaceContainerHighest, colors.onSurfaceVariant)
  }
}

@Composable
private fun ActionRow(
  item: BottomSheetItem,
  style: ActionStyle,
  shape: RoundedCornerShape,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  titleColor: Color = MaterialTheme.colorScheme.onSurface,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val rotation by animateFloatAsState(
    targetValue = if (pressed) 45F else 0F,
    animationSpec = spring(dampingRatio = 0.4F, stiffness = Spring.StiffnessMediumLow),
    label = "actionIconRotation",
  )
  Surface(
    onClick = onClick,
    modifier = modifier.fillMaxWidth(),
    shape = shape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    interactionSource = interactionSource,
  ) {
    Row(
      modifier = Modifier
        .heightIn(min = 68.dp)
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ShapedIcon(
        icon = item.icon,
        shape = style.shape,
        containerColor = style.container,
        contentColor = style.content,
        size = 44.dp,
        shapeRotation = rotation,
      )
      Spacer(Modifier.width(16.dp))
      Text(
        modifier = Modifier.weight(1F),
        text = stringResource(item.titleRes),
        style = MaterialTheme.typography.titleMedium.copy(hyphens = Hyphens.Auto),
        color = titleColor,
      )
      Spacer(Modifier.width(8.dp))
      Icon(
        imageVector = VoiceIcons.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
