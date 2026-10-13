package voice.core.data

import voice.core.documentfile.CachedDocumentFile
import voice.core.documentfile.walk

// A book keeps all of its chapter ids in a single row, and Android reads a row through a 2 MiB cursor window.
// A row that doesn't fit can never be read again, so loading the library would crash on every start.
public const val MAX_CHAPTER_IDS_BYTES: Int = 2_000_000

public fun List<ChapterId>.fitIntoOneBook(): Boolean {
  // the size of the json array they are stored as
  val bytes = sumOf { it.value.encodeToByteArray().size + 3 } + 1
  return bytes <= MAX_CHAPTER_IDS_BYTES
}

public fun CachedDocumentFile.audioFilesFitIntoOneBook(): Boolean {
  return walk()
    .filter { it.isAudioFile() }
    .map { ChapterId(it.uri) }
    .toList()
    .fitIntoOneBook()
}
