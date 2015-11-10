package de.ph1b.audiobook.model;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.ph1b.audiobook.utils.AlphaComparator;


/**
 * Simple class holding various static comparators.
 *
 * @author Paul Woitaschek
 */
public class NaturalOrderComparator {

    public static final Comparator<String> STRING_COMPARATOR = new AlphaComparator();
    public static final Comparator<File> FILE_COMPARATOR = (lhs, rhs) -> {
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
                    return STRING_COMPARATOR.compare(left.getAbsolutePath(), right.getAbsolutePath());
                }
            }

            if (lhsParents.size() == rhsParents.size()) {
                // if the amount of folders matches the files are within the same directory.
                int lastIndex = lhsParents.size() - 1;
                return STRING_COMPARATOR.compare(lhsParents.get(lastIndex).getName(), rhsParents.get(lastIndex).getName());
            } else if (lhsParents.size() < rhsParents.size()) {
                // if the first element has more parents, return a 1 so elements with a lower
                // hierarchy will be first
                return 1;
            } else {
                // else return -1 so the elemnts with the larger hierarchy will be later
                return -1;
            }
        }
    };
}
