package de.ph1b.audiobook.features.firebase

/**
 * Proxy class that allows us excluding firebase crash in debug builds
 *
 * @author Paul Woitaschek
 */
object FirebaseCrashProxy {

  fun log(message: String) {
  }
}
