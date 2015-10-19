import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.TestCase;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.ph1b.audiobook.model.NaturalOrderComparator;

/**
 * A simple test for the file comparator that sorts in a 'natural' way.
 *
 * @author Paul Woitaschek
 */
public class NaturalOrderComparatorTest extends TestCase {

    private List<File> list;

    @SmallTest
    public void testNaturalCompare() {
        String first = "00 I";
        String second = "01 I";
        assertTrue(NaturalOrderComparator.STRING_COMPARATOR.compare(first, second) < 0);
    }

    /**
     * Tests if the comparison results in the desired order.
     */
    @SmallTest
    public void testFileComparison() {
        File f1 = new File("a.jpg");
        File f2 = new File("b.jpg");
        File f3 = new File("/folder/a.jpg");
        File f4 = new File("/folder/b.jpg");
        File f5 = new File("/xfolder/d.jpg");
        File f6 = new File("/xfolder");
        File f7 = new File("/folder");
        File f8 = new File("Ab.jpg");
        File f9 = new File("aC.jpg");
        File f10 = new File("00 Introduction.mp3");
        File f11 = new File("01 How to build a universe.mp3");
        File f12 = new File("9.mp3");
        File f13 = new File("10.mp3");
        list = Arrays.asList(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13);
        Collections.sort(list, NaturalOrderComparator.FILE_COMPARATOR);
        System.out.println(list);

        assertTrue(indexOf(f1) < indexOf(f2));
        assertTrue(indexOf(f3) < indexOf(f1));
        assertTrue(indexOf(f5) > indexOf(f3));
        assertTrue(indexOf(f7) < indexOf(f5));
        assertTrue(indexOf(f8) < indexOf(f9));
        assertTrue(indexOf(f9) > indexOf(f1));
        assertTrue(indexOf(f9) < indexOf(f2));
        assertTrue(indexOf(f11) > indexOf(f10));
        assertTrue(indexOf(f12) < indexOf(f13));
    }

    private int indexOf(File file) {
        return list.indexOf(file);
    }
}
