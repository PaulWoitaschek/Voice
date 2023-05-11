package voice.documentfile

import android.content.Context
import android.net.Uri
import javax.inject.Inject

class CachedDocumentFileSystem
@Inject constructor(
  internal val context: Context,
) {

  private val cache = mutableMapOf<Uri, CachedDocumentFile>()

  fun file(uri: Uri): CachedDocumentFile {
    return cache.getOrPut(uri) {
      CachedDocumentFile(this, uri)
    }
  }
}
