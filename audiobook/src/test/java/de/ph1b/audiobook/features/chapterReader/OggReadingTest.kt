package de.ph1b.audiobook.features.chapterReader

import org.assertj.core.api.Assertions.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

private fun unsignedByteArrayOf(vararg values: Int) = values.map { it.toByte() }.toByteArray()

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
}
