package voice.app.misc

import android.app.PendingIntent
import android.content.Context
import javax.inject.Inject
import voice.app.features.MainActivity
import voice.playback.notification.MainActivityIntentProvider

class MainActivityIntentProviderImpl
@Inject constructor(
  private val context: Context,
) : MainActivityIntentProvider {

  override fun toCurrentBook(): PendingIntent {
    val intent = MainActivity.goToBookIntent(context)
    return PendingIntent.getActivity(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }
}
