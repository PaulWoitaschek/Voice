package voice.documentfile

import android.content.Context
import android.net.Uri

internal data class RealCachedDocumentFile(
  val context: Context,
  override val uri: Uri,
  private val preFilledContent: FileContents?,
) : CachedDocumentFile {

  override val children: List<CachedDocumentFile> by lazy {
    if (isDirectory) {
      parseContents(uri, context)
    } else {
      emptyList()
    }
  }

  private val content: FileContents? by lazy {
    preFilledContent ?: FileContents.query(context, uri)
  }

  override val name: String? by lazy { content?.name }
  override val isDirectory: Boolean by lazy { content?.isDirectory ?: false }
  override val isFile: Boolean by lazy { content?.isFile ?: false }
  override val length: Long by lazy { content?.length ?: 0L }
  override val lastModified: Long by lazy { content?.lastModified ?: 0L }
}
