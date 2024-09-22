package voice.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import voice.review.play.R
import voice.strings.R as StringsR

@Composable
internal fun AskForReviewDialog(
  onReview: (Int) -> Unit,
  onReviewDeny: () -> Unit,
  onDismiss: () -> Unit,
) {
  var selectedStars by remember { mutableIntStateOf(5) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(stringResource(StringsR.string.review_request_title))
    },
    text = {
      Column {
        Text(stringResource(StringsR.string.review_request_content))
        Spacer(Modifier.size(16.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
        ) {
          val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.star),
          )
          repeat(5) { index ->
            val star = index + 1
            val selected = star <= selectedStars
            val progress by animateLottieCompositionAsState(
              composition,
              speed = if (selected) 0.5f else -100f,
            )
            LottieAnimation(
              progress = { progress },
              modifier = Modifier
                .clickable {
                  selectedStars = star
                }
                .size(44.dp),
              composition = composition,
            )
          }
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onReviewDeny) {
        Text(stringResource(StringsR.string.review_request_button_no))
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          onReview(selectedStars)
        },
      ) {
        Text(stringResource(StringsR.string.review_request_button_yes))
      }
    },
  )
}

@Composable
@Preview
private fun AskForReviewDialogPreview() {
  AskForReviewDialog(
    onReview = {},
    onReviewDeny = {},
    onDismiss = {},
  )
}
