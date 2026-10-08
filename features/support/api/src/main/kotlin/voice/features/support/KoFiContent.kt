@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.support

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import voice.core.ui.icons.VoiceIcons
import voice.core.strings.R as StringsR

/** Donating through Ko-fi, with three colorful tiles of what the donations pay for. */
@Composable
internal fun KoFiContent(onKoFiClick: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    SupportHelps()
    val size = ButtonDefaults.MediumContainerHeight
    Button(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(size),
      onClick = onKoFiClick,
      contentPadding = ButtonDefaults.contentPaddingFor(size, hasStartIcon = true),
      shapes = ButtonDefaults.shapesFor(size),
    ) {
      Icon(
        imageVector = VoiceIcons.Coffee,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)),
      )
      Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
      Text(
        text = stringResource(StringsR.string.support_action_donate_kofi),
        style = ButtonDefaults.textStyleFor(size),
      )
    }
    Text(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(StringsR.string.support_kofi_hint),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun SupportHelps() {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = stringResource(StringsR.string.support_helps_title),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val colors = MaterialTheme.colorScheme
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      HelpTile(
        icon = VoiceIcons.Construction,
        title = stringResource(StringsR.string.support_helps_maintenance),
        shape = MaterialShapes.Cookie4Sided,
        container = colors.secondaryContainer,
        content = colors.onSecondaryContainer,
        modifier = Modifier.weight(1F),
      )
      HelpTile(
        icon = VoiceIcons.AutoAwesome,
        title = stringResource(StringsR.string.support_helps_features),
        shape = MaterialShapes.Sunny,
        container = colors.tertiaryContainer,
        content = colors.onTertiaryContainer,
        modifier = Modifier.weight(1F),
      )
      HelpTile(
        icon = VoiceIcons.LockOpen,
        title = stringResource(StringsR.string.support_helps_open_source),
        shape = MaterialShapes.Clover4Leaf,
        container = colors.primaryContainer,
        content = colors.onPrimaryContainer,
        modifier = Modifier.weight(1F),
      )
    }
  }
}

@Composable
private fun HelpTile(
  icon: ImageVector,
  title: String,
  shape: RoundedPolygon,
  container: Color,
  content: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxHeight(),
    shape = RoundedCornerShape(24.dp),
    color = container,
    contentColor = content,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .background(content, shape.toShape()),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          modifier = Modifier.size(22.dp),
          imageVector = icon,
          contentDescription = null,
          tint = container,
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center,
      )
    }
  }
}
