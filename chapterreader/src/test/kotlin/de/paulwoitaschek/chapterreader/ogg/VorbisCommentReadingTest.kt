package de.paulwoitaschek.chapterreader.ogg

import de.paulwoitaschek.chapterreader.Chapter
import de.paulwoitaschek.chapterreader.ogg.vorbisComment.VorbisComment
import de.paulwoitaschek.chapterreader.ogg.vorbisComment.VorbisCommentParseException
import de.paulwoitaschek.chapterreader.ogg.vorbisComment.VorbisCommentReader
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.io.ByteArrayInputStream

class VorbisCommentReadingTest {

  @Test
  fun parseChapterTime() {
    VorbisCommentReader.parseChapterTime("10:02:10.231") shouldBe 36130231
    VorbisCommentReader.parseChapterTime("110:2:10.23") shouldBe 396130230
    VorbisCommentReader.parseChapterTime("0:0:0.11111") shouldBe 111
    VorbisCommentReader.parseChapterTime("asdasd") shouldBe null
  }

  @Test
  fun vorbisCommentChapters() {
    VorbisComment(
      "",
      mapOf(
        "lala" to "asdasd",
      ),
    ).asChapters().size shouldBe 0

    VorbisComment(
      "",
      mapOf(
        "CHAPTER001" to "00:00:01.000",
        "CHAPTER001NAME" to "C1",
        "CHAPTER002" to "00:00:02.000",
        "CHAPTER002NAME" to "C2",
        "CHAPTER004" to "00:00:04.000",
        "CHAPTER004NAME" to "C4",
      ),
    ).asChapters() shouldContainExactly listOf(Chapter(1000L, "C1"), Chapter(2000L, "C2"))

    VorbisComment(
      "",
      mapOf(
        "CHAPTER001" to "00:00:01.000",
        "CHAPTER001NAME" to "C1",
        "CHAPTER002" to "00:00:02.000",
      ),
    ).asChapters().size shouldBe 0

    VorbisComment(
      "",
      mapOf(
        "CHAPTER001" to "00:00:01.000",
        "CHAPTER001NAME" to "C1",
        "CHAPTER002" to "00:00:02.00d0",
        "CHAPTER002NAME" to "C2",
      ),
    ).asChapters().size shouldBe 0
  }

  private fun decodeHex(hex: String) = DatatypeConverter.parseHexBinary(hex)

  @Test
  fun parseVorbisComment() {
    val stream1 =
      ByteArrayInputStream(
        decodeHex("0d00000076656e646f7220737472696e670300000005000000613d6173640a0000005449544c453d7465787407000000757466383dcf80"),
      )
    VorbisCommentReader.readComment(stream1) shouldBe VorbisComment(
      vendor = "vendor string",
      comments = mapOf(
        "A" to "asd",
        "TITLE" to "text",
        "UTF8" to "π",
      ),
    )

    val stream2 = ByteArrayInputStream(decodeHex("000000000200000005000000613d61736406000000617364617364"))
    val thrown = try {
      VorbisCommentReader.readComment(stream2)
      false
    } catch (_: VorbisCommentParseException) {
      true
    }
    thrown.shouldBeTrue()
  }
}
