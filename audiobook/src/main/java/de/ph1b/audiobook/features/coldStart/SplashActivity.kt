package de.ph1b.audiobook.features.coldStart

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import de.ph1b.audiobook.features.MainActivity

/**
 * Activity that just exists to fake a toolbar through its windowbackground upon start
 *
 * @author Paul Woitaschek
 */
class SplashActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val intent = Intent(this, MainActivity::class.java)
    startActivity(intent)
    finish()
  }
}