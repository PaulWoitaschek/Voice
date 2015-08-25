package de.ph1b.audiobook.model;


import junit.framework.TestCase;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * A simple test for the file comparator that sorts in a 'natural' way.
 *
 * @author Paul Woitaschek
 */
public class NaturalOrderComparatorTest extends TestCase {

    private List<File> list;


    /**
     * Tests if the comparation results in the desired order.
     */
    public void testFileComparation() {
        File f1 = new File("a.jpg");
        File f2 = new File("b.jpg");
        File f3 = new File("/folder/a.jpg");
        File f4 = new File("/folder/b.jpg");
        File f5 = new File("/xfolder/d.jpg");
        File f6 = new File("/xfolder");
        File f7 = new File("/folder");
        File f8 = new File("Ab.jpg");
        File f9 = new File("aC.jpg");
        list = Arrays.asList(f1, f2, f3, f4, f5, f6, f7, f8, f9);
        Collections.sort(list, NaturalOrderComparator.FILE_COMPARATOR);
        System.out.println(list);

        assertTrue(indexOf(f1) < indexOf(f2));
        assertTrue(indexOf(f3) < indexOf(f1));
        assertTrue(indexOf(f5) > indexOf(f3));
        assertTrue(indexOf(f7) < indexOf(f5));
        assertTrue(indexOf(f8) < indexOf(f9));
        assertTrue(indexOf(f9) > indexOf(f1));
        assertTrue(indexOf(f9) < indexOf(f2));
    }

    private int indexOf(File file) {
        return list.indexOf(file);
    }

}