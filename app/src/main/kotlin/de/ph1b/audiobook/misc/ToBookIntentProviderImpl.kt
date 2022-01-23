package de.ph1b.audiobook.misc

import android.content.Context
import android.content.Intent
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.features.MainActivity
import de.ph1b.audiobook.playback.notification.ToBookIntentProvider
import javax.inject.Inject

class ToBookIntentProviderImpl
@Inject constructor(private val context: Context) : ToBookIntentProvider {

  override fun goToBookIntent(id: Book2.Id): Intent {
    return MainActivity.goToBookIntent(context, id)
  }
}
