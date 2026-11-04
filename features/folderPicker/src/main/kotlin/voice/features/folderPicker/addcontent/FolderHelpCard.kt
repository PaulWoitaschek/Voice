@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.folderPicker.addcontent

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import voice.core.strings.R as StringsR
import voice.core.ui.ShapedIcon
import voice.core.ui.VoiceTheme
import voice.core.ui.icons.VoiceIcons

internal enum class FolderHelp {
  PickerClosed,
  NoPicker,
}

internal enum class FolderHelpAnswer {
  NoFiles,
  NotFound,
  OtherApp,
}

@Composable
internal fun FolderHelpCard(
  help: FolderHelp,
  answer: FolderHelpAnswer?,
  onAnswer: (FolderHelpAnswer) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Column(
      Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()).padding(16.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        ShapedIcon(
          icon = VoiceIcons.Lightbulb,
          shape = MaterialShapes.Sunny,
          containerColor = MaterialTheme.colorScheme.tertiaryContainer,
          contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
          size = 44.dp,
        )
        Spacer(Modifier.width(16.dp))
        Text(
          modifier = Modifier.weight(1F).semantics { heading() },
          text =
            stringResource(
              when (help) {
                FolderHelp.PickerClosed -> StringsR.string.folder_add_help_title
                FolderHelp.NoPicker -> StringsR.string.folder_add_no_picker_title
              },
            ),
          style = MaterialTheme.typography.titleMediumEmphasized,
        )
      }
      when (help) {
        FolderHelp.PickerClosed -> {
          Spacer(Modifier.height(12.dp))
          Answers(answer = answer, onAnswer = onAnswer)
          when (answer) {
            FolderHelpAnswer.NoFiles -> {
              Spacer(Modifier.height(8.dp))
              HelpText(StringsR.string.folder_add_help_no_files)
            }
            FolderHelpAnswer.NotFound -> {
              // older versions of Android still let apps open the Download folder
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Spacer(Modifier.height(8.dp))
                HelpText(StringsR.string.folder_add_help_download)
              }
              Spacer(Modifier.height(8.dp))
              HelpText(StringsR.string.folder_add_help_computer)
            }
            FolderHelpAnswer.OtherApp -> {
              Spacer(Modifier.height(8.dp))
              HelpText(StringsR.string.folder_add_help_other_app)
            }
            null -> {}
          }
        }
        FolderHelp.NoPicker -> {
          Spacer(Modifier.height(8.dp))
          HelpText(StringsR.string.folder_add_no_picker_message)
        }
      }
    }
  }
}

@Composable
private fun Answers(
  answer: FolderHelpAnswer?,
  onAnswer: (FolderHelpAnswer) -> Unit,
) {
  FlowRow(
    modifier = Modifier.selectableGroup(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    FolderHelpAnswer.entries.forEach { option ->
      val selected = option == answer
      FilterChip(
        selected = selected,
        onClick = { onAnswer(option) },
        modifier = Modifier.semantics { role = Role.RadioButton },
        leadingIcon =
          if (selected) {
            {
              Icon(
                imageVector = VoiceIcons.Check,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
              )
            }
          } else {
            null
          },
        label = {
          Text(
            stringResource(
              when (option) {
                FolderHelpAnswer.NoFiles -> StringsR.string.folder_add_help_answer_no_files
                FolderHelpAnswer.NotFound -> StringsR.string.folder_add_help_answer_not_found
                FolderHelpAnswer.OtherApp -> StringsR.string.folder_add_help_answer_other_app
              },
            ),
          )
        },
      )
    }
  }
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
  state: Pair<FolderHelp, FolderHelpAnswer?>,
) {
  VoiceTheme {
    FolderHelpCard(help = state.first, answer = state.second, onAnswer = {})
  }
}

private class FolderHelpPreviewParameterProvider :
  PreviewParameterProvider<Pair<FolderHelp, FolderHelpAnswer?>> {

  override val values: Sequence<Pair<FolderHelp, FolderHelpAnswer?>>
    get() =
      (listOf(null) + FolderHelpAnswer.entries).map { FolderHelp.PickerClosed to it }.asSequence() +
        (FolderHelp.NoPicker to null)
}
