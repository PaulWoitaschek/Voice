package de.ph1b.audiobook.features.chapterReader.ogg

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
import java.util.ArrayDeque

private val OGG_PAGE_MAGIC = "OggS".toByteArray()

data class OggPage(
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
    return continuedPacket == other.continuedPacket
        && finishedPacket == other.finishedPacket
        && firstPageOfStream == other.firstPageOfStream
        && lastPageOfStream == other.lastPageOfStream
        && absoluteGranulePosition == other.absoluteGranulePosition
        && streamSerialNumber == other.streamSerialNumber
        && pageSequenceNumber == other.pageSequenceNumber
        && packets.size == other.packets.size
        && packets.indices.all { packets[it] contentEquals other.packets[it] }
  }

  override fun hashCode(): Int {
    var hashCode = 17
    hashCode = 31 * hashCode + continuedPacket.hashCode()
    hashCode = 31 * hashCode + finishedPacket.hashCode()
    hashCode = 31 * hashCode + firstPageOfStream.hashCode()
    hashCode = 31 * hashCode + lastPageOfStream.hashCode()
    hashCode = 31 * hashCode + absoluteGranulePosition.hashCode()
    hashCode = 31 * hashCode + streamSerialNumber.hashCode()
    hashCode = 31 * hashCode + pageSequenceNumber.hashCode()
    packets.forEach {
      hashCode = 31 * hashCode + it.contentHashCode()
    }
    return hashCode
  }
}

class OGGPageParseException(message: String) : Exception(message)

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

fun Iterable<ByteArray>.concat(): ByteArray {
  val res = ByteArray(this.sumBy { it.size })
  var idx = 0
  for (part in this) {
    System.arraycopy(part, 0, res, idx, part.size)
    idx += part.size
  }
  return res
}

class OggStream(private val pullPage: OggStream.() -> Unit) : Iterator<ByteArray> {
  private val packetsQue = ArrayDeque<ByteArray>()
  private val packetBuffer = mutableListOf<ByteArray>()
  private var isDone = false

  fun pushPage(page: OggPage) {
    if (isDone) return
    val start = if (page.continuedPacket) {
      if (page.packets.size > 1 || page.finishedPacket) {
        packetBuffer.add(page.packets[0])
        packetsQue.add(packetBuffer.concat())
        packetBuffer.clear()
      }
      1
    } else 0
    val end = page.packets.lastIndex - if (page.finishedPacket) 0 else {
      packetBuffer.add(page.packets[page.packets.lastIndex])
      1
    }
    (start..end).mapTo(packetsQue) { page.packets[it] }
    if (page.lastPageOfStream) isDone = true
  }

  override fun hasNext(): Boolean {
    while (packetsQue.isEmpty()) {
      if (isDone) return false
      pullPage()
    }
    return true
  }

  override fun next(): ByteArray {
    if (!hasNext()) throw NoSuchElementException()
    return packetsQue.poll()
  }

  fun peek(): ByteArray {
    if (!hasNext()) throw NoSuchElementException()
    return packetsQue.peek()
  }
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
