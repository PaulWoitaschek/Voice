package de.ph1b.audiobook.misc.argumentDelegate

import android.os.Bundle


class StringArgumentDelegate : ArgumentDelegate<String>() {

  override fun write(args: Bundle, key: String, value: String) {
    args.putString(key, value)
  }

  override fun read(args: Bundle, key: String): String = args.getString(key)
}
