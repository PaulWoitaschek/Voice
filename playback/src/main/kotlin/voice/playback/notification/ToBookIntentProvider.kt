package voice.playback.notification

import android.content.Intent
import voice.data.Book

interface ToBookIntentProvider {
  fun goToBookIntent(id: Book.Id): Intent
}
