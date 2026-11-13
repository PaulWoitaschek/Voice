package voice.features.bookOverview.select

import dev.zacsweers.metro.Inject
import voice.core.audiobookshelf.ServerBookEdit
import voice.core.audiobookshelf.ServerEdit
import voice.core.audiobookshelf.ServerLibrary
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.BookMetadata
import voice.core.data.CustomMetadata
import voice.core.data.isRemote
import voice.core.data.repo.BookRepository

enum class BookField {
  Series,
  Author,
}

internal data class BooksEdited(
  val field: BookField,
  val count: Int,
  val failed: Set<BookId>,
  // the account can't change books, so trying again doesn't help
  val notAllowed: Boolean,
  val undo: Map<BookId, String?>,
)

@Inject
class SelectedBooksEditor(
  private val repo: BookRepository,
  private val serverLibrary: ServerLibrary,
) {

  internal suspend fun set(
    field: BookField,
    ids: Collection<BookId>,
    value: String,
  ): BooksEdited {
    return edit(field, ids.associateWith { value.trim() })
  }

  internal suspend fun undo(edited: BooksEdited): BooksEdited {
    return edit(edited.field, edited.undo)
  }

  private suspend fun edit(
    field: BookField,
    values: Map<BookId, String?>,
  ): BooksEdited {
    val books = repo.all().filter { it.id in values }
    val undo = mutableMapOf<BookId, String?>()
    val failed = mutableSetOf<BookId>()
    var notAllowed = false
    var unreachable = false
    books.forEach { book ->
      val value = values.getValue(book.id)
      if (book.id.isRemote) {
        // each of the other books would only wait for the timeout as well
        val result =
          if (notAllowed || unreachable) {
            ServerEdit.Failed
          } else {
            serverLibrary.updateBook(book.id, serverEdit(book, field, value))
          }
        when (result) {
          // a local edit from before the server edits can cover what the server has
          ServerEdit.Saved -> undo[book.id] = field.of(book.content.metadata)
          ServerEdit.NotAllowed -> {
            failed += book.id
            notAllowed = true
          }
          ServerEdit.Unreachable -> {
            failed += book.id
            unreachable = true
          }
          ServerEdit.Failed -> failed += book.id
        }
      } else {
        // the undo puts back the edit, or lets the book follow its files again
        undo[book.id] = field.of(book.custom)
        repo.updateCustomMetadata(book.id) { field.set(it, value) }
      }
    }
    return BooksEdited(
      field = field,
      count = undo.size,
      failed = failed,
      notAllowed = notAllowed,
      undo = undo,
    )
  }
}

// the server has no "follow the files", so an empty name takes the series or author off
private fun serverEdit(
  book: Book,
  field: BookField,
  value: String?,
): ServerBookEdit =
  when (field) {
    // the part goes along, or the server would lose it
    BookField.Series -> ServerBookEdit(series = value.orEmpty(), part = book.content.metadata.part)
    BookField.Author -> ServerBookEdit(author = value.orEmpty())
  }

internal fun BookField.of(book: Book): String? =
  when (this) {
    BookField.Series -> book.series
    BookField.Author -> book.author
  }

private fun BookField.of(metadata: BookMetadata): String? =
  when (this) {
    BookField.Series -> metadata.series
    BookField.Author -> metadata.author
  }

private fun BookField.of(custom: CustomMetadata): String? =
  when (this) {
    BookField.Series -> custom.series
    BookField.Author -> custom.author
  }

private fun BookField.set(
  custom: CustomMetadata,
  value: String?,
): CustomMetadata =
  when (this) {
    BookField.Series -> custom.copy(series = value)
    BookField.Author -> custom.copy(author = value)
  }
