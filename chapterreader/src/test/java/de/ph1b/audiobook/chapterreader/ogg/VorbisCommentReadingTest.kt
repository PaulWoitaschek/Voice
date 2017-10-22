package de.ph1b.audiobook.chapterreader.ogg

import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.chapterreader.ogg.vorbisComment.VorbisComment
import de.ph1b.audiobook.chapterreader.ogg.vorbisComment.VorbisCommentParseException
import de.ph1b.audiobook.chapterreader.ogg.vorbisComment.VorbisCommentReader
import org.junit.Test
import java.io.ByteArrayInputStream

class VorbisCommentReadingTest {

  @Test
  fun parseChapterTime() {
    assertThat(VorbisCommentReader.parseChapterTime("10:02:10.231")).isEqualTo(36130231)
    assertThat(VorbisCommentReader.parseChapterTime("110:2:10.23")).isEqualTo(396130230)
    assertThat(VorbisCommentReader.parseChapterTime("0:0:0.11111")).isEqualTo(111)
    assertThat(VorbisCommentReader.parseChapterTime("asdasd")).isNull()
  }

  @Test
  fun vorbisCommentChapters() {
    assertThat(
        VorbisComment(
            "", mapOf(
            "lala" to "asdasd"
        )
        ).chapters.size
    ).isEqualTo(0)

    assertThat(
        VorbisComment(
            "", mapOf(
            "CHAPTER001" to "00:00:01.000",
            "CHAPTER001NAME" to "C1",
            "CHAPTER002" to "00:00:02.000",
            "CHAPTER002NAME" to "C2",
            "CHAPTER004" to "00:00:04.000",
            "CHAPTER004NAME" to "C4"
        )
        ).chapters
    ).isEqualTo(
        mapOf(
            1000 to "C1",
            2000 to "C2"
        )
    )

    assertThat(
        VorbisComment(
            "", mapOf(
            "CHAPTER001" to "00:00:01.000",
            "CHAPTER001NAME" to "C1",
            "CHAPTER002" to "00:00:02.000"
        )
        ).chapters.size
    ).isEqualTo(0)

    assertThat(
        VorbisComment(
            "", mapOf(
            "CHAPTER001" to "00:00:01.000",
            "CHAPTER001NAME" to "C1",
            "CHAPTER002" to "00:00:02.00d0",
            "CHAPTER002NAME" to "C2"
        )
        ).chapters.size
    ).isEqualTo(0)
  }

  private fun decodeHex(hex: String) = DatatypeConverter.parseHexBinary(hex)

  @Test
  fun parseVorbisComment() {
    val stream1 = ByteArrayInputStream(decodeHex("0d00000076656e646f7220737472696e670300000005000000613d6173640a0000005449544c453d7465787407000000757466383dcf80"))
    assertThat(VorbisCommentReader.readComment(stream1)).isEqualTo(
        VorbisComment(
            vendor = "vendor string",
            comments = mapOf(
                "A" to "asd",
                "TITLE" to "text",
                "UTF8" to "π"
            )
        )
    )

    val stream2 = ByteArrayInputStream(decodeHex("000000000200000005000000613d61736406000000617364617364"))
    val thrown = try {
      VorbisCommentReader.readComment(stream2)
      false
    } catch (_: VorbisCommentParseException) {
      true
    }
    assertThat(thrown).isTrue()
  }
}
