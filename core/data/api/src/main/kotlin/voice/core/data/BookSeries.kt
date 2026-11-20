package voice.core.data

import java.text.Normalizer
import voice.core.common.comparator.NaturalOrderComparator

public fun nameKey(name: String): String? {
  return Normalizer.normalize(name, Normalizer.Form.NFKD)
    // accents like the one of ä can go, but in scripts like Devanagari the marks are part of the
    // letters
    .filter { it.isLetterOrDigit() || (it.category in Marks && it !in Accents) }
    .lowercase()
    .takeIf { it.isNotEmpty() }
}

private val Marks =
  setOf(
    CharCategory.NON_SPACING_MARK,
    CharCategory.COMBINING_SPACING_MARK,
    CharCategory.ENCLOSING_MARK,
  )
private val Accents = '\u0300'..'\u036F'

public val Book.seriesName: String?
  get() = series?.trim()?.takeIf { it.isNotEmpty() }

public val Book.seriesPart: String?
  get() = part?.trim()?.takeIf { it.isNotEmpty() }

public val Book.seriesLine: String?
  get() = seriesName?.let { name -> listOfNotNull(name, seriesPart).joinToString(" ") }

// Voice doesn't store file names, so books without a part follow their names, which mostly come
// from their files
public val SeriesOrder: Comparator<Book> =
  compareBy<Book, String?>(nullsLast(NaturalOrderComparator.stringComparator)) { it.seriesPart }
    .then(BookComparator.ByName)
    .thenBy { it.id.value }

public fun List<Book>.nextInSeries(book: Book): Book? {
  val key = book.seriesName?.let(::nameKey) ?: return null
  return filter {
      it.seriesName?.let(::nameKey) == key &&
        SeriesOrder.compare(it, book) > 0 &&
        it.category != BookCategory.FINISHED &&
        !it.custom.hidden
    }
    .minWithOrNull(SeriesOrder)
}
