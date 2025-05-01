package de.paulwoitaschek.chapterreader.ogg

import de.paulwoitaschek.chapterreader.ogg.oggReading.OggPage
import de.paulwoitaschek.chapterreader.ogg.oggReading.OggStream
import de.paulwoitaschek.chapterreader.ogg.oggReading.PackageSizeParser
import de.paulwoitaschek.chapterreader.ogg.oggReading.concat
import de.paulwoitaschek.chapterreader.ogg.oggReading.demuxOggStreams
import de.paulwoitaschek.chapterreader.ogg.oggReading.readOggPages
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.io.File
import kotlin.random.Random

class OggReadingTest {

  private fun unsignedByteArrayOf(vararg values: Int) = values.map { it.toByte() }.toByteArray()

  @Test
  fun computePacketSizesFromSegmentTableTest() {
    PackageSizeParser.fromSegmentTable(
      unsignedByteArrayOf(
        255,
        255,
        14,
        255,
        0,
        255,
        255,
        17,
      ),
    ) shouldContainExactly listOf(2 * 255 + 14, 255, 2 * 255 + 17)

    PackageSizeParser.fromSegmentTable(
      unsignedByteArrayOf(
        255,
        255,
        255,
        255,
        70,
        255,
        255,
      ),
    ) shouldContainExactly listOf(4 * 255 + 70, 2 * 255)
  }

  @Test
  fun readOggPagesTest() {
    val oggPagesResource = javaClass.classLoader!!.getResource("ogg/ogg_pages.ogg")
    val pages = File(oggPagesResource.path).inputStream().use {
      readOggPages(it).toList()
    }

    pages.shouldContainExactly(
      OggPage(
        continuedPacket = false,
        finishedPacket = true,
        firstPageOfStream = true,
        lastPageOfStream = false,
        absoluteGranulePosition = 0,
        streamSerialNumber = 7891011,
        pageSequenceNumber = 0,
        packets = listOf(
          "asdeceq".toByteArray(),
        ),
      ),
      OggPage(
        continuedPacket = false,
        finishedPacket = false,
        firstPageOfStream = false,
        lastPageOfStream = false,
        absoluteGranulePosition = -1,
        streamSerialNumber = 7891011,
        pageSequenceNumber = 1,
        packets = listOf(
          "b".repeat(255).toByteArray(),
        ),
      ),
      OggPage(
        continuedPacket = true,
        finishedPacket = true,
        firstPageOfStream = false,
        lastPageOfStream = true,
        absoluteGranulePosition = 123459,
        streamSerialNumber = 7891011,
        pageSequenceNumber = 2,
        packets = listOf(
          "bbb".toByteArray(),
          "ae".toByteArray(),
        ),
      ),
    )
  }

  @Test
  fun byteArrayConcatTest() {
    listOf(
      "asd".toByteArray(),
      "hhh".toByteArray(),
      "oiwpier".toByteArray(),
    ).concat()
      .shouldBe("asdhhhoiwpier".toByteArray())
  }

  @Test
  fun oggStreamTest() {
    val pages = listOf(
      OggPage(
        continuedPacket = false,
        finishedPacket = true,
        firstPageOfStream = true,
        lastPageOfStream = false,
        absoluteGranulePosition = 0,
        streamSerialNumber = 7891011,
        pageSequenceNumber = 0,
        packets = listOf(
          "asdeceq".toByteArray(),
          "bbb".toByteArray(),
          "cccc".toByteArray(),
        ),
      ),
      OggPage(
        continuedPacket = false,
        finishedPacket = false,
        firstPageOfStream = false,
        lastPageOfStream = false,
        absoluteGranulePosition = -1,
        streamSerialNumber = 7891011,
        pageSequenceNumber = 1,
        packets = listOf(
          "ddd".toByteArray(),
          "x".repeat(255).toByteArray(),
        ),
      ),
      OggPage(
        continuedPacket = true,
        finishedPacket = false,
        firstPageOfStream = false,
        lastPageOfStream = false,
        absoluteGranulePosition = 123459,
        streamSerialNumber = 7891011,
        pageSequenceNumber = 2,
        packets = listOf(
          "x".repeat(255).toByteArray(),
        ),
      ),
      OggPage(
        continuedPacket = true,
        finishedPacket = false,
        firstPageOfStream = false,
        lastPageOfStream = false,
        absoluteGranulePosition = 123459,
        streamSerialNumber = 7891011,
        pageSequenceNumber = 2,
        packets = listOf(
          "x".toByteArray(),
          "www".toByteArray(),
          "y".repeat(255).toByteArray(),
        ),
      ),
      OggPage(
        continuedPacket = true,
        finishedPacket = true,
        firstPageOfStream = false,
        lastPageOfStream = true,
        absoluteGranulePosition = 123459,
        streamSerialNumber = 7891011,
        pageSequenceNumber = 2,
        packets = listOf(
          "y".toByteArray(),
          "asd".toByteArray(),
        ),
      ),
      OggPage(
        continuedPacket = true,
        finishedPacket = true,
        firstPageOfStream = false,
        lastPageOfStream = true,
        absoluteGranulePosition = 123459,
        streamSerialNumber = 7891011,
        pageSequenceNumber = 2,
        packets = listOf(
          "y".toByteArray(),
          "asd".toByteArray(),
        ),
      ),
    )
    val pagesIt = pages.iterator()
    var numPulled = 0
    val stream = OggStream {
      ++numPulled
      pushPage(pagesIt.next())
    }
    val streamIt = stream.iterator()
    numPulled.shouldBe(0)
    for ((packetStr, expectedNumPulled) in listOf(
      "asdeceq" to 1,
      "bbb" to 1,
      "cccc" to 1,
      "ddd" to 2,
      "x".repeat(255 + 255 + 1) to 4,
      "www" to 4,
      "y".repeat(256) to 5,
      "asd" to 5,
    )) {
      streamIt.next().shouldBe(packetStr.toByteArray())
      numPulled.shouldBe(expectedNumPulled)
    }
    streamIt.hasNext().shouldBe(false)
    pagesIt.hasNext().shouldBe(true)
  }

  @Test
  fun demuxOggStreamsTest() {
    val random = Random

    fun genPage(id: Int) = OggPage(
      continuedPacket = false,
      finishedPacket = true,
      firstPageOfStream = false,
      lastPageOfStream = false,
      absoluteGranulePosition = 0,
      streamSerialNumber = id,
      pageSequenceNumber = 0,
      packets = listOf("v".toByteArray()),
    )

    val numStreams = 10
    val streamIds = (0 until numStreams)
    val firstPages = streamIds
      .map { genPage(it) }
      .map { it.copy(firstPageOfStream = true) }
      .shuffled()

    val pages = (1..numStreams * 30)
      .map { genPage(random.nextInt(numStreams)) }

    val lastPages = streamIds
      .map { genPage(it) }
      .map { it.copy(lastPageOfStream = true) }
      .shuffled()

    val allPages = listOf(firstPages, pages, lastPages).flatten()

    val expectedResults = allPages
      .groupingBy { it.streamSerialNumber }.eachCount()

    val oggStreams = demuxOggStreams(allPages.asSequence())

    val result = streamIds.map { 0 }.toMutableList()
    val left = streamIds.toMutableSet()
    while (!left.isEmpty()) {
      val id = random.nextInt(numStreams)
      if (!left.contains(id)) continue
      if (oggStreams[id]!!.hasNext()) {
        oggStreams[id]!!.next()
        ++result[id]
      } else {
        left.remove(id)
      }
    }

    result.mapIndexed { i, v -> i to v }.toMap()
      .shouldBe(expectedResults)
  }
}
