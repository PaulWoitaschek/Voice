package de.paul_woitaschek.audiobook.model;

import java.util.ArrayList;
import java.util.Comparator;

public class BookmarkComparator implements Comparator<Bookmark> {

    private final ArrayList<Chapter> chapters;

    public BookmarkComparator(ArrayList<Chapter> chapters) {
        this.chapters = chapters;
    }

    @Override
    public int compare(Bookmark lhs, Bookmark rhs) {

        int indexLhs = -1;
        int indexRhs = -1;

        for (int i = 0; i < chapters.size(); i++) {
            String chapterPath = chapters.get(i).getPath();
            if (lhs.getPath().equals(chapterPath)) {
                indexLhs = i;
            }
            if (rhs.getPath().equals(chapterPath)) {
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
        return lhs.getTitle().compareTo(rhs.getTitle());
    }
}
