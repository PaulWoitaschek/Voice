package de.ph1b.audiobook.features.chapterReader

import de.ph1b.audiobook.misc.toMap
import org.assertj.core.api.Assertions.*
import org.bouncycastle.util.encoders.Hex
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
class VorbisCommentReadingTest {
  @Test
  fun parseChapterTime() {
    assertThat(parseVorbisCommentChapterTime("10:02:10.231")).isEqualTo(36130231)
    assertThat(parseVorbisCommentChapterTime("110:2:10.23")).isEqualTo(396130230)
    assertThat(parseVorbisCommentChapterTime("0:0:0.11111")).isEqualTo(111)
    assertThat(parseVorbisCommentChapterTime("asdasd")).isEqualTo(null)
  }

  @Test
  fun vorbisCommentChapters() {
    assertThat(VorbisComment("", mapOf(
        "lala" to "asdasd"
    )).chapters.size()).isEqualTo(0)

    assertThat(VorbisComment("", mapOf(
        "CHAPTER001" to "00:00:01.000",
        "CHAPTER001NAME" to "C1",
        "CHAPTER002" to "00:00:02.000",
        "CHAPTER002NAME" to "C2",
        "CHAPTER004" to "00:00:04.000",
        "CHAPTER004NAME" to "C4"
    )).chapters.toMap()).isEqualTo(mapOf(
        1000 to "C1",
        2000 to "C2"
    ))

    assertThat(VorbisComment("", mapOf(
        "CHAPTER001" to "00:00:01.000",
        "CHAPTER001NAME" to "C1",
        "CHAPTER002" to "00:00:02.000"
    )).chapters.size()).isEqualTo(0)

    assertThat(VorbisComment("", mapOf(
        "CHAPTER001" to "00:00:01.000",
        "CHAPTER001NAME" to "C1",
        "CHAPTER002" to "00:00:02.00d0",
        "CHAPTER002NAME" to "C2"
    )).chapters.size()).isEqualTo(0)
  }

  @Test
  fun parseVorbisComment() {
    val stream1 = ByteArrayInputStream(Hex.decode("0d00000076656e646f7220737472696e670300000005000000613d6173640a0000005449544c453d7465787407000000757466383dcf80"))
    assertThat(readVorbisComment(stream1)).isEqualTo(VorbisComment(
        vendor = "vendor string",
        comments = mapOf(
            "A" to "asd",
            "TITLE" to "text",
            "UTF8" to "π"
        )
    ))

    val stream2 = ByteArrayInputStream(Hex.decode("000000000200000005000000613d61736406000000617364617364"))
    try {
      readVorbisComment(stream2)
      failBecauseExceptionWasNotThrown(VorbisCommentParseException::class.java)
    } catch (_: VorbisCommentParseException) {}
  }
}
