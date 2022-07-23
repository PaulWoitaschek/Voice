package voice.playback.notification

import android.content.Intent
import voice.common.BookId

interface ToBookIntentProvider {
  fun goToBookIntent(id: BookId): Intent
}
