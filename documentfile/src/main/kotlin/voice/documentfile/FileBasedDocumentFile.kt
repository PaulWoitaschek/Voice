package voice.documentfile

import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import java.io.File

data class FileBasedDocumentFile(private val file: File) : CachedDocumentFile {

  override val children: List<CachedDocumentFile> get() = file.listFiles()?.map { FileBasedDocumentFile(it) } ?: emptyList()
  override val name: String? get() = file.name
  override val isDirectory: Boolean get() = file.isDirectory
  override val isFile: Boolean get() = file.isFile
  override val length: Long get() = file.length()
  override val lastModified: Long get() = file.lastModified()
  override val uri: Uri get() = file.toUri()
}

object FileBasedDocumentFactory : CachedDocumentFileFactory {
  override fun create(uri: Uri): CachedDocumentFile {
    return FileBasedDocumentFile(uri.toFile())
  }
}
