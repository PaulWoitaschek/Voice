package voice.review

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.view.ContextThemeWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.squareup.anvil.annotations.ContributesTo
import kotlinx.coroutines.launch
import voice.common.AppScope
import voice.common.rootComponentAs
import voice.logging.core.Logger

@Composable
fun ReviewFeature() {
  val reviewComponent = rootComponentAs<ReviewComponent>()
  val reviewInfoState = remember { mutableStateOf<ReviewInfo?>(null) }
  LaunchedEffect(Unit) {
    val reviewManager = reviewComponent.reviewManager
    reviewInfoState.value = try {
      reviewManager.requestReview()
    } catch (e: Exception) {
      Logger.e(e, "Error while creating the review flow")
      null
    }
  }
  val reviewInfo = reviewInfoState.value ?: return

  val shouldShowReviewDialog = remember {
    reviewComponent.shouldShowReviewDialog
  }
  var showReviewDialog by remember { mutableStateOf(false) }
  var showFeedbackDialog by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    showReviewDialog = shouldShowReviewDialog.shouldShow()
  }
  val scope = rememberCoroutineScope()
  if (showReviewDialog) {
    val reviewManager = reviewComponent.reviewManager
    val activity = LocalContext.current.findActivity() ?: return
    AskForReviewDialog(
      onReview = { stars ->
        Logger.d("User rated $stars")
        scope.launch {
          shouldShowReviewDialog.setShown()
          if (stars < 5) {
            showFeedbackDialog = true
          } else {
            try {
              reviewManager.launchReview(activity, reviewInfo)
            } catch (e: Exception) {
              Logger.e(e, "Error while launching the review flow")
            }
          }
          showReviewDialog = false
        }
      },
      onReviewDeny = {
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
  val mailToLauncher = rememberLauncherForActivityResult(contract = MailToContract, onResult = {})
  if (showFeedbackDialog) {
    AskForFeedbackDialog(
      onFeedback = {
        showFeedbackDialog = false
        try {
          mailToLauncher.launch("audiobook@posteo.de")
        } catch (e: ActivityNotFoundException) {
          Logger.e(e, "Could not find an email app")
        }
      },
      onDismiss = {
        showFeedbackDialog = false
      },
    )
  }
}

private fun Context.findActivity(): Activity? {
  return when (this) {
    is Activity -> this
    is ContextThemeWrapper -> this.baseContext.findActivity()
    else -> null
  }
}

@ContributesTo(AppScope::class)
interface ReviewComponent {
  var shouldShowReviewDialog: ShouldShowReviewDialog
  val reviewManager: ReviewManager
}
