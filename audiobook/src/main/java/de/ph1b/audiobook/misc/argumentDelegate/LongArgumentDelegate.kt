package de.ph1b.audiobook.misc.argumentDelegate

import android.os.Bundle


class LongArgumentDelegate : ArgumentDelegate<Long>() {

  override fun write(args: Bundle, key: String, value: Long) {
    args.putLong(key, value)
  }

  override fun read(args: Bundle, key: String) = args.getLong(key)
}
