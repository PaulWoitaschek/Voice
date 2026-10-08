@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.deleteBook

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import voice.core.ui.MorphShape
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.rememberCoverThumbnailRequest
import voice.features.bookOverview.views.CoverShape
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR

@Composable
internal fun DeleteBookSheet(
  viewState: DeleteBookViewState,
  onDismiss: () -> Unit,
  onConfirmDeletion: () -> Unit,
  onDeleteCheckBoxCheck: (Boolean) -> Unit,
) {
  val sheetState = rememberBottomSheetState(
    initialValue = Hidden,
    enabledValues = setOf(Hidden, Expanded),
  )
  val scope = rememberCoroutineScope()
  val hideThen: (() -> Unit) -> Unit = { action ->
    scope.launch {
      try {
        sheetState.hide()
      } finally {
        action()
      }
    }
  }
  val armed = viewState.deleteCheckBoxChecked
  val colors = MaterialTheme.colorScheme
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
        .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      DeleteHero(cover = viewState.cover, armed = armed)
      Spacer(Modifier.height(20.dp))
      Text(
        text = stringResource(StringsR.string.book_delete_dialog_title),
        style = MaterialTheme.typography.headlineSmallEmphasized,
        textAlign = TextAlign.Center,
      )
      if (viewState.name != null) {
        Spacer(Modifier.height(4.dp))
        Text(
          text = viewState.name,
          style = MaterialTheme.typography.titleMedium,
          textAlign = TextAlign.Center,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(Modifier.height(12.dp))
      Text(
        text = stringResource(StringsR.string.book_delete_dialog_message),
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(12.dp))
      Text(
        modifier = Modifier
          .background(colors.surfaceContainerHigh, RoundedCornerShape(12.dp))
          .padding(horizontal = 12.dp, vertical = 8.dp),
        text = viewState.fileToDelete,
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(Modifier.height(20.dp))
      val confirmContainer by animateColorAsState(
        targetValue = if (armed) colors.errorContainer else colors.surfaceContainerHigh,
        label = "confirmContainer",
      )
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(confirmContainer)
          .toggleable(value = armed, role = Role.Checkbox, onValueChange = onDeleteCheckBoxCheck)
          .heightIn(min = 56.dp)
          .padding(start = 4.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Checkbox(
          checked = armed,
          onCheckedChange = null,
          modifier = Modifier.padding(12.dp),
          colors = CheckboxDefaults.colors(
            checkedColor = colors.error,
            checkmarkColor = colors.onError,
          ),
        )
        Text(
          text = stringResource(StringsR.string.book_delete_dialog_confirm_files),
          style = MaterialTheme.typography.titleSmall,
          color = if (armed) colors.onErrorContainer else colors.onSurface,
        )
      }
      Spacer(Modifier.height(24.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = { hideThen(onDismiss) },
          shapes = ButtonDefaults.shapes(),
          modifier = Modifier
            .weight(1F)
            .heightIn(min = ButtonDefaults.MediumContainerHeight),
        ) {
          Text(stringResource(StringsR.string.common_dialog_cancel))
        }
        Button(
          onClick = { hideThen(onConfirmDeletion) },
          enabled = armed,
          shapes = ButtonDefaults.shapes(),
          colors = ButtonDefaults.buttonColors(
            containerColor = colors.error,
            contentColor = colors.onError,
          ),
          modifier = Modifier
            .weight(1F)
            .heightIn(min = ButtonDefaults.MediumContainerHeight),
        ) {
          Icon(VoiceIcons.Delete, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
          Spacer(Modifier.width(ButtonDefaults.IconSpacing))
          Text(stringResource(StringsR.string.common_action_delete))
        }
      }
    }
  }
}

/** The cover with a trash badge. Once deleting is confirmed, the cover leans away and the badge bristles and shakes. */
@Composable
private fun DeleteHero(
  cover: String?,
  armed: Boolean,
) {
  val colors = MaterialTheme.colorScheme
  val lean by animateFloatAsState(
    targetValue = if (armed) 1F else 0F,
    animationSpec = spring(dampingRatio = 0.5F, stiffness = Spring.StiffnessMediumLow),
    label = "coverLean",
  )
  val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
  val shake = remember { Animatable(0F) }
  LaunchedEffect(armed) {
    if (armed) {
      shake.animateTo(
        targetValue = 0F,
        animationSpec = keyframes {
          durationMillis = 480
          -14F at 60
          12F at 140
          -9F at 220
          6F at 300
          -3F at 380
        },
      )
    }
  }
  Box(
    modifier = Modifier.size(width = 136.dp, height = 120.dp),
    contentAlignment = Alignment.Center,
  ) {
    AsyncImage(
      modifier = Modifier
        .size(104.dp)
        .graphicsLayer {
          rotationZ = -4F - lean * 6F
          val scale = 1F - lean * 0.06F
          scaleX = scale
          scaleY = scale
          alpha = 1F - lean * 0.25F
        }
        .clip(CoverShape),
      model = rememberCoverThumbnailRequest(cover),
      placeholder = ColorPainter(colors.surfaceContainerHighest),
      error = painterResource(id = UiR.drawable.album_art),
      contentScale = ContentScale.Crop,
      contentDescription = null,
    )
    Box(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .size(52.dp)
        .graphicsLayer { rotationZ = shake.value + lean * 20F },
      contentAlignment = Alignment.Center,
    ) {
      Box(
        Modifier
          .matchParentSize()
          .background(colors.error, MorphShape(morph, lean)),
      )
      Icon(
        imageVector = VoiceIcons.Delete,
        contentDescription = null,
        tint = colors.onError,
        modifier = Modifier.size(26.dp),
      )
    }
  }
}
