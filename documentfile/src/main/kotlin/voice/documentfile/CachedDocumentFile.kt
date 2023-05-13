package voice.documentfile

import android.content.Context
import android.net.Uri

class CachedDocumentFile
internal constructor(
  val context: Context,
  val uri: Uri,
  private val preFilledContent: FileContents?,
) {

  constructor(
    context: Context,
    uri: Uri,
  ) : this(context, uri, null)

  override fun toString(): String {
    return "CachedDocumentFile($uri)"
  }

  val children: List<CachedDocumentFile> by lazy {
    if (isDirectory) {
      parseContents(uri, context)
    } else {
      emptyList()
    }
  }

  private val content: FileContents? by lazy {
    preFilledContent ?: context.contentResolver.query(uri, FileContents.columns, null, null, null)?.use { cursor ->
      if (cursor.moveToFirst()) {
        FileContents.readFrom(cursor)
      } else {
        null
      }
    }
  }

  val name: String? by lazy { content?.name }
  val isDirectory: Boolean by lazy { content?.isDirectory ?: false }
  val isFile: Boolean by lazy { content?.isFile ?: false }
  val length: Long by lazy { content?.length ?: 0L }
  val lastModified: Long by lazy { content?.lastModified ?: 0L }
}

fun CachedDocumentFile.nameWithoutExtension(): String {
  val name = name
  return if (name == null) {
    uri.pathSegments.lastOrNull()
      ?.dropWhile { it != ':' }
      ?.removePrefix(":")
      ?.takeUnless { it.isBlank() }
      ?: uri.toString()
  } else {
    name.substringBeforeLast(".")
      .takeUnless { it.isEmpty() }
      ?: name
  }
}
