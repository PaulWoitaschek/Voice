package voice.features.review

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import kotlinx.coroutines.delay
import voice.core.common.FeedbackLinks
import voice.core.common.rootGraphAs
import voice.core.logging.api.Logger
import kotlin.time.Duration.Companion.seconds
import voice.core.strings.R as StringsR

/**
 * Shows the rating prompt when the time is right, but only while [canShow], so it never covers
 * anything but the library.
 */
@Composable
fun ReviewFeature(canShow: Boolean) {
  val controller = remember { rootGraphAs<ReviewGraph>().reviewPromptController }
  var prompt by remember { mutableStateOf<ReviewPrompt?>(null) }
  if (canShow) {
    LaunchedEffect(Unit) {
      // the library comes first
      delay(2.seconds)
      prompt = controller.prompt()
    }
  }
  val context = LocalContext.current
  // Away from the library, the prompt waits and comes back with it.
  prompt?.takeIf { canShow }?.let { current ->
    ReviewPromptSheet(
      prompt = current,
      onAnswer = { answer ->
        prompt = null
        controller.answer(current, answer)
        context.follow(answer, controller.versionName)
      },
    )
  }
}

private fun Context.follow(
  answer: ReviewAnswer,
  versionName: String,
) {
  when (answer) {
    ReviewAnswer.Rate -> openStoreListing()
    ReviewAnswer.Idea -> open(Intent(Intent.ACTION_VIEW, FeedbackLinks.IDEAS.toUri()))
    ReviewAnswer.Bug -> open(Intent(Intent.ACTION_VIEW, FeedbackLinks.bugReport(versionName).toUri()))
    ReviewAnswer.Email -> {
      val body = listOf(
        "\n\n—",
        "Voice $versionName",
        "Android ${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})",
        "${Build.MANUFACTURER} ${Build.MODEL}",
      ).joinToString(separator = "\n")
      val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
        .putExtra(Intent.EXTRA_EMAIL, arrayOf(FeedbackLinks.EMAIL))
        .putExtra(Intent.EXTRA_SUBJECT, getString(StringsR.string.review_prompt_email_subject))
        .putExtra(Intent.EXTRA_TEXT, body)
      open(intent)
    }
    ReviewAnswer.Later -> Unit
  }
}

private fun Context.openStoreListing() {
  val market = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
    .setPackage("com.android.vending")
  try {
    startActivity(market)
  } catch (_: ActivityNotFoundException) {
    // without the Play Store app, the browser shows the listing
    open(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri()))
  }
}

private fun Context.open(intent: Intent) {
  try {
    startActivity(intent)
  } catch (e: ActivityNotFoundException) {
    Logger.w(e, "Nothing can open $intent")
  }
}

@ContributesTo(AppScope::class)
interface ReviewGraph {
  val reviewPromptController: ReviewPromptController
}
