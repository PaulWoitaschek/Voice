package de.ph1b.audiobook.features.coldStart

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.ph1b.audiobook.features.MainActivity

/**
 * Activity that just exists to fake a toolbar through its windowbackground upon start
 */
class SplashActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // app shortcut
    val playCurrentBookImmediately = intent.action == "playCurrent"
    val intent = MainActivity.newIntent(this, playCurrentBookImmediately)
    startActivity(intent)
    finish()
  }
}
