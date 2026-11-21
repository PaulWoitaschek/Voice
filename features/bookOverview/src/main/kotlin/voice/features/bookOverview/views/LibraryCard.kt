@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.views

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR
import voice.core.ui.ShapedIcon
import voice.core.ui.VoiceTheme

@Composable
internal fun LibraryCard(
  @DrawableRes icon: Int,
  shape: RoundedPolygon,
  title: String,
  message: String,
  action: String,
  onClick: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLargeIncreased,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Column(Modifier.padding(24.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        ShapedIcon(
          icon = icon,
          shape = shape,
          containerColor = MaterialTheme.colorScheme.tertiary,
          contentColor = MaterialTheme.colorScheme.onTertiary,
          size = 56.dp,
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleLargeEmphasized,
          modifier = Modifier.semantics { heading() },
        )
      }
      Spacer(Modifier.height(12.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(20.dp))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(onClick = onClick, shapes = ButtonDefaults.shapes()) {
          Text(action)
        }
        TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
          Text(stringResource(StringsR.string.library_survey_dismiss))
        }
      }
    }
  }
}

@Composable
internal fun SurveyCard(
  onClick: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LibraryCard(
    icon = UiR.drawable.ic_forum,
    shape = MaterialShapes.Cookie9Sided,
    title = stringResource(StringsR.string.library_survey_title),
    message = stringResource(StringsR.string.library_survey_message),
    action = stringResource(StringsR.string.library_survey_open),
    onClick = onClick,
    onDismiss = onDismiss,
    modifier = modifier,
  )
}

@Composable
internal fun SupportCard(
  onClick: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LibraryCard(
    icon = UiR.drawable.ic_favorite,
    shape = MaterialShapes.SoftBurst,
    title = stringResource(StringsR.string.review_prompt_finished_title),
    message = stringResource(StringsR.string.support_description_maintenance),
    action = stringResource(StringsR.string.settings_support_support_voice_title),
    onClick = onClick,
    onDismiss = onDismiss,
    modifier = modifier,
  )
}

@Composable
@Preview
private fun SurveyCardPreview() {
  VoiceTheme {
    SurveyCard(onClick = {}, onDismiss = {})
  }
}

@Composable
@Preview
private fun SupportCardPreview() {
  VoiceTheme {
    SupportCard(onClick = {}, onDismiss = {})
  }
}
