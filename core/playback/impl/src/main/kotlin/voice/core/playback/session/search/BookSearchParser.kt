package voice.core.playback.session.search

import android.os.Bundle
import android.provider.MediaStore
import dev.zacsweers.metro.Inject

@Inject
class BookSearchParser {

  fun parse(
    query: String?,
    extras: Bundle?,
  ): VoiceSearch {
    return VoiceSearch(
      query = query,
      mediaFocus = extras?.getString(MediaStore.EXTRA_MEDIA_FOCUS),
      album = extras?.getString(MediaStore.EXTRA_MEDIA_ALBUM),
      artist = extras?.getString(MediaStore.EXTRA_MEDIA_ARTIST),
    )
  }
}
