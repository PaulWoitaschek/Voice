package voice.core.documentfile

import android.net.Uri

interface CachedDocumentFileFactory {
  fun create(uri: Uri): CachedDocumentFile
}
