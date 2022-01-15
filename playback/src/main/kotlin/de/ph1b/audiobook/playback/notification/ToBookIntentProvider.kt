package de.ph1b.audiobook.playback.notification

import android.content.Intent
import android.net.Uri
import java.util.UUID

interface ToBookIntentProvider {
  fun goToBookIntent(id: Uri): Intent
}
