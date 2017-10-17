package de.ph1b.audiobook.common


object DatatypeConverter {

  fun parseHexBinary(s: String): ByteArray {
    val len = s.length

    // "111" is not a valid hex encoding.
    if (len % 2 != 0)
      throw IllegalArgumentException("hexBinary needs to be even-length: " + s)

    val out = ByteArray(len / 2)

    var i = 0
    while (i < len) {
      val h = hexToBin(s[i])
      val l = hexToBin(s[i + 1])
      if (h == -1 || l == -1)
        throw IllegalArgumentException("contains illegal character for hexBinary: " + s)

      out[i / 2] = (h * 16 + l).toByte()
      i += 2
    }

    return out
  }

  private fun hexToBin(ch: Char): Int = when (ch) {
    in '0'..'9' -> ch - '0'
    in 'A'..'F' -> ch - 'A' + 10
    else -> if (ch in 'a'..'f') ch - 'a' + 10 else -1
  }
}
