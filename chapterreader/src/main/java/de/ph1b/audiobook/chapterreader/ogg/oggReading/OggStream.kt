package de.ph1b.audiobook.chapterreader.ogg.oggReading

import java.util.ArrayDeque

internal class OggStream(private val pullPage: OggStream.() -> Unit) : Iterator<ByteArray> {
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
