package voice.core.documentfile

import android.net.Uri

interface CachedDocumentFile {
  val children: List<CachedDocumentFile>
  val name: String?
  val isDirectory: Boolean
  val isFile: Boolean
  val length: Long
  val lastModified: Long
  val uri: Uri
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
