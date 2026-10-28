package voice.core.search

import voice.core.data.Book

data class BookSearchResult(
  val book: Book,
  // the ranges point into the field's text, which for a series includes its part
  val matches: Map<BookSearchField, List<IntRange>>,
  val fieldsMatchingAllWords: Set<BookSearchField>,
  val score: Int,
)

enum class BookSearchField {
  Title,
  Author,
  Series,
  Narrator,
  Genre,
  ;

  fun text(book: Book): String? {
    val text = when (this) {
      Title -> book.name
      Author -> book.author
      Series -> book.content.metadata.series?.let { series ->
        listOfNotNull(series, book.content.metadata.part?.takeIf { it.isNotBlank() }).joinToString(" ")
      }
      Narrator -> book.content.metadata.narrator
      Genre -> book.content.metadata.genre
    }
    return text?.takeIf { it.isNotBlank() }
  }
}
