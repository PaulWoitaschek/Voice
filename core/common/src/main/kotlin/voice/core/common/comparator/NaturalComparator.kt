/*
 * This file contains code copied or adapted from:
 * https://github.com/JetBrains/intellij-community/blob/fc5900ddec25d8b603f315ee92c7538f29b32a8f/platform/util/base/src/com/intellij/openapi/util/text/NaturalComparator.java#L16 *
 * Original License: Apache V2
 *
 * Modifications:
 * Copyright (c) 2026 Paul Woitaschek
 *
 * This file is distributed as part of Voice under the GNU GPL v3.
 */
package voice.core.common.comparator

import java.text.Collator
import java.util.Locale

internal class NaturalComparator : Comparator<String> {

  private val collator: Collator = Collator.getInstance(Locale.getDefault()).apply {
    strength = Collator.PRIMARY
  }

  override fun compare(
    s1: String,
    s2: String,
  ): Int {
    if (s1 == s2) {
      return 0
    }
    return naturalCompare(s1 = s1, s2 = s2, length1 = s1.length, length2 = s2.length, ignoreCase = true, likeFileNames = true)
  }

  private fun isDecimalDigit(c: Char) = c in '0'..'9'

  private fun naturalCompare(
    s1: String,
    s2: String,
    length1: Int,
    length2: Int,
    ignoreCase: Boolean,
    likeFileNames: Boolean,
  ): Int {
    var i = 0
    var j = 0
    while (i < length1 && j < length2) {
      val ch1 = s1[i]
      val ch2 = s2[j]
      if ((isDecimalDigit(ch1) || ch1 == ' ') && (isDecimalDigit(ch2) || ch2 == ' ')) {
        val start1 = skipChar(s1, skipChar(s1, i, length1, ' '), length1, '0')
        val start2 = skipChar(s2, skipChar(s2, j, length2, ' '), length2, '0')

        val end1 = skipDigits(s1, start1, length1)
        val end2 = skipDigits(s2, start2, length2)

        // numbers with more digits are always greater than shorter numbers
        val lengthDiff = (end1 - start1) - (end2 - start2)
        if (lengthDiff != 0) return lengthDiff

        // compare numbers with equal digit count
        val numberDiff = compareCharRange(s1, s2, start1, start2, end1)
        if (numberDiff != 0) return numberDiff

        // compare number length including leading spaces and zeroes
        val fullLengthDiff = (end1 - i) - (end2 - j)
        if (fullLengthDiff != 0) return fullLengthDiff

        // the numbers are the same; compare leading spaces and zeroes
        val leadingDiff = compareCharRange(s1, s2, i, j, start1)
        if (leadingDiff != 0) return leadingDiff

        i = end1 - 1
        j = end2 - 1
      } else if (likeFileNames) {
        // for super natural comparison (IDEA-80435)
        if (ch1 != ch2) {
          val diff: Int = when {
            ch1 == '-' && ch2 != '_' -> {
              compareChars('_', ch2)
            }
            ch2 == '-' && ch1 != '_' -> {
              compareChars(ch1, '_')
            }
            else -> {
              compareChars(ch1, ch2)
            }
          }
          if (diff != 0) return diff
        }
      } else {
        val diff = compareChars(ch1, ch2)
        if (diff != 0) return diff
      }
      i++
      j++
    }
    // After the loop, the end of one of the strings might not have been reached if the other
    // string ends with a number and the strings are equal until the end of that number.
    // When there are more characters in the string, then it is greater.
    if (i < length1) return +1
    if (j < length2) return -1
    if (length1 != length2) return length1 - length2

    // do case-sensitive compare if case-insensitive strings are equal
    return if (ignoreCase) naturalCompare(s1, s2, length1, length2, false, likeFileNames) else 0
  }

  private fun compareCharRange(
    s1: String,
    s2: String,
    offset1: Int,
    offset2: Int,
    end1: Int,
  ): Int {
    var i = offset1
    var j = offset2
    while (i < end1) {
      val diff = s1[i].code - s2[j].code
      if (diff != 0) return diff
      i++
      j++
    }
    return 0
  }

  private fun compareChars(
    lhs: Char,
    rhs: Char,
  ): Int {
    if (lhs.isLetter() && rhs.isLetter()) {
      val collatorDiff = collator.compare(lhs.toString(), rhs.toString())
      if (collatorDiff != 0) {
        return collatorDiff
      }
    }

    val diff1 = lhs.uppercaseChar() - rhs.uppercaseChar()
    if (diff1 != 0) {
      val diff2 = lhs.lowercaseChar() - rhs.lowercaseChar()
      if (diff2 != 0) {
        return diff2
      }
    }
    return 0
  }

  private fun skipDigits(
    s: String,
    start: Int,
    end: Int,
  ): Int {
    var start = start
    while (start < end && isDecimalDigit(s[start])) start++
    return start
  }

  private fun skipChar(
    s: String,
    start: Int,
    end: Int,
    c: Char,
  ): Int {
    var start = start
    while (start < end && s[start] == c) start++
    return start
  }
}
