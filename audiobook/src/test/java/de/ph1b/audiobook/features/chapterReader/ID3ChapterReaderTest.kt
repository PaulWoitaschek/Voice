package de.ph1b.audiobook.features.chapterReader

import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Test for the id3 chapter reader
 *
 * @author Paul Woitaschek
 */
class ID3ChapterReaderTest {

  @Test
  fun readInputStream() {
    val client = OkHttpClient.Builder().build()
    val request = Request.Builder()
        .url("https://auphonic.com/media/blog/auphonic_chapters_demo.mp3")
        .build()
    val call = client.newCall(request)
    val inputStream = call.execute().body().byteStream()
    val chapters = ID3ChapterReader.readInputStream(inputStream)

    val positions = chapters.map { it.start }
    assertThat(positions)
        .isEqualTo(listOf(
            0L,
            15000L,
            22000L,
            34000L,
            45000L,
            60000L,
            76000L,
            94000L,
            111500L)
        )

    val titles = chapters.map { it.title }
    assertThat(titles)
        .isEqualTo(listOf(
            "Intro",
            "Creating a new production",
            "Sound analysis", "Adaptive leveler",
            "Global loudness normalization",
            "Audio restoration algorithms",
            "Output file formats",
            "External services",
            "Get a free account!")
        )
  }
}