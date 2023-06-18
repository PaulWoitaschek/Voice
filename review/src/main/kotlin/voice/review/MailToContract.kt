package voice.review

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.net.toUri

internal object MailToContract : ActivityResultContract<String, Unit>() {

  override fun createIntent(context: Context, input: String): Intent {
    return Intent(Intent.ACTION_SENDTO)
      .putExtra(Intent.EXTRA_EMAIL, arrayOf(input))
      .setData("mailto:".toUri())
  }

  override fun parseResult(resultCode: Int, intent: Intent?) {}
}
