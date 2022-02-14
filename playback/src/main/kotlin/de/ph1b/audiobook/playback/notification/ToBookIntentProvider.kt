package de.ph1b.audiobook.playback.notification

import android.content.Intent
import de.ph1b.audiobook.data.Book

interface ToBookIntentProvider {
  fun goToBookIntent(id: Book.Id): Intent
}
