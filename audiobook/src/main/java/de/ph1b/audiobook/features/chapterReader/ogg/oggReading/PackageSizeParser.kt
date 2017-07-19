package de.ph1b.audiobook.features.chapterReader.ogg.oggReading

import de.ph1b.audiobook.features.chapterReader.toUInt


object PackageSizeParser {

  fun fromSegmentTable(segmentTable: ByteArray): List<Int> = segmentTable
      .map { it.toUInt() }
      .fold(mutableListOf(0), { acc, e ->
        acc[acc.lastIndex] += e
        if (e != 255) {
          acc.add(0)
        }
        acc
      })
      .filter { it != 0 }
}
