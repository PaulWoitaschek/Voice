@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookmark

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import voice.core.data.Bookmark
import voice.core.strings.R
import voice.core.ui.BookmarkBadge
import voice.core.ui.MorphShape
import voice.core.ui.bookmarkStyle
import voice.core.ui.icons.VoiceIcons
import java.text.NumberFormat

/**
 * Details for a bookmark. A new bookmark is already saved when this opens, so swiping the sheet
 * away loses nothing.
 */
@Composable
internal fun BookmarkEditorSheet(
  editor: BookmarkEditorViewState,
  onNoteChange: (String) -> Unit,
  onKindChange: (Bookmark.Kind) -> Unit,
  onSleepKindClick: () -> Unit,
  onMoveEarlier: () -> Unit,
  onMoveLater: () -> Unit,
  onUndo: () -> Unit,
  onDelete: () -> Unit,
  onDone: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDone,
    sheetState = rememberBottomSheetState(
      initialValue = Hidden,
      enabledValues = setOf(Hidden, Expanded),
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
    ) {
      EditorHeader(editor)
      Spacer(Modifier.height(20.dp))
      Text(
        text = stringResource(if (editor.isNew) R.string.bookmark_nudge_new else R.string.bookmark_nudge_edit),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(8.dp))
      Nudge(
        time = editor.time,
        canMoveEarlier = editor.canMoveEarlier,
        canMoveLater = editor.canMoveLater,
        onMoveEarlier = onMoveEarlier,
        onMoveLater = onMoveLater,
      )
      Spacer(Modifier.height(16.dp))
      OutlinedTextField(
        value = editor.note,
        onValueChange = onNoteChange,
        label = { Text(stringResource(R.string.bookmark_note_label)) },
        maxLines = 3,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(20.dp))
      KindPicker(
        editor = editor,
        onKindChange = onKindChange,
        onSleepKindClick = onSleepKindClick,
      )
      Spacer(Modifier.height(24.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (editor.isNew) {
          TextButton(onClick = onUndo) {
            Icon(VoiceIcons.Undo, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.common_action_undo))
          }
        } else {
          TextButton(onClick = onDelete) {
            Icon(VoiceIcons.Delete, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.common_action_delete))
          }
        }
        Spacer(Modifier.weight(1F))
        Button(onClick = onDone, shapes = ButtonDefaults.shapes()) {
          Icon(VoiceIcons.Check, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
          Spacer(Modifier.width(ButtonDefaults.IconSpacing))
          Text(stringResource(R.string.common_action_done))
        }
      }
    }
  }
}

@Composable
private fun EditorHeader(editor: BookmarkEditorViewState) {
  val colors = MaterialTheme.colorScheme
  val percentFormat = remember { NumberFormat.getPercentInstance() }
  Row(verticalAlignment = Alignment.CenterVertically) {
    if (editor.isNew) {
      // the burst from the player's long press, popping in
      val pop = remember { Animatable(0F) }
      LaunchedEffect(Unit) {
        pop.animateTo(1F, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow))
      }
      Box(
        modifier = Modifier
          .size(56.dp)
          .graphicsLayer {
            scaleX = pop.value
            scaleY = pop.value
            rotationZ = (1F - pop.value) * -90F
          }
          .background(colors.primary, MaterialShapes.SoftBurst.toShape()),
        contentAlignment = Alignment.Center,
      ) {
        Icon(VoiceIcons.Check, contentDescription = null, tint = colors.onPrimary)
      }
    } else {
      BookmarkBadge(kind = editor.kind, setBySleepTimer = editor.setBySleepTimer, size = 56.dp)
    }
    Spacer(Modifier.width(16.dp))
    Column {
      Text(
        text = stringResource(if (editor.isNew) R.string.bookmark_saved else R.string.bookmark_edit_title),
        style = MaterialTheme.typography.headlineSmallEmphasized,
      )
      val chapter = if (editor.showChapter) chapterLabel(editor.chapterNumber, editor.chapterName) else null
      Text(
        text = listOfNotNull(chapter, editor.time, percentFormat.format(editor.percent / 100.0)).joinToString(" · "),
        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
        color = colors.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun Nudge(
  time: String,
  canMoveEarlier: Boolean,
  canMoveLater: Boolean,
  onMoveEarlier: () -> Unit,
  onMoveLater: () -> Unit,
) {
  val earlier = stringResource(R.string.bookmark_nudge_earlier)
  val later = stringResource(R.string.bookmark_nudge_later)
  Row(verticalAlignment = Alignment.CenterVertically) {
    FilledTonalButton(
      onClick = onMoveEarlier,
      enabled = canMoveEarlier,
      shapes = ButtonDefaults.shapes(),
      modifier = Modifier.semantics { contentDescription = earlier },
    ) {
      Text("−15 s")
    }
    Text(
      text = time,
      style = MaterialTheme.typography.titleLargeEmphasized.copy(fontFeatureSettings = "tnum"),
      modifier = Modifier.weight(1F),
      textAlign = TextAlign.Center,
    )
    FilledTonalButton(
      onClick = onMoveLater,
      enabled = canMoveLater,
      shapes = ButtonDefaults.shapes(),
      modifier = Modifier.semantics { contentDescription = later },
    ) {
      Text("+15 s")
    }
  }
}

@Composable
private fun KindPicker(
  editor: BookmarkEditorViewState,
  onKindChange: (Bookmark.Kind) -> Unit,
  onSleepKindClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .selectableGroup(),
    horizontalArrangement = Arrangement.SpaceEvenly,
  ) {
    if (editor.wasSetBySleepTimer) {
      KindTile(
        kind = Bookmark.Kind.Note,
        setBySleepTimer = true,
        selected = editor.setBySleepTimer,
        onClick = onSleepKindClick,
      )
    }
    Bookmark.Kind.entries.forEach { kind ->
      KindTile(
        kind = kind,
        setBySleepTimer = false,
        selected = !editor.setBySleepTimer && editor.kind == kind,
        onClick = { onKindChange(kind) },
      )
    }
  }
}

/** Picking a kind morphs its circle into the kind's shape and rings it. */
@Composable
private fun KindTile(
  kind: Bookmark.Kind,
  setBySleepTimer: Boolean,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  val style = bookmarkStyle(kind, setBySleepTimer)
  val morph = remember(style.polygon) { Morph(MaterialShapes.Circle, style.polygon) }
  val progress by animateFloatAsState(
    targetValue = if (selected) 1F else 0F,
    animationSpec = spring(dampingRatio = 0.5F, stiffness = Spring.StiffnessMediumLow),
    label = "kindMorph",
  )
  Column(
    modifier = Modifier
      .clip(MaterialTheme.shapes.large)
      .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
      .padding(4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier
        .size(64.dp)
        .border(
          width = 2.dp,
          color = if (selected) colors.primary else Color.Transparent,
          shape = CircleShape,
        )
        .padding(6.dp),
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier = Modifier
          .size(52.dp)
          .graphicsLayer { rotationZ = progress * 20F }
          .clip(MorphShape(morph, progress))
          .background(if (selected) style.container else colors.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
      ) {
        val icon = style.icon ?: VoiceIcons.Favorite
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (selected) style.content else colors.onSurfaceVariant,
          modifier = Modifier
            .size(24.dp)
            .graphicsLayer { rotationZ = progress * -20F },
        )
      }
    }
    Spacer(Modifier.height(4.dp))
    Text(
      text = kindLabel(kind, setBySleepTimer),
      style = MaterialTheme.typography.labelMedium,
      color = if (selected) colors.onSurface else colors.onSurfaceVariant,
    )
  }
}
