package de.ph1b.audiobook.misc

import android.os.Bundle
import java.util.UUID

fun Bundle.putUUID(key: String, id: UUID) {
  putString(key, id.toString())
}

fun Bundle.getUUID(key: String): UUID {
  val stringValue = getString(key)!!
  return UUID.fromString(stringValue)
}
