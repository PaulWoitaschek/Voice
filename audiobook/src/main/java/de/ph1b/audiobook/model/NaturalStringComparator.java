package de.ph1b.audiobook.model;

import java.util.Comparator;

class NaturalStringComparator implements Comparator<String> {


    @Override
    public int compare(String lhs, String rhs) {
        return NaturalComparator.compare(lhs, rhs);
    }
}