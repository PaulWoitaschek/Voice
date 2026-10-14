package voice.core.search

import voice.core.data.Book
import voice.core.data.BookContent

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

  fun text(content: BookContent): String? {
    val text = when (this) {
      Title -> content.name
      Author -> content.metadata.author
      Series -> content.metadata.series?.let { series ->
        listOfNotNull(series, content.metadata.part?.takeIf { it.isNotBlank() }).joinToString(" ")
      }
      Narrator -> content.metadata.narrator
      Genre -> content.metadata.genre
    }
    return text?.takeIf { it.isNotBlank() }
  }
}
