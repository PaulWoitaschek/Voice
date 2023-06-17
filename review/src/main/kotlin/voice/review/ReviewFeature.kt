package voice.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.squareup.anvil.annotations.ContributesTo
import kotlinx.coroutines.launch
import voice.common.AppScope
import voice.common.rootComponentAs

@Composable
fun ReviewFeature() {
  val shouldShowReviewDialog = remember {
    rootComponentAs<ReviewComponent>().shouldShowReviewDialog
  }
  var showReviewDialog by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    showReviewDialog = shouldShowReviewDialog.shouldShow()
  }
  val scope = rememberCoroutineScope()
  if (showReviewDialog) {
    AskForReviewDialog(
      onRate = {
        showReviewDialog = false
        scope.launch {
          shouldShowReviewDialog.setShown()
        }
      },
      onReviewDenied = {
        showReviewDialog = false
        scope.launch {
          shouldShowReviewDialog.setShown()
        }
      },
      onDismiss = {
        showReviewDialog = false
      },
    )
  }
}

@ContributesTo(AppScope::class)
interface ReviewComponent {
  var shouldShowReviewDialog: ShouldShowReviewDialog
}
