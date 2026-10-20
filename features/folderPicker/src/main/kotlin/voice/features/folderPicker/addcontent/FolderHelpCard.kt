@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.folderPicker.addcontent

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import voice.core.ui.ShapedIcon
import voice.core.ui.VoiceTheme
import voice.core.ui.icons.VoiceIcons
import voice.core.strings.R as StringsR

internal enum class FolderHelp {
  PickerClosed,
  NoPicker,
}

@Composable
internal fun FolderHelpCard(
  help: FolderHelp,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Row(Modifier.padding(16.dp)) {
      ShapedIcon(
        icon = VoiceIcons.Lightbulb,
        shape = MaterialShapes.Sunny,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        size = 44.dp,
      )
      Spacer(Modifier.width(16.dp))
      Column(Modifier.weight(1F)) {
        when (help) {
          FolderHelp.PickerClosed -> {
            HelpTitle(StringsR.string.folder_add_help_title)
            // older versions of Android still let apps open the Download folder
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
              Spacer(Modifier.height(4.dp))
              HelpText(StringsR.string.folder_add_help_download)
            }
            Spacer(Modifier.height(8.dp))
            HelpText(StringsR.string.folder_add_help_computer)
          }
          FolderHelp.NoPicker -> {
            HelpTitle(StringsR.string.folder_add_no_picker_title)
            Spacer(Modifier.height(4.dp))
            HelpText(StringsR.string.folder_add_no_picker_message)
          }
        }
      }
    }
  }
}

@Composable
private fun HelpTitle(@StringRes text: Int) {
  Text(
    text = stringResource(text),
    style = MaterialTheme.typography.titleMediumEmphasized,
  )
}

@Composable
private fun HelpText(@StringRes text: Int) {
  Text(
    text = stringResource(text),
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
@Preview
private fun FolderHelpCardPreview(
  @PreviewParameter(FolderHelpPreviewParameterProvider::class)
  help: FolderHelp,
) {
  VoiceTheme {
    FolderHelpCard(help)
  }
}

private class FolderHelpPreviewParameterProvider : PreviewParameterProvider<FolderHelp> {

  override val values: Sequence<FolderHelp>
    get() = FolderHelp.entries.asSequence()
}
