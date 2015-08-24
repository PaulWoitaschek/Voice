package de.ph1b.audiobook.model;


import android.support.annotation.VisibleForTesting;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class NaturalOrderComparator {

    public static final Comparator<File> FILE_COMPARATOR = new Comparator<File>() {
        @Override
        public int compare(File lhs, File rhs) {
            return naturalCompare(lhs, rhs);
        }
    };

    public static int naturalCompare(String lhs, String rhs) {
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
        String lowerString = s.toLowerCase();
        if (i >= lowerString.length()) {
            return 0;
        } else {
            return lowerString.charAt(i); // modified to lower case to ignore case
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

    @VisibleForTesting
    private static int naturalCompare(File lhs, File rhs) {
        if (lhs.isDirectory() && !rhs.isDirectory()) {
            // Directory before non-directory
            return -1;
        } else if (!lhs.isDirectory() && rhs.isDirectory()) {
            // Non-directory after directory
            return 1;
        } else {

            // make a list containing the file and all its parent directories
            List<File> lhsParents = new ArrayList<>(10);
            File lhsFile = lhs;
            do {
                lhsParents.add(lhsFile);
            } while ((lhsFile = lhsFile.getParentFile()) != null);
            List<File> rhsParents = new ArrayList<>(10);
            File rhsFile = rhs;
            do {
                rhsParents.add(rhsFile);
            }
            while ((rhsFile = rhsFile.getParentFile()) != null);

            // reverse the list so it starts with the topmost parent
            Collections.reverse(lhsParents);
            Collections.reverse(rhsParents);

            // iterates beginning from the topmost parent folder and returns the calculated sorting
            // if they differ
            for (int i = 0; i < lhsParents.size() && i < rhsParents.size(); i++) {
                File left = lhsParents.get(i);
                File right = rhsParents.get(i);
                if (!left.equals(right)) {
                    return naturalCompare(left.getAbsolutePath(), right.getAbsolutePath());
                }
            }

            if (lhsParents.size() == rhsParents.size()) {
                // if the amount of folders matches the files are within the same directory.
                int lastIndex = lhsParents.size() - 1;
                return naturalCompare(lhsParents.get(lastIndex).getName(), rhsParents.get(lastIndex).getName());
            } else if (lhsParents.size() < rhsParents.size()) {
                // if the first element has more parents, return a 1 so elements with a lower
                // hierarchy will be first
                return 1;
            } else {
                // else return -1 so the elemnts with the larger hierarchy will be later
                return -1;
            }
        }
    }
}
