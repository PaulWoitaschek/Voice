package de.ph1b.audiobook.features.chapterReader.ogg.oggReading

import android.util.SparseArray
import de.ph1b.audiobook.features.chapterReader.readBytes
import de.ph1b.audiobook.features.chapterReader.readLeInt32
import de.ph1b.audiobook.features.chapterReader.readLeInt64
import de.ph1b.audiobook.features.chapterReader.readLeUInt32
import de.ph1b.audiobook.features.chapterReader.readUInt8
import de.ph1b.audiobook.features.chapterReader.skipBytes
import de.ph1b.audiobook.features.chapterReader.toUInt
import java.io.EOFException
import java.io.InputStream

private val OGG_PAGE_MAGIC = "OggS".toByteArray()

fun computePacketSizesFromSegmentTable(segmentTable: ByteArray): List<Int>
    = segmentTable.map { it.toUInt() }.fold(mutableListOf(0), { acc, e ->
  acc[acc.lastIndex] += e
  if (e != 255)
    acc.add(0)
  acc
}).filter { it != 0 }

fun readOggPages(stream: InputStream): Sequence<OggPage> {
  return generateSequence gen@ {
    // https://www.ietf.org/rfc/rfc3533.txt
    val capturePattern = try {
      stream.readBytes(4)
    } catch (_: EOFException) {
      return@gen null
    }
    if (!(capturePattern contentEquals OGG_PAGE_MAGIC))
      throw OggPageParseException("Invalid capture pattern")
    try {
      if (stream.readUInt8() != 0)
        throw OggPageParseException("Expected stream structure version 0")
      val headerTypeFlag = stream.readUInt8()
      val absoluteGranulePosition = stream.readLeInt64()
      val streamSerialNumber = stream.readLeInt32()
      val pageSequenceNumber = stream.readLeUInt32()
      stream.skipBytes(4)  // checksum
      val numberPageSegments = stream.readUInt8()
      val segmentTable = stream.readBytes(numberPageSegments)
      val packets = computePacketSizesFromSegmentTable(segmentTable).map {
        stream.readBytes(it)
      }

      OggPage(
          continuedPacket = headerTypeFlag and 0b001 != 0,
          finishedPacket = segmentTable[segmentTable.lastIndex].toUInt() != 255,
          firstPageOfStream = headerTypeFlag and 0b010 != 0,
          lastPageOfStream = headerTypeFlag and 0b100 != 0,
          absoluteGranulePosition = absoluteGranulePosition,
          streamSerialNumber = streamSerialNumber,
          pageSequenceNumber = pageSequenceNumber,
          packets = packets
      )
    } catch (_: EOFException) {
      throw OggPageParseException("Unexpected end of stream")
    }
  }
}

fun Iterable<ByteArray>.concat(): ByteArray {
  val res = ByteArray(this.sumBy { it.size })
  var idx = 0
  for (part in this) {
    System.arraycopy(part, 0, res, idx, part.size)
    idx += part.size
  }
  return res
}

fun demuxOggStreams(oggPages: Sequence<OggPage>): SparseArray<OggStream> {
  val it = oggPages.iterator()
  val streamMap = SparseArray<OggStream>()

  fun pushToStream(page: OggPage) {
    streamMap[page.streamSerialNumber].pushPage(page)
  }

  while (it.hasNext()) {
    val page = it.next()
    if (page.firstPageOfStream) {
      val stream = OggStream({
        pushToStream(it.next())
      })
      stream.pushPage(page)
      streamMap.put(page.streamSerialNumber, stream)
    } else {
      pushToStream(page)
      break
    }
  }

  return streamMap
}
