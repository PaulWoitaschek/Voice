package de.ph1b.audiobook.common.comparator

import java.util.Comparator

/**
 * Simple copy of IntelliJs-Community naturalCompare.
 * Licensed as Apache v2.
 */
class IntelliJStringComparator : Comparator<String> {

  private fun isDecimalDigit(c: Char) = c in '0'..'9'

  override fun compare(lhs: String?, rhs: String?) = naturalCompare(lhs, rhs, false)

  private fun naturalCompare(lhs: String?, rhs: String?, caseSensitive: Boolean): Int {
    if (lhs === rhs) {
      return 0
    }
    if (lhs == null) {
      return -1
    }
    if (rhs == null) {
      return 1
    }

    val string1Length = lhs.length
    val string2Length = rhs.length
    var i = 0
    var j = 0
    while (i < string1Length && j < string2Length) {
      var ch1 = lhs[i]
      var ch2 = rhs[j]
      if ((isDecimalDigit(ch1) || ch1 == ' ') && (isDecimalDigit(ch2) || ch2 == ' ')) {
        var startNum1 = i
        while (ch1 == ' ' || ch1 == '0') {
          // skip leading spaces and zeros
          startNum1++
          if (startNum1 >= string1Length) break
          ch1 = lhs[startNum1]
        }
        var startNum2 = j
        while (ch2 == ' ' || ch2 == '0') {
          // skip leading spaces and zeros
          startNum2++
          if (startNum2 >= string2Length) break
          ch2 = rhs[startNum2]
        }
        i = startNum1
        j = startNum2
        // find end index of number
        while (i < string1Length && isDecimalDigit(lhs[i])) i++
        while (j < string2Length && isDecimalDigit(rhs[j])) j++
        val lengthDiff = i - startNum1 - (j - startNum2)
        if (lengthDiff != 0) {
          // numbers with more digits are always greater than shorter numbers
          return lengthDiff
        }
        while (startNum1 < i) {
          // compare numbers with equal digit count
          val diff = lhs[startNum1] - rhs[startNum2]
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
            val diff1 = ch1.toUpperCase() - ch2.toUpperCase()
            if (diff1 != 0) {
              val diff2 = ch1.toLowerCase() - ch2.toLowerCase()
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
      return naturalCompare(lhs, rhs, true)
    }
    return string1Length - string2Length
  }
}
