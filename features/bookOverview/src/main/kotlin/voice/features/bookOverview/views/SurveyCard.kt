@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import voice.core.ui.ShapedIcon
import voice.core.ui.VoiceTheme
import voice.core.ui.icons.VoiceIcons
import voice.core.strings.R as StringsR

@Composable
internal fun SurveyCard(
  onClick: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(32.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Column(Modifier.padding(24.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        ShapedIcon(
          icon = VoiceIcons.Forum,
          shape = MaterialShapes.Cookie9Sided,
          containerColor = MaterialTheme.colorScheme.tertiary,
          contentColor = MaterialTheme.colorScheme.onTertiary,
          size = 56.dp,
        )
        Text(
          text = stringResource(StringsR.string.library_survey_title),
          style = MaterialTheme.typography.titleLargeEmphasized,
          modifier = Modifier.semantics { heading() },
        )
      }
      Spacer(Modifier.height(12.dp))
      Text(
        text = stringResource(StringsR.string.library_survey_message),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(20.dp))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(onClick = onClick, shapes = ButtonDefaults.shapes()) {
          Text(stringResource(StringsR.string.library_survey_open))
        }
        TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
          Text(stringResource(StringsR.string.library_survey_dismiss))
        }
      }
    }
  }
}

@Composable
@Preview
private fun SurveyCardPreview() {
  VoiceTheme {
    SurveyCard(onClick = {}, onDismiss = {})
  }
}
