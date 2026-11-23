package voice.core.search

import voice.core.data.Book

data class BookSearchResult(
  val book: Book,
  // the ranges point into the field's text, which for a series includes its part
  val matches: Map<BookSearchField, List<IntRange>>,
  // how well the author and the series match, for each whose name has all words of the search
  val nameScores: Map<BookSearchField, Int>,
  val score: Int,
  // the chapter that explains why the search found the book, null when the other fields do
  val chapter: String? = null,
) {

  fun text(field: BookSearchField): String? =
    if (field == BookSearchField.Chapters) chapter else field.text(book)
}

enum class BookSearchField {
  Title,
  Author,
  Series,
  Narrator,
  Genre,
  Chapters;

  // a book has many chapters, which BookSearch gets from its chapter list
  internal fun text(book: Book): String? {
    val text =
      when (this) {
        Title -> book.name
        Author -> book.author
        Series ->
          book.series?.let { series ->
            listOfNotNull(series, book.part?.takeIf { it.isNotBlank() }).joinToString(" ")
          }
        Narrator -> book.content.metadata.narrator
        Genre -> book.content.metadata.genre
        Chapters -> null
      }
    return text?.takeIf { it.isNotBlank() }
  }
}
