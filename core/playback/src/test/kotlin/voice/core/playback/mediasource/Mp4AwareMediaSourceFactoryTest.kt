package voice.core.playback.mediasource

import android.net.Uri
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.ClippingMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.test.utils.FakeMediaSource
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
class Mp4AwareMediaSourceFactoryTest {

  private val default = RecordingFactory()
  private val mp4 = RecordingFactory()
  private val factory = Mp4AwareMediaSourceFactory(default = default, mp4 = mp4)

  @Test
  fun `routes mp4 containers to the platform extractor`() {
    val uris = listOf(
      "file:///books/book.mp4",
      "file:///books/book.m4a",
      "file:///books/book.m4b",
      "content://com.android.externalstorage.documents/document/primary%3ABooks%2Fbook.m4b",
    )
    uris.forEach { factory.createMediaSource(mediaItem(it)) }

    assertEquals(expected = uris.map(Uri::parse), actual = mp4.mediaItems.map { it.uri() })
    assertEquals(expected = emptyList(), actual = default.mediaItems)
  }

  @Test
  fun `routes everything else to the default factory`() {
    val uris = listOf(
      "file:///books/book.mp3",
      "file:///books/book.ogg",
      "file:///books/book.opus",
      "file:///books/book.mka",
      "file:///books/book",
    )
    uris.forEach { factory.createMediaSource(mediaItem(it)) }

    assertEquals(expected = uris.map(Uri::parse), actual = default.mediaItems.map { it.uri() })
    assertEquals(expected = emptyList(), actual = mp4.mediaItems)
  }

  @Test
  fun `clips mp4 sources itself`() {
    val source = factory.createMediaSource(
      mediaItem("file:///books/book.m4b", startMs = 2_000, endMs = 6_000),
    )

    // ProgressiveMediaSource.Factory ignores the clipping configuration, so the chapter mark would
    // otherwise play from the start of the file and never end.
    assertIs<ClippingMediaSource>(source)
    assertEquals(expected = 4_000, actual = source.preparedDurationMs())
  }

  @Test
  fun `leaves unclipped mp4 sources alone`() {
    val source = factory.createMediaSource(mediaItem("file:///books/book.m4b"))

    assertSame(expected = mp4.created.single(), actual = source)
  }

  @Test
  fun `leaves clipping of other containers to the default factory`() {
    val source = factory.createMediaSource(
      mediaItem("file:///books/book.mp3", startMs = 5_000, endMs = 12_000),
    )

    assertSame(expected = default.created.single(), actual = source)
  }

  private fun mediaItem(
    uri: String,
    startMs: Long = 0,
    endMs: Long = C.TIME_END_OF_SOURCE,
  ): MediaItem = MediaItem.Builder()
    .setUri(uri)
    .setClippingConfiguration(
      MediaItem.ClippingConfiguration.Builder()
        .setStartPositionMs(startMs)
        .setEndPositionMs(endMs)
        .build(),
    )
    .build()

  private fun MediaItem.uri(): Uri? = localConfiguration?.uri

  private fun MediaSource.preparedDurationMs(): Long {
    var timeline: Timeline? = null
    prepareSource({ _, refreshed -> timeline = refreshed }, null, PlayerId.UNSET)
    shadowOf(Looper.getMainLooper()).idle()
    return timeline!!.getWindow(0, Timeline.Window()).durationMs
  }

  private class RecordingFactory : MediaSource.Factory {

    val mediaItems = mutableListOf<MediaItem>()
    val created = mutableListOf<MediaSource>()

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
      mediaItems += mediaItem
      return FakeMediaSource().also { created += it }
    }

    override fun getSupportedTypes(): IntArray = intArrayOf(C.CONTENT_TYPE_OTHER)

    override fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider): MediaSource.Factory = this

    override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory = this
  }
}
