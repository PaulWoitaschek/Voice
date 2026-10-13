package voice.core.scanner

import voice.core.data.Chapter
import voice.core.data.ChapterId

/**
 * What stays the same when an audio file is moved or its folder is renamed.
 */
internal data class FileFingerprint(
  val name: String,
  val size: Long,
)

internal fun Chapter.fingerprint(): FileFingerprint? = fileName?.let { FileFingerprint(name = it, size = fileSize) }

/**
 * The chapters by their file. Files that can't be told apart are left out.
 */
internal fun List<Chapter>.byFingerprint(): Map<FileFingerprint, ChapterId> {
  return groupBy { it.fingerprint() }
    .mapNotNull { (fingerprint, chapters) ->
      val chapter = chapters.singleOrNull()
      if (fingerprint == null || chapter == null) null else fingerprint to chapter.id
    }
    .toMap()
}
