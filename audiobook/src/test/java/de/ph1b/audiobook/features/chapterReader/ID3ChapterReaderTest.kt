package de.ph1b.audiobook.features.chapterReader

import de.ph1b.audiobook.misc.forEachIndexed
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

    val positions = ArrayList<Int>()
    val titles = ArrayList<String>()
    chapters.forEachIndexed { _, key, value ->
      positions.add(key)
      titles.add(value)
    }

    assertThat(positions)
        .isEqualTo(listOf(
            0,
            15000,
            22000,
            34000,
            45000,
            60000,
            76000,
            94000,
            111500)
        )

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
