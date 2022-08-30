package voice.folderPicker.selectType

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import voice.data.supportedAudioFormats

internal class DocumentFileCache {

  private val cache = mutableMapOf<Uri, CachedDocumentFile>()

  fun DocumentFile.cached(): CachedDocumentFile {
    return cache.getOrPut(uri) { CachedDocumentFile(this) }
  }

  inner class CachedDocumentFile(private val documentFile: DocumentFile) {

    val children: List<CachedDocumentFile> by lazy {
      documentFile.listFiles().map { it.cached() }
    }

    val name: String? by lazy { documentFile.name }

    val isDirectory: Boolean by lazy { documentFile.isDirectory }
    private val isFile: Boolean by lazy { documentFile.isFile }
    val length: Long by lazy { documentFile.length() }

    private fun walk(): Sequence<CachedDocumentFile> = sequence {
      suspend fun SequenceScope<CachedDocumentFile>.walk(file: CachedDocumentFile) {
        yield(file)
        if (file.isDirectory) {
          file.children.forEach { walk(it) }
        }
      }
      walk(this@CachedDocumentFile)
    }

    fun isAudioFile(): Boolean {
      if (!isFile) return false
      val name = name ?: return false
      val extension = name.substringAfterLast(".").lowercase()
      return extension in supportedAudioFormats
    }

    fun audioFileCount(): Int {
      return walk().count {
        it.isAudioFile()
      }
    }
  }
}
