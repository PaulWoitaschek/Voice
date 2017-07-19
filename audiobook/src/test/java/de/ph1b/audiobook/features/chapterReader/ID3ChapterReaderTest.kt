package de.ph1b.audiobook.features.chapterReader

import de.ph1b.audiobook.features.chapterReader.id3.ID3ChapterReader
import de.ph1b.audiobook.misc.toMap
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test for the id3 chapter reader
 */
@RunWith(RobolectricTestRunner::class)
class ID3ChapterReaderTest {

  @Test
  fun readInputStream() {
    val client = OkHttpClient.Builder().build()
    val request = Request.Builder()
        .url("https://auphonic.com/media/blog/auphonic_chapters_demo.mp3")
        .build()
    val call = client.newCall(request)
    val inputStream = call.execute().body()!!.byteStream()
    val chapters = ID3ChapterReader.readInputStream(inputStream)

    assertThat(chapters.toMap()).isEqualTo(mapOf(
        0 to "Intro",
        15000 to "Creating a new production",
        22000 to "Sound analysis",
        34000 to "Adaptive leveler",
        45000 to "Global loudness normalization",
        60000 to "Audio restoration algorithms",
        76000 to "Output file formats",
        94000 to "External services",
        111500 to "Get a free account!"
    ))
  }
}
