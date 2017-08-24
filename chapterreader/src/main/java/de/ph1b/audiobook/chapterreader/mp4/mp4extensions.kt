package de.ph1b.audiobook.chapterreader.mp4

import java.io.EOFException
import java.io.RandomAccessFile
import java.util.ArrayList

internal data class Mp4Atom(val name: String, val position: Long, val length: Long, val children: List<Mp4Atom>)

internal fun RandomAccessFile.atoms(toVisit: List<String>, endOfAtom: Long? = null): List<Mp4Atom> {
  val atoms = ArrayList<Mp4Atom>()
  while (true) {
    if (filePointer >= length()) break
    val length = readUnsignedInt()

    val name = readBoxHeader()
    val atomPosition = filePointer - 8

    val visit = toVisit.contains(name)
    val children = if (visit) {
      atoms(toVisit, atomPosition + length)
    } else {
      skipBytes(length.toInt() - 8)
      emptyList()
    }

    atoms.add(Mp4Atom(name, atomPosition, length, children))

    if (endOfAtom != null && filePointer == endOfAtom) {
      break
    }
  }
  return atoms
}

internal fun List<Mp4Atom>.findAtom(vararg path: String): Mp4Atom? {
  if (path.isEmpty()) return null

  forEach { root ->
    if (root.name == path.first()) {
      var match: Mp4Atom? = null
      path.forEachIndexed { index, s ->
        if (index != 0) {
          val searchIn = match ?: root
          match = searchIn.children.firstOrNull { it.name == s }
        }
      }
      if (match != null) return match
    }
  }

  return firstOrNull { it.name == path.first() }
}

internal fun RandomAccessFile.readUnsignedInt(): Long =
    (read().toLong() and 0xFFL shl 24) or
        (read().toLong() and 0xFFL shl 16) or
        (read().toLong() and 0xFFL shl 8) or
        (read().toLong() and 0xFFL)

private val boxHeaderBuffer = ByteArray(4)

@Synchronized
internal fun RandomAccessFile.readBoxHeader(): String {
  val result = read(boxHeaderBuffer)
  if (result == -1) throw EOFException("Can't read box header")
  return String(boxHeaderBuffer)
}

internal fun RandomAccessFile.readUInt64(): Long = (readUnsignedInt() shl 32) + readUnsignedInt()
