package de.ph1b.audiobook.model;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.ph1b.audiobook.utils.IntelliJStringComparator;


/**
 * Simple class holding various static comparators.
 *
 * @author Paul Woitaschek
 */
public class NaturalOrderComparator {

    public static final Comparator<String> STRING_COMPARATOR = new IntelliJStringComparator();
    public static final Comparator<File> FILE_COMPARATOR = (lhs, rhs) -> {
        if (lhs.isDirectory() || rhs.isDirectory()) {
            return lhs.compareTo(rhs);
        }

        List<File> left = getFileWithParents(lhs);
        List<File> right = getFileWithParents(rhs);

        int leftSize = left.size();
        int rightSize = right.size();

        // compare parents only and return if one differs
        for (int i = 0, toLeft = leftSize - 1, toRight = rightSize - 1; i < toLeft && i < toRight; i++) {
            String pl = left.get(i).getName();
            String pr = right.get(i).getName();
            if (!pl.equals(pr)) {
                return STRING_COMPARATOR.compare(pl, pr);
            }
        }

        // if sizes are the same
        if (leftSize == rightSize) {
            return STRING_COMPARATOR.compare(lhs.getName(), rhs.getName());
        } else {
            return rightSize - leftSize;
        }
    };

    private static List<File> getFileWithParents(File target) {
        List<File> all = new ArrayList<>(10);
        File current = target;
        do {
            all.add(current);
        } while ((current = current.getParentFile()) != null);
        Collections.reverse(all);
        return all;
    }
}
