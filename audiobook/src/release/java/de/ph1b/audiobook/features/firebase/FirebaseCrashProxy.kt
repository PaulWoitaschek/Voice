package de.ph1b.audiobook.features.firebase

import com.google.firebase.crash.FirebaseCrash

/**
 * Proxy class that allows us excluding firebase crash in debug builds
 *
 * @author Paul Woitaschek
 */
object FirebaseCrashProxy {

  fun log(message: String) = FirebaseCrash.log(message)
}
