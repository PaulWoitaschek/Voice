package de.ph1b.audiobook.features

import android.content.Context
import android.graphics.Bitmap
import de.ph1b.audiobook.common.ImageHelper
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.repo.BookRepo2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

class CoverSaver
@Inject constructor(
  private val imageHelper: ImageHelper,
  private val repo: BookRepo2,
  private val context: Context,
) {

  suspend fun save(bookId: Book2.Id, cover: Bitmap) {
    val coversFolder = withContext(Dispatchers.IO) {
      File(context.filesDir, "bookCovers")
        .also { coverFolder -> coverFolder.mkdirs() }
    }
    val newCover = File(coversFolder, "${UUID.randomUUID()}.webp")
    imageHelper.saveCover(cover, newCover)

    val oldCover = repo.flow(bookId).first()?.content?.cover
    if (oldCover != null) {
      withContext(Dispatchers.IO) {
        oldCover.delete()
      }
    }

    repo.updateBook(bookId) {
      it.copy(cover = newCover)
    }
  }
}
