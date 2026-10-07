@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.folderPicker.addcontent

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import voice.core.logging.api.Logger
import voice.core.ui.icons.VoiceIcons
import voice.features.folderPicker.folderPicker.FileTypeSelection
import voice.core.strings.R as StringsR

/** Two big tiles to either pick a folder or a single file. */
@Composable
internal fun ContentTypeChoices(
  onAdd: (FileTypeSelection, Uri) -> Unit,
  modifier: Modifier = Modifier,
) {
  val openDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null) {
      onAdd(FileTypeSelection.File, uri)
    }
  }
  val documentTreeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
    if (uri != null) {
      onAdd(FileTypeSelection.Folder, uri)
    }
  }
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(IntrinsicSize.Min),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    ChoiceTile(
      modifier = Modifier
        .weight(1F)
        .fillMaxHeight(),
      text = stringResource(StringsR.string.folder_add_type_folder),
      icon = VoiceIcons.Folder,
      shape = MaterialShapes.Cookie9Sided,
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      onClick = {
        try {
          documentTreeLauncher.launch(null)
        } catch (e: ActivityNotFoundException) {
          Logger.w(e, "Could not add folder")
        }
      },
    )
    ChoiceTile(
      modifier = Modifier
        .weight(1F)
        .fillMaxHeight(),
      text = stringResource(StringsR.string.folder_add_type_file),
      icon = VoiceIcons.AudioFile,
      shape = MaterialShapes.Clover4Leaf,
      containerColor = MaterialTheme.colorScheme.tertiaryContainer,
      contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
      onClick = {
        try {
          openDocumentLauncher.launch(arrayOf("*/*"))
        } catch (e: ActivityNotFoundException) {
          Logger.w(e, "Could not add file")
        }
      },
    )
  }
}

/** A tile that squishes when pressed while its shape gives a little twirl. */
@Composable
private fun ChoiceTile(
  text: String,
  icon: ImageVector,
  shape: RoundedPolygon,
  containerColor: Color,
  contentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val squish by animateFloatAsState(
    targetValue = if (pressed) 0.94F else 1F,
    animationSpec = spring(dampingRatio = 0.4F, stiffness = Spring.StiffnessMedium),
    label = "tileSquish",
  )
  val twirl by animateFloatAsState(
    targetValue = if (pressed) 60F else 0F,
    animationSpec = spring(dampingRatio = 0.4F, stiffness = Spring.StiffnessMediumLow),
    label = "tileTwirl",
  )
  Surface(
    onClick = onClick,
    modifier = modifier.graphicsLayer {
      scaleX = squish
      scaleY = squish
    },
    shape = RoundedCornerShape(32.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    interactionSource = interactionSource,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Box(
          Modifier
            .matchParentSize()
            .graphicsLayer { rotationZ = twirl }
            .background(containerColor, shape.toShape()),
        )
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier.size(28.dp),
        )
      }
      Spacer(Modifier.height(12.dp))
      Text(
        text = text,
        style = MaterialTheme.typography.titleMediumEmphasized,
        textAlign = TextAlign.Center,
      )
    }
  }
}
