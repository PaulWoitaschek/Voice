package voice.core.scanner

import androidx.media3.common.Metadata.Entry
import androidx.media3.container.MdtaMetadataEntry
import androidx.media3.extractor.metadata.id3.BinaryFrame
import androidx.media3.extractor.metadata.id3.InternalFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

internal class TagReaderTest {

  @Test
  fun `m4b movement`() {
    assertEquals(
      expected = "The Stormlight Archive" to "2",
      actual = seriesAndPart(text("MVNM", "The Stormlight Archive"), text("MVIN", "2")),
    )
  }

  @Test
  fun `m4b custom fields`() {
    assertEquals(
      expected = "The Stormlight Archive" to "2",
      actual =
        seriesAndPart(
          InternalFrame("com.apple.iTunes", "SERIES", "The Stormlight Archive"),
          InternalFrame("com.apple.iTunes", "SERIES-PART", "2"),
        ),
    )
  }

  @Test
  fun `m4b tv show`() {
    assertEquals(expected = "Die drei ???" to null, actual = seriesAndPart(text("TVSHOW", "Die drei ???")))
  }

  @Test
  fun `m4b metadata keys`() {
    assertEquals(
      expected = "The Stormlight Archive" to "2",
      actual = seriesAndPart(mdta("series", "The Stormlight Archive"), mdta("series-part", "2")),
    )
  }

  @Test
  fun `m4b metadata keys that aren't text are skipped`() {
    assertEquals(
      expected = "The Stormlight Archive" to null,
      actual =
        seriesAndPart(
          mdta("series", "The Stormlight Archive"),
          MdtaMetadataEntry("series-part", byteArrayOf(0, 0, 0, 2), MdtaMetadataEntry.TYPE_INDICATOR_INT32),
        ),
    )
  }

  @Test
  fun `mp3 movement frames`() {
    assertEquals(
      expected = "Discworld" to "3",
      actual =
        seriesAndPart(
          BinaryFrame("MVNM", byteArrayOf(3) + "Discworld\u0000".toByteArray(Charsets.UTF_8)),
          BinaryFrame("MVIN", byteArrayOf(0) + "3/41".toByteArray(Charsets.ISO_8859_1)),
        ),
    )
  }

  @Test
  fun `mp3 movement frame in utf-16`() {
    val littleEndianWithBom = byteArrayOf(1, 0xFF.toByte(), 0xFE.toByte()) + "Discworld\u0000".toByteArray(Charsets.UTF_16LE)
    assertEquals(expected = "Discworld" to null, actual = seriesAndPart(BinaryFrame("MVNM", littleEndianWithBom)))
  }

  @Test
  fun `mp3 custom fields`() {
    assertEquals(
      expected = "The Stormlight Archive" to "2",
      actual = seriesAndPart(txxx("SERIES", "The Stormlight Archive"), txxx("SERIES-PART", "2")),
    )
  }

  @Test
  fun `vorbis comments in any case`() {
    assertEquals(
      expected = "The Stormlight Archive" to "2",
      actual = seriesAndPart(VorbisComment("Series", "The Stormlight Archive"), VorbisComment("series-part", "2")),
    )
    assertEquals(
      expected = "Discworld" to "3",
      actual = seriesAndPart(VorbisComment("MOVEMENTNAME", "Discworld"), VorbisComment("MOVEMENT", "3")),
    )
  }

  @Test
  fun `grouping with a part`() {
    assertEquals(
      expected = "The Stormlight Archive" to "2",
      actual = seriesAndPart(text("TIT1", "The Stormlight Archive #2; Cosmere #4")),
    )
    assertEquals(
      expected = "The Stormlight Archive" to "2.5",
      actual = seriesAndPart(BinaryFrame("GRP1", byteArrayOf(3) + "The Stormlight Archive #2.5".toByteArray())),
    )
    assertEquals(
      expected = "The Stormlight Archive" to "2",
      actual = seriesAndPart(VorbisComment("GROUPING", "The Stormlight Archive #2")),
    )
    assertEquals(
      expected = "The Stormlight Archive" to "2",
      actual = seriesAndPart(text("TIT1", "Cosmere; The Stormlight Archive #2")),
    )
  }

  @Test
  fun `the mp3 grouping wins over the work`() {
    assertEquals(
      expected = "The Stormlight Archive" to "2",
      actual =
        seriesAndPart(
          text("TIT1", "Cosmere #4"),
          BinaryFrame("GRP1", byteArrayOf(3) + "The Stormlight Archive #2".toByteArray()),
        ),
    )
  }

  @Test
  fun `grouping without a part is no series`() {
    assertEquals(expected = null to null, actual = seriesAndPart(text("TIT1", "Fantasy")))
  }

  @Test
  fun `series tag wins over show, movement and grouping`() {
    assertEquals(
      expected = "The Stormlight Archive" to "2",
      actual =
        seriesAndPart(
          text("TIT1", "Cosmere #4"),
          text("MVNM", "Cosmere"),
          text("MVIN", "4"),
          text("TVSHOW", "Brandon Sanderson"),
          txxx("SERIES", "The Stormlight Archive"),
          txxx("SERIES-PART", "2"),
        ),
    )
  }

  @Test
  fun `a series doesn't take the part of another one`() {
    assertEquals(
      expected = "The Stormlight Archive" to null,
      actual = seriesAndPart(txxx("SERIES", "The Stormlight Archive"), text("MVNM", "Cosmere"), text("MVIN", "4")),
    )
  }

  @Test
  fun `a part without a series is dropped`() {
    assertEquals(expected = null to null, actual = seriesAndPart(VorbisComment("MOVEMENT", "2")))
  }

  @Test
  fun `several series in one tag`() {
    assertEquals(
      expected = "The Stormlight Archive" to "2",
      actual = seriesAndPart(txxx("SERIES", "The Stormlight Archive; Cosmere"), txxx("SERIES-PART", "2; 4")),
    )
  }

  @Test
  fun `only the movement loses its count`() {
    assertEquals(
      expected = "Die drei ???" to "S01/E05",
      actual = seriesAndPart(text("TVSHOW", "Die drei ???"), txxx("EPISODE_ID", "S01/E05")),
    )
  }

  @Test
  fun `show wins over movement`() {
    assertEquals(
      expected = "Die drei ???" to "63",
      actual = seriesAndPart(text("MVNM", "Hörspiele"), text("TVSHOW", "Die drei ???"), txxx("EPISODE_ID", "63")),
    )
  }

  @Test
  fun `blank tags don't count`() {
    assertEquals(
      expected = "Discworld" to "3",
      actual = seriesAndPart(txxx("SERIES", " "), txxx("SERIES-PART", ""), text("MVNM", " Discworld "), text("MVIN", "3")),
    )
  }

  private fun text(
    id: String,
    value: String,
  ) = TextInformationFrame(id, null, listOf(value))

  private fun txxx(
    description: String,
    value: String,
  ) = TextInformationFrame("TXXX", description, listOf(value))

  private fun mdta(
    key: String,
    value: String,
  ) = MdtaMetadataEntry(key, value.toByteArray(), MdtaMetadataEntry.TYPE_INDICATOR_STRING)

  private fun seriesAndPart(vararg entries: Entry): Pair<String?, String?> {
    val builder = Metadata.Builder(fileName = "file")
    entries.forEach { builder.read(it) }
    val metadata = builder.build(1.minutes)
    return metadata.series to metadata.part
  }
}
