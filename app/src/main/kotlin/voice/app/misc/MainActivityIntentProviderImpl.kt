package voice.app.misc

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dev.zacsweers.metro.Inject
import voice.app.MainActivity
import voice.core.playback.notification.MainActivityIntentProvider

@Inject
class MainActivityIntentProviderImpl(private val context: Context) : MainActivityIntentProvider {

  override fun toCurrentBook(): PendingIntent {
    return PendingIntent.getActivity(
      context,
      0,
      currentBookIntent(),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  override fun currentBookIntent(): Intent = MainActivity.goToBookIntent(context)

  override fun libraryIntent(): Intent = Intent(context, MainActivity::class.java)
    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
}
