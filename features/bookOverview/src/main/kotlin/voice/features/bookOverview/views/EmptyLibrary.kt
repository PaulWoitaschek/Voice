@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.core.ui.ShapedIcon
import voice.core.ui.VoiceTheme
import voice.core.ui.icons.VoiceIcons
import voice.core.strings.R as StringsR

@Composable
internal fun EmptyLibrary(
  onShowFoldersClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // on short windows, like a phone in landscape, the button would end up below the fold
  val showHero = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() } >= 600.dp
  Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier = Modifier
        .widthIn(max = 480.dp)
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (showHero) {
        EmptyLibraryHero()
        Spacer(Modifier.height(16.dp))
      }
      Text(
        text = stringResource(StringsR.string.library_empty_title),
        style = MaterialTheme.typography.headlineMediumEmphasized,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() },
      )
      Spacer(Modifier.height(8.dp))
      Text(
        text = stringResource(StringsR.string.library_empty_message),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(24.dp))
      Button(
        onClick = onShowFoldersClick,
        shapes = ButtonDefaults.shapes(),
        contentPadding = ButtonDefaults.MediumContentPadding,
        modifier = Modifier.heightIn(min = ButtonDefaults.MediumContainerHeight),
      ) {
        Icon(
          imageVector = VoiceIcons.Folder,
          contentDescription = null,
          modifier = Modifier.size(ButtonDefaults.MediumIconSize),
        )
        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(ButtonDefaults.MediumContainerHeight)))
        Text(
          text = stringResource(StringsR.string.library_empty_action),
          style = MaterialTheme.typography.titleMedium,
        )
      }
    }
  }
}

@Composable
private fun EmptyLibraryHero() {
  Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
    ShapedIcon(
      icon = VoiceIcons.LibraryBooks,
      shape = MaterialShapes.Cookie12Sided,
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      size = 168.dp,
    )
    ShapedIcon(
      icon = VoiceIcons.SearchOff,
      shape = MaterialShapes.SoftBurst,
      containerColor = MaterialTheme.colorScheme.tertiary,
      contentColor = MaterialTheme.colorScheme.onTertiary,
      size = 72.dp,
      shapeRotation = 12F,
      modifier = Modifier.offset(x = 64.dp, y = 56.dp),
    )
  }
}

@Composable
@Preview
private fun EmptyLibraryPreview() {
  VoiceTheme {
    Surface {
      EmptyLibrary(onShowFoldersClick = {})
    }
  }
}
