package voice.documentfile

import android.content.Context
import android.net.Uri
import javax.inject.Inject

internal data class Properties(
  val name: String?,
  val isFile: Boolean,
  val isDirectory: Boolean,
  val length: Long,
)

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
