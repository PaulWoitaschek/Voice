import android.test.suitebuilder.annotation.MediumTest;

import com.google.common.collect.Lists;

import junit.framework.TestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ph1b.audiobook.model.NaturalOrderComparator;

/**
 * A simple test for the file comparator that sorts in a 'natural' way.
 *
 * @author Paul Woitaschek
 */
public class NaturalOrderComparatorTest extends TestCase {

    @MediumTest
    public void testFileSpecific() {
        List<File> desiredOrder = Lists.newArrayList(
                new File("/folder/subfolder/subsubfolder/test2.mp3"),
                new File("/folder/subfolder/test.mp3"),
                new File("/folder/subfolder/test2.mp3"),
                new File("/folder/a.jpg"),
                new File("/folder/aC.jpg"),
                new File("/xfolder/d.jpg"),
                new File("/xFolder/d.jpg"),
                new File("/a.jpg")
        );

        List<File> sorted = new ArrayList<>(desiredOrder);
        Collections.sort(sorted, NaturalOrderComparator.INSTANCE.getFILE_COMPARATOR());
        assertEquals(desiredOrder, sorted);
    }

    @MediumTest
    public void testSimpleComparison() {
        List<String> desiredOrder = Lists.newArrayList(
                "00 I",
                "00 Introduction",
                "1",
                "01 How to build a universe",
                "01 I",
                "2",
                "9",
                "10",
                "a",
                "Ab",
                "aC",
                "Ba",
                "cA",
                "D",
                "e"
        );

        List<String> sorted = new ArrayList<>(desiredOrder);
        Collections.sort(sorted, NaturalOrderComparator.INSTANCE.getSTRING_COMPARATOR());
        assertEquals(desiredOrder, sorted);
    }
}
