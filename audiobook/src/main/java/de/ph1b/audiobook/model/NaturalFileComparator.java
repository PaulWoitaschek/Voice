package de.ph1b.audiobook.model;

import java.io.File;
import java.util.Comparator;


public class NaturalFileComparator implements Comparator<File> {

    @Override
    public int compare(File lhs, File rhs) {
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
            return NaturalComparator.compare(a, b);
        }
    }
}
