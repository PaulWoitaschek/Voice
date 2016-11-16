package de.ph1b.audiobook.misc

import java.util.*

/**
 * Simple copy of IntelliJs-Community naturalCompare (in StringUtil).
 * Licensed as Apache v2.
 *
 * @author Paul Woitaschek
 */
class IntelliJStringComparator : Comparator<String> {

  private fun isDecimalDigit(c: Char): Boolean {
    return c >= '0' && c <= '9'
  }

  private fun toUpperCase(a: Char): Char {
    if (a < 'a') {
      return a
    }
    if (a <= 'z') {
      return (a.toInt() + ('A' - 'a')).toChar()
    }
    return Character.toUpperCase(a)
  }

  private fun naturalCompare(string1: String?, string2: String?, caseSensitive: Boolean): Int {

    if (string1 === string2) {
      return 0
    }
    if (string1 == null) {
      return -1
    }
    if (string2 == null) {
      return 1
    }

    val string1Length = string1.length
    val string2Length = string2.length
    var i = 0
    var j = 0
    while (i < string1Length && j < string2Length) {
      var ch1 = string1[i]
      var ch2 = string2[j]
      if ((isDecimalDigit(ch1) || ch1 == ' ') && (isDecimalDigit(ch2) || ch2 == ' ')) {
        var startNum1 = i
        while (ch1 == ' ' || ch1 == '0') { // skip leading spaces and zeros
          startNum1++
          if (startNum1 >= string1Length) break
          ch1 = string1[startNum1]
        }
        var startNum2 = j
        while (ch2 == ' ' || ch2 == '0') { // skip leading spaces and zeros
          startNum2++
          if (startNum2 >= string2Length) break
          ch2 = string2[startNum2]
        }
        i = startNum1
        j = startNum2
        // find end index of number
        while (i < string1Length && isDecimalDigit(string1[i])) i++
        while (j < string2Length && isDecimalDigit(string2[j])) j++
        val lengthDiff = i - startNum1 - (j - startNum2)
        if (lengthDiff != 0) {
          // numbers with more digits are always greater than shorter numbers
          return lengthDiff
        }
        while (startNum1 < i) {
          // compare numbers with equal digit count
          val diff = string1[startNum1] - string2[startNum2]
          if (diff != 0) {
            return diff
          }
          startNum1++
          startNum2++
        }
        i--
        j--
      } else {
        if (caseSensitive) {
          return ch1 - ch2
        } else {
          // similar logic to charsMatch() below
          if (ch1 != ch2) {
            val diff1 = toUpperCase(ch1) - toUpperCase(ch2)
            if (diff1 != 0) {
              val diff2 = toLowerCase(ch1) - toLowerCase(ch2)
              if (diff2 != 0) {
                return diff2
              }
            }
          }
        }
      }
      i++
      j++
    }
    // After the loop the end of one of the strings might not have been reached, if the other
    // string ends with a number and the strings are equal until the end of that number. When
    // there are more characters in the string, then it is greater.
    if (i < string1Length) {
      return 1
    }
    if (j < string2Length) {
      return -1
    }
    if (!caseSensitive && string1Length == string2Length) {
      // do case sensitive compare if case insensitive strings are equal
      return naturalCompare(string1, string2, true)
    }
    return string1Length - string2Length
  }

  private fun toLowerCase(a: Char): Char {
    if (a < 'A' || a >= 'a' && a <= 'z') {
      return a
    }
    if (a <= 'Z') {
      return (a.toInt() + ('a' - 'A')).toChar()
    }

    return Character.toLowerCase(a)
  }

  override fun compare(lhs: String, rhs: String): Int {
    return naturalCompare(lhs, rhs, false)
  }
}
