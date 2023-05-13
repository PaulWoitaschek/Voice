package voice.documentfile

fun CachedDocumentFile.walk(): Sequence<CachedDocumentFile> = sequence {
  suspend fun SequenceScope<CachedDocumentFile>.walk(file: CachedDocumentFile) {
    yield(file)
    if (file.isDirectory) {
      file.children.forEach { walk(it) }
    }
  }
  walk(this@walk)
}
