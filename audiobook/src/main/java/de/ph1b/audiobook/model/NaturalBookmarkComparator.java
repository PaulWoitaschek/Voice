package de.ph1b.audiobook.model;

import java.io.File;
import java.util.Comparator;
import java.util.List;

public class NaturalBookmarkComparator implements Comparator<Bookmark> {

    private final List<Chapter> chapters;

    public NaturalBookmarkComparator(List<Chapter> chapters) {
        this.chapters = chapters;
    }

    @Override
    public int compare(Bookmark lhs, Bookmark rhs) {

        int indexLhs = -1;
        int indexRhs = -1;

        for (int i = 0; i < chapters.size(); i++) {
            File chapterFile = chapters.get(i).getFile();
            if (lhs.getMediaFile().equals(chapterFile)) {
                indexLhs = i;
            }
            if (rhs.getMediaFile().equals(chapterFile)) {
                indexRhs = i;
            }
        }

        // throw exception if bookmark does not belong to book
        if (indexLhs == -1) {
            throw new IllegalArgumentException("bookmark=" + lhs + " could not be found in:" + chapters);
        }
        if (indexRhs == -1) {
            throw new IllegalArgumentException("bookmark=" + rhs + " could not be found in:" + chapters);
        }

        // if position in chapter is earlier or later, return.
        if (indexLhs > indexRhs) {
            return 1;
        } else if (indexLhs < indexRhs) {
            return -1;
        }

        // if time is earlier or later return
        if (lhs.getTime() > rhs.getTime()) {
            return 1;
        } else if (lhs.getTime() < rhs.getTime()) {
            return -1;
        }

        // if there is nothing else to compare, compare the titles.
        return NaturalOrderComparator.INSTANCE.compare(lhs.getTitle(), rhs.getTitle());
    }
}
