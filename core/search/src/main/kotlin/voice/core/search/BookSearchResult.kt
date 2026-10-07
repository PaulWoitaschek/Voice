package voice.core.search

import voice.core.data.Book
import voice.core.data.BookContent

data class BookSearchResult(
  val book: Book,
  /**
   * The fields that matched the query. The ranges point into the field's [BookSearchField.text].
   */
  val matches: Map<BookSearchField, List<IntRange>>,
  /**
   * The fields that match every word of the query on their own, e.g. the author for "stephen king".
   */
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

  /**
   * The text of this field the search looks at. A series includes its part, for example "Harry Potter 1".
   */
  fun text(content: BookContent): String? {
    val text = when (this) {
      Title -> content.name
      Author -> content.author
      Series -> content.series?.let { series ->
        listOfNotNull(series, content.part?.takeIf { it.isNotBlank() }).joinToString(" ")
      }
      Narrator -> content.narrator
      Genre -> content.genre
    }
    return text?.takeIf { it.isNotBlank() }
  }
}
