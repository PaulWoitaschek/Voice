package de.ph1b.audiobook.features.chapterReader.ogg

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
