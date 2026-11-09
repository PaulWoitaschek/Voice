@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.folderPicker.addcontent

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR
import voice.core.ui.ShapedIcon
import voice.core.ui.VoiceTheme
import voice.core.ui.segmentedShape

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
  onLibriVox: () -> Unit,
  onSurvey: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Column(
      Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()).padding(16.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        ShapedIcon(
          icon = UiR.drawable.ic_lightbulb,
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
              Spacer(Modifier.height(12.dp))
              NoFilesText(onLibriVox = onLibriVox)
            }
            FolderHelpAnswer.NotFound -> {
              // older versions of Android still let apps open the Download folder
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Spacer(Modifier.height(12.dp))
                HelpText(StringsR.string.folder_add_help_download)
              }
              Spacer(Modifier.height(8.dp))
              HelpText(StringsR.string.folder_add_help_computer)
            }
            FolderHelpAnswer.OtherApp -> {
              Spacer(Modifier.height(12.dp))
              HelpText(StringsR.string.folder_add_help_other_app)
            }
            null -> {}
          }
          Spacer(Modifier.height(12.dp))
          val size = ButtonDefaults.ExtraSmallContainerHeight
          FilledTonalButton(
            onClick = onSurvey,
            contentPadding = ButtonDefaults.contentPaddingFor(size),
            shapes = ButtonDefaults.shapesFor(size),
            modifier = Modifier.heightIn(size),
          ) {
            Text(
              text = stringResource(StringsR.string.folder_add_help_survey),
              style = ButtonDefaults.textStyleFor(size),
            )
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
  Column(
    modifier = Modifier.selectableGroup(),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    FolderHelpAnswer.entries.forEachIndexed { index, option ->
      val selected = option == answer
      val color by
        animateColorAsState(
          targetValue =
            if (selected) {
              MaterialTheme.colorScheme.secondaryContainer
            } else {
              MaterialTheme.colorScheme.surfaceBright
            },
          animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        )
      Surface(
        selected = selected,
        onClick = { onAnswer(option) },
        modifier = Modifier.fillMaxWidth().semantics { role = Role.RadioButton },
        shape = segmentedShape(index, FolderHelpAnswer.entries.size),
        color = color,
        contentColor =
          if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
          } else {
            MaterialTheme.colorScheme.onSurface
          },
      ) {
        Row(
          modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text =
              stringResource(
                when (option) {
                  FolderHelpAnswer.NoFiles -> StringsR.string.folder_add_help_answer_no_files
                  FolderHelpAnswer.NotFound -> StringsR.string.folder_add_help_answer_not_found
                  FolderHelpAnswer.OtherApp -> StringsR.string.folder_add_help_answer_other_app
                },
              ),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1F),
          )
          if (selected) {
            Spacer(Modifier.width(8.dp))
            Icon(painter = painterResource(UiR.drawable.ic_check), contentDescription = null)
          }
        }
      }
    }
  }
}

@Composable
private fun NoFilesText(onLibriVox: () -> Unit) {
  val text = stringResource(StringsR.string.folder_add_help_no_files)
  val linkColor = MaterialTheme.colorScheme.primary
  val linkedText =
    remember(text, linkColor, onLibriVox) {
      buildAnnotatedString {
        append(text)
        // the translations keep the name, some with an ending, like LibriVoxista
        val start = text.indexOf(LIBRIVOX)
        if (start >= 0) {
          addLink(
            LinkAnnotation.Clickable(
              tag = LIBRIVOX,
              styles =
                TextLinkStyles(
                  SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                ),
            ) {
              onLibriVox()
            },
            start = start,
            end = start + LIBRIVOX.length,
          )
        }
      }
    }
  Text(
    text = linkedText,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

private const val LIBRIVOX = "LibriVox"

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
    FolderHelpCard(
      help = state.first,
      answer = state.second,
      onAnswer = {},
      onLibriVox = {},
      onSurvey = {},
    )
  }
}

private class FolderHelpPreviewParameterProvider :
  PreviewParameterProvider<Pair<FolderHelp, FolderHelpAnswer?>> {

  override val values: Sequence<Pair<FolderHelp, FolderHelpAnswer?>>
    get() =
      (listOf(null) + FolderHelpAnswer.entries).map { FolderHelp.PickerClosed to it }.asSequence() +
        (FolderHelp.NoPicker to null)
}
