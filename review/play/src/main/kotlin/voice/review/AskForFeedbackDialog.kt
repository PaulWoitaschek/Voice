package voice.review

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import voice.strings.R as StringsR

@Composable
internal fun AskForFeedbackDialog(
  onFeedback: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(stringResource(StringsR.string.review_feedback_title))
    },
    text = {
      Text(stringResource(StringsR.string.review_feedback_content))
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(StringsR.string.review_feedback_button_no))
      }
    },
    confirmButton = {
      TextButton(
        onClick = onFeedback,
      ) {
        Text(stringResource(StringsR.string.review_feedback_button_yes))
      }
    },
  )
}

@Composable
@Preview
private fun AskForFeedbackDialogPreview() {
  AskForFeedbackDialog(
    onFeedback = {},
    onDismiss = {},
  )
}
