package de.ph1b.audiobook.playback.notification

import android.content.Intent
import java.util.UUID

interface ToBookIntentProvider {
  fun goToBookIntent(id: UUID): Intent
}
