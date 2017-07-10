package de.ph1b.audiobook.features.chapterReader

import java.io.EOFException
import java.io.InputStream

private val OGG_PAGE_MAGIC = "OggS".toByteArray()

internal data class OggPage(
    val continuedPacket: Boolean,
    val finishedPacket: Boolean,
    val firstPageOfStream: Boolean,
    val lastPageOfStream: Boolean,
    val absoluteGranulePosition: Long,
    val streamSerialNumber: Int,
    val pageSequenceNumber: Long,
    val packets: List<ByteArray>) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is OggPage) return false
    return continuedPacket == other.continuedPacket &&
        finishedPacket == other.finishedPacket &&
        firstPageOfStream == other.firstPageOfStream &&
        lastPageOfStream == other.lastPageOfStream &&
        absoluteGranulePosition == other.absoluteGranulePosition &&
        streamSerialNumber == other.streamSerialNumber &&
        pageSequenceNumber == other.pageSequenceNumber &&
        packets.size == other.packets.size &&
        packets.indices.all { packets[it] contentEquals other.packets[it] }
  }

  override fun hashCode()
      = listOf(continuedPacket.hashCode(), finishedPacket.hashCode(), firstPageOfStream.hashCode(),
          lastPageOfStream.hashCode(), absoluteGranulePosition.hashCode(),
          streamSerialNumber.hashCode(), pageSequenceNumber.hashCode(),
          packets.map { it.contentHashCode() }.hashCode()).hashCode()
}

class OGGPageParseException(message: String) : Exception(message)

internal fun computePacketSizesFromSegmentTable(segmentTable: ByteArray): List<Int>
    = segmentTable.map { it.toUInt() }.fold(mutableListOf(0), { acc, e ->
  acc[acc.lastIndex] += e
  if (e != 255)
    acc.add(0)
  acc
}).filter { it != 0 }

internal fun readOggPages(stream: InputStream): Sequence<OggPage> {
  return generateSequence gen@ {
    // https://www.ietf.org/rfc/rfc3533.txt
    val capturePattern = try {
      stream.readBytes(4)
    } catch (_: EOFException) {
      return@gen null
    }
    if (!(capturePattern contentEquals OGG_PAGE_MAGIC))
      throw OGGPageParseException("Invalid capture pattern")
    try {
      if (stream.readUInt8() != 0)
        throw OGGPageParseException("Expected stream structure version 0")
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
      throw OGGPageParseException("Unexpected end of stream")
    }
  }
}
