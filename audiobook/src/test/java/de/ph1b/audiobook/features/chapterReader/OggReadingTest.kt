package de.ph1b.audiobook.features.chapterReader

import org.assertj.core.api.Assertions.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.*

private fun unsignedByteArrayOf(vararg values: Int) = values.map { it.toByte() }.toByteArray()

private val shuffleRandom = Random()

private fun <T> Iterable<T>.shuffle(): List<T> {
  val res = this.toMutableList()
  res.forEachIndexed { i, v ->
    val pos = shuffleRandom.nextInt(res.size)
    res[i] = res[pos]
    res[pos] = v
  }
  return res
}

@RunWith(RobolectricTestRunner::class)
class OggReadingTest {
  @Test
  fun computePacketSizesFromSegmentTableTest() {
    assertThat(computePacketSizesFromSegmentTable(unsignedByteArrayOf(
        255, 255, 14, 255, 0, 255, 255, 17)))
        .isEqualTo(listOf(2*255 + 14, 255, 2*255 + 17))
    assertThat(computePacketSizesFromSegmentTable(unsignedByteArrayOf(
        255, 255, 255, 255, 70, 255, 255)))
        .isEqualTo(listOf(4*255 + 70, 2*255))
  }

  @Test
  fun readOggPagesTest() {
    val oggPagesResource = javaClass.classLoader.getResource("oggChapterReader/ogg_pages.ogg")
    val pages = File(oggPagesResource.path).inputStream().use {
      readOggPages(it).toList()
    }

    assertThat(pages).isEqualTo(listOf(
        OggPage(
            continuedPacket = false,
            finishedPacket = true,
            firstPageOfStream = true,
            lastPageOfStream = false,
            absoluteGranulePosition = 0,
            streamSerialNumber = 7891011,
            pageSequenceNumber = 0,
            packets = listOf(
                "asdeceq".toByteArray()
            )
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
                "b".repeat(255).toByteArray()
            )
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
                "ae".toByteArray()
            )
        )
    ))
  }

  @Test
  fun byteArrayConcatTest() {
    assertThat(listOf(
        "asd".toByteArray(),
        "hhh".toByteArray(),
        "oiwpier".toByteArray()).concat())
        .isEqualTo("asdhhhoiwpier".toByteArray())
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
                "cccc".toByteArray()
            )
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
                "x".repeat(255).toByteArray()
            )
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
                "x".repeat(255).toByteArray()
            )
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
                "y".repeat(255).toByteArray()
            )
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
                "asd".toByteArray()
            )
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
                "asd".toByteArray()
            )
        )
    )
    val pagesIt = pages.iterator()
    var numPulled = 0
    val stream = OggStream({
      ++numPulled
      pushPage(pagesIt.next())
    })
    val streamIt = stream.iterator()
    assertThat(numPulled).isEqualTo(0)
    for ((packetStr, expectedNumPulled) in listOf(
        "asdeceq" to 1,
        "bbb" to 1,
        "cccc" to 1,
        "ddd" to 2,
        "x".repeat(255 + 255 + 1) to 4,
        "www" to 4,
        "y".repeat(256) to 5,
        "asd" to 5
    )) {
      assertThat(streamIt.next()).isEqualTo(packetStr.toByteArray())
      assertThat(numPulled).isEqualTo(expectedNumPulled)
    }
    assertThat(streamIt.hasNext()).isFalse()
    assertThat(pagesIt.hasNext()).isTrue()
  }

  @Test
  fun demuxOggStreamsTest() {
    val random = Random()

    fun genPage(id: Int) = OggPage(
          continuedPacket = false,
          finishedPacket = true,
          firstPageOfStream = false,
          lastPageOfStream = false,
          absoluteGranulePosition = 0,
          streamSerialNumber = id,
          pageSequenceNumber = 0,
          packets = listOf("v".toByteArray())
      )

    val numStreams = 10
    val streamIds = (0..numStreams - 1)
    val firstPages = streamIds
        .map { genPage(it) }
        .map { it.copy(firstPageOfStream = true) }
        .shuffle()

    val pages = (1..numStreams * 30)
        .map { genPage(random.nextInt(numStreams)) }

    val lastPages = streamIds
        .map { genPage(it) }
        .map { it.copy(lastPageOfStream = true) }
        .shuffle()

    val allPages = listOf(firstPages, pages, lastPages).flatten()

    val expectedResults = allPages
        .groupingBy { it.streamSerialNumber }.eachCount()

    val oggStreams = demuxOggStreams(allPages.asSequence())

    val result = streamIds.map { 0 }.toMutableList()
    val left = streamIds.toMutableSet()
    while (!left.isEmpty()) {
      val id = random.nextInt(numStreams)
      if (!left.contains(id)) continue
      if (oggStreams[id].hasNext()) {
        oggStreams[id].next()
        ++result[id]
      } else {
        left.remove(id)
      }
    }

    assertThat(result.mapIndexed { i, v -> i to v}.toMap())
        .isEqualTo(expectedResults)
  }
}
