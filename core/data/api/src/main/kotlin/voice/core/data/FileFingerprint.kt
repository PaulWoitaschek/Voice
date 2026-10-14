package voice.core.data

import android.provider.DocumentsContract
import androidx.core.net.toUri
import java.security.MessageDigest

/**
 * What stays the same when an audio file is moved or its folder is renamed, or is copied to another device.
 */
public data class FileFingerprint(
  val name: String,
  val size: Long,
)

public fun List<Chapter>.fingerprints(): List<FileFingerprint>? = map { it.fingerprint() ?: return null }

public fun List<Chapter>.syncKey(): String? {
  val fingerprints = fingerprints()?.takeIf { it.isNotEmpty() } ?: return null
  // a file name can't contain a slash
  val text = fingerprints.joinToString(separator = "") { "${it.name}/${it.size}\n" }
  return MessageDigest.getInstance("SHA-256")
    .digest(text.toByteArray())
    .joinToString(separator = "") { "%02x".format(it) }
}

private fun Chapter.fingerprint(): FileFingerprint? {
  // some providers don't report sizes, and a name like 01.mp3 alone says nothing
  if (fileSize <= 0) return null
  return id.fileName()?.let { FileFingerprint(name = it, size = fileSize) }
}

// other providers have ids without the path, which usually stay the same when a file moves
private fun ChapterId.fileName(): String? {
  val uri = value.toUri()
  return when {
    uri.scheme == "file" -> uri.lastPathSegment
    uri.authority == "com.android.externalstorage.documents" -> try {
      DocumentsContract.getDocumentId(uri).substringAfter(':').substringAfterLast('/')
    } catch (_: IllegalArgumentException) {
      null
    }
    else -> null
  }?.takeIf { it.isNotEmpty() }
}
