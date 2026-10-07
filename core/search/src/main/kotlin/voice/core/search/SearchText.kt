package voice.core.search

import java.text.Normalizer
import java.util.Locale

/**
 * A text split into lowercase words the way the full text search sees them: separated by anything that isn't a
 * letter, a digit or a mark. Each word remembers where it came from in the original text.
 *
 * With [removeDiacritics], "Café" and "cafe" become the same word.
 */
internal class SearchText(
  original: String,
  removeDiacritics: Boolean = true,
) {

  class Word(
    val text: String,
    private val originalStarts: IntArray,
    private val originalEnds: IntArray,
  ) {

    /**
     * The range in the original text that the first [length] characters of this word came from.
     */
    fun originalRange(length: Int): IntRange = originalStarts.first()..originalEnds[length - 1]
  }

  val words: List<Word>

  init {
    val words = mutableListOf<Word>()
    val text = StringBuilder()
    val starts = mutableListOf<Int>()
    val ends = mutableListOf<Int>()
    fun endWord() {
      if (text.isNotEmpty()) {
        words += Word(text.toString(), starts.toIntArray(), ends.toIntArray())
        text.clear()
        starts.clear()
        ends.clear()
      }
    }

    var index = 0
    while (index < original.length) {
      val codePoint = original.codePointAt(index)
      val charCount = Character.charCount(codePoint)
      val lowercase = String(Character.toChars(codePoint)).lowercase(Locale.ROOT)
      val normalized = if (removeDiacritics) {
        Normalizer.normalize(lowercase, Normalizer.Form.NFD).filterNot { it in COMBINING_DIACRITICS }
      } else {
        lowercase
      }
      normalized.codePoints().forEach { normalizedCodePoint ->
        if (normalizedCodePoint.isWordCharacter()) {
          repeat(Character.charCount(normalizedCodePoint)) {
            starts += index
            ends += index + charCount - 1
          }
          text.appendCodePoint(normalizedCodePoint)
        } else {
          endWord()
        }
      }
      index += charCount
    }
    endWord()
    this.words = words
  }

  private companion object {
    val COMBINING_DIACRITICS = '̀'..'ͯ'

    val WORD_CHARACTER_TYPES = setOf(
      Character.UPPERCASE_LETTER,
      Character.LOWERCASE_LETTER,
      Character.TITLECASE_LETTER,
      Character.MODIFIER_LETTER,
      Character.OTHER_LETTER,
      Character.DECIMAL_DIGIT_NUMBER,
      Character.LETTER_NUMBER,
      Character.OTHER_NUMBER,
      Character.NON_SPACING_MARK,
      Character.COMBINING_SPACING_MARK,
      Character.ENCLOSING_MARK,
    ).map { it.toInt() }.toSet()

    fun Int.isWordCharacter(): Boolean = Character.getType(this) in WORD_CHARACTER_TYPES
  }
}
