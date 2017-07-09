/**
 * Helpers in reading primitive types from InputStream.
 * They throw exception on end of stream.
 */
package de.ph1b.audiobook.features.chapterReader

import java.io.EOFException
import java.io.InputStream

fun InputStream.skipBytes(number: Int) = IOUtils.skipFully(this, number.toLong())

fun InputStream.readBytes(number: Int): ByteArray {
  val buf = ByteArray(number)
  var index = 0
  while (index < number) {
    val read = read(buf, index, number - index)
    if (read == -1) throw EOFException()
    index += read
  }
  return buf
}

fun InputStream.readUInt8(): Int {
  val res = read()
  if (res == -1) throw EOFException()
  return res
}

fun InputStream.readLeUInt32(): Long {
  val buf = readBytes(4)
  return buf[0].toULong() or
      (buf[1].toULong() shl 8) or
      (buf[2].toULong() shl 16) or
      (buf[3].toULong() shl 24)
}

fun InputStream.readLeInt32(): Int {
  val buf = readBytes(4)
  return buf[0].toUnsignedInt() or
      (buf[1].toUnsignedInt() shl 8) or
      (buf[2].toUnsignedInt() shl 16) or
      (buf[3].toUnsignedInt() shl 24)
}

fun InputStream.readLeInt64(): Long {
  val buf = readBytes(8)
  return buf[0].toULong() or
      (buf[1].toULong() shl 8) or
      (buf[2].toULong() shl 16) or
      (buf[3].toULong() shl 24) or
      (buf[4].toULong() shl 32) or
      (buf[5].toULong() shl 40) or
      (buf[6].toULong() shl 48) or
      (buf[7].toULong() shl 56)
}

fun ByteArray.startsWith(prefix: ByteArray): Boolean {
  if (this.size < prefix.size) return false
  return prefix.withIndex().all { (i, v) -> v == this[i] }
}

fun Byte.toULong() = toLong() and 0xFFL
