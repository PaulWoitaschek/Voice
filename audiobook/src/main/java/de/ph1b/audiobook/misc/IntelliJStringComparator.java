package de.ph1b.audiobook.misc;

import android.support.annotation.Nullable;

import java.util.Comparator;

/**
 * Simple copy of IntelliJs-Community naturalCompare (in StringUtil).
 * Licensed as Apache v2.
 *
 * @author Paul Woitaschek
 */
public class IntelliJStringComparator implements Comparator<String> {

  private static boolean isDecimalDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private static char toUpperCase(char a) {
    if (a < 'a') {
      return a;
    }
    if (a <= 'z') {
      return (char) (a + ('A' - 'a'));
    }
    return Character.toUpperCase(a);
  }


  private static int naturalCompare(@Nullable String string1, @Nullable String string2, boolean caseSensitive) {
    //noinspection StringEquality
    if (string1 == string2) {
      return 0;
    }
    if (string1 == null) {
      return -1;
    }
    if (string2 == null) {
      return 1;
    }

    final int string1Length = string1.length();
    final int string2Length = string2.length();
    int i = 0;
    int j = 0;
    for (; i < string1Length && j < string2Length; i++, j++) {
      char ch1 = string1.charAt(i);
      char ch2 = string2.charAt(j);
      if ((isDecimalDigit(ch1) || ch1 == ' ') && (isDecimalDigit(ch2) || ch2 == ' ')) {
        int startNum1 = i;
        while (ch1 == ' ' || ch1 == '0') { // skip leading spaces and zeros
          startNum1++;
          if (startNum1 >= string1Length) break;
          ch1 = string1.charAt(startNum1);
        }
        int startNum2 = j;
        while (ch2 == ' ' || ch2 == '0') { // skip leading spaces and zeros
          startNum2++;
          if (startNum2 >= string2Length) break;
          ch2 = string2.charAt(startNum2);
        }
        i = startNum1;
        j = startNum2;
        // find end index of number
        while (i < string1Length && isDecimalDigit(string1.charAt(i))) i++;
        while (j < string2Length && isDecimalDigit(string2.charAt(j))) j++;
        final int lengthDiff = (i - startNum1) - (j - startNum2);
        if (lengthDiff != 0) {
          // numbers with more digits are always greater than shorter numbers
          return lengthDiff;
        }
        for (; startNum1 < i; startNum1++, startNum2++) {
          // compare numbers with equal digit count
          final int diff = string1.charAt(startNum1) - string2.charAt(startNum2);
          if (diff != 0) {
            return diff;
          }
        }
        i--;
        j--;
      } else {
        if (caseSensitive) {
          return ch1 - ch2;
        } else {
          // similar logic to charsMatch() below
          if (ch1 != ch2) {
            final int diff1 = toUpperCase(ch1) - toUpperCase(ch2);
            if (diff1 != 0) {
              final int diff2 = toLowerCase(ch1) - toLowerCase(ch2);
              if (diff2 != 0) {
                return diff2;
              }
            }
          }
        }
      }
    }
    // After the loop the end of one of the strings might not have been reached, if the other
    // string ends with a number and the strings are equal until the end of that number. When
    // there are more characters in the string, then it is greater.
    if (i < string1Length) {
      return 1;
    }
    if (j < string2Length) {
      return -1;
    }
    if (!caseSensitive && string1Length == string2Length) {
      // do case sensitive compare if case insensitive strings are equal
      return naturalCompare(string1, string2, true);
    }
    return string1Length - string2Length;
  }

  private static char toLowerCase(char a) {
    if (a < 'A' || a >= 'a' && a <= 'z') {
      return a;
    }
    if (a <= 'Z') {
      return (char) (a + ('a' - 'A'));
    }

    return Character.toLowerCase(a);
  }

  @Override
  public int compare(String lhs, String rhs) {
    return naturalCompare(lhs, rhs, false);
  }
}
