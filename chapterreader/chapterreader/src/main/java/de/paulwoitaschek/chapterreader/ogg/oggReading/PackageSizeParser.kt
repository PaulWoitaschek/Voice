package de.paulwoitaschek.chapterreader.ogg.oggReading

import de.paulwoitaschek.chapterreader.misc.toUInt

internal object PackageSizeParser {

  fun fromSegmentTable(segmentTable: ByteArray): List<Int> = segmentTable
    .map { it.toUInt() }
    .fold(
      mutableListOf(0), { acc, e ->
      acc[acc.lastIndex] += e
      if (e != 255) {
        acc.add(0)
      }
      acc
    }
    )
    .filter { it != 0 }
}
