package de.ph1b.audiobook.model;

import java.io.File;
import java.util.Comparator;


public class NaturalOrderComparator implements Comparator<Object> {

    static int naturalCompare(String lhs, String rhs) {
        int ia = 0, ib = 0;
        int nza, nzb;
        char ca, cb;
        int result;
        while (true) {
            // only count the number of zeroes leading the last number
            // compared
            nza = nzb = 0;
            ca = charAt(lhs, ia);
            cb = charAt(rhs, ib);
            // skip over leading spaces or zeros
            while (Character.isSpaceChar(ca) || ca == '0') {
                if (ca == '0') {
                    nza++;
                } else {
                    // only count consecutive zeroes
                    nza = 0;
                }
                ca = charAt(lhs, ++ia);
            }
            while (Character.isSpaceChar(cb) || cb == '0') {
                if (cb == '0') {
                    nzb++;
                } else {
                    // only count consecutive zeroes
                    nzb = 0;
                }
                cb = charAt(rhs, ++ib);
            }
            // process run of digits
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                if ((result = compareRight(lhs.substring(ia),
                        rhs.substring(ib))) != 0) {
                    return result;
                }
            }
            if (ca == 0 && cb == 0) {
                // The strings compare the same. Perhaps the caller
                // will want to call str-cmp to break the tie.
                return nza - nzb;
            }
            if (ca < cb) {
                return -1;
            } else if (ca > cb) {
                return +1;
            }
            ++ia;
            ++ib;
        }
    }

    private static char charAt(String s, int i) {
        if (i >= s.length()) {
            return 0;
        } else {
            return s.toLowerCase().charAt(i); // modified to lower case to
            // ignore case // ignore case
        }
    }

    private static int compareRight(String a, String b) {
        int bias = 0;
        int ia = 0;
        int ib = 0;
        // The longest run of digits wins. That aside, the greatest
        // value wins, but we can't know that it will until we've scanned
        // both numbers to know that they have the same magnitude, so we
        // remember it in BIAS.
        for (; ; ia++, ib++) {
            char ca = charAt(a, ia);
            char cb = charAt(b, ib);
            if (!Character.isDigit(ca) && !Character.isDigit(cb)) {
                return bias;
            } else if (!Character.isDigit(ca)) {
                return -1;
            } else if (!Character.isDigit(cb)) {
                return +1;
            } else if (ca < cb) {
                if (bias == 0) {
                    bias = -1;
                }
            } else if (ca > cb) {
                if (bias == 0) {
                    bias = +1;
                }
            } else if (ca == 0 && cb == 0) {
                return bias;
            }
        }
    }

    private static int naturalCompare(File lhs, File rhs) {
        if (lhs.isDirectory() && !rhs.isDirectory()) {
            // Directory before non-directory
            return -1;
        } else if (!lhs.isDirectory() && rhs.isDirectory()) {
            // Non-directory after directory
            return 1;
        } else {
            // Alphabetic order otherwise, ignoring Capital
            String a = lhs.getName();
            String b = rhs.getName();
            return naturalCompare(a, b);
        }
    }

    @Override
    public int compare(Object lhs, Object rhs) {
        if (lhs instanceof Chapter && rhs instanceof Chapter) {
            Chapter a = (Chapter) lhs;
            Chapter b = (Chapter) rhs;
            return compare(new File(a.getPath()), b.getPath());
        } else if (lhs instanceof File && rhs instanceof File) {
            File a = (File) lhs;
            File b = (File) rhs;
            return naturalCompare(a, b);
        } else if (lhs instanceof String && rhs instanceof String) {
            String a = (String) lhs;
            String b = (String) rhs;
            return naturalCompare(a, b);
        } else {
            return naturalCompare(String.valueOf(lhs), String.valueOf(rhs));
        }
    }
}
