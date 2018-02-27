package de.ph1b.audiobook.signing

import java.io.File
import java.util.*

/**
 * Read the signing details from a properties file.
 */
@Suppress("unused")
class SigningDetailsProvider {

  private val STORE_FILE = "STORE_FILE"
  private val STORE_PASSWORD = "STORE_PASSWORD"
  private val KEY_ALIAS = "KEY_ALIAS"
  private val KEY_PASSWORD = "KEY_PASSWORD"

  fun provide(propFile: File): SigningDetails? {
    val props = Properties()
    if (propFile.canRead()) {
      props.load(propFile.inputStream())
      if (props.containsKey(STORE_FILE) && props.containsKey(STORE_PASSWORD) && props.containsKey(
          KEY_ALIAS
        ) && props.containsKey(
          KEY_PASSWORD
        )) {
        val file = File(props[STORE_FILE].toString())
        val storePass = props[STORE_PASSWORD].toString()
        val keyAlias = props[KEY_ALIAS].toString()
        val keyPass = props[KEY_PASSWORD].toString()
        return SigningDetails(file, storePass, keyAlias, keyPass)
      } else throw IllegalArgumentException("Invalid properties $props")
    } else {
      println("signing.properties not found")
      return null
    }
  }
}
