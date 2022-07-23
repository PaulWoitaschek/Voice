package voice.app.misc

import android.content.Context
import android.content.Intent
import voice.app.features.MainActivity
import voice.common.BookId
import voice.playback.notification.ToBookIntentProvider
import javax.inject.Inject

class ToBookIntentProviderImpl
@Inject constructor(private val context: Context) : ToBookIntentProvider {

  override fun goToBookIntent(id: BookId): Intent {
    return MainActivity.goToBookIntent(context, id)
  }
}
