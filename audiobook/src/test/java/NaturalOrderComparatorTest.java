

/*
 * This file is part of Material Audiobook Player.
 *
 * Material Audiobook Player is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Material Audiobook Player is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Material Audiobook Player. If not, see <http://www.gnu.org/licenses/>.
 * /licenses/>.
 */

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ph1b.audiobook.model.NaturalOrderComparator;
import kotlin.collections.CollectionsKt;

/**
 * A simple test for the file comparator that sorts in a 'natural' way.
 *
 * @author Paul Woitaschek
 */
public class NaturalOrderComparatorTest {

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testWhatEver() throws IOException {
        testFolder.newFolder("folder", "subfolder", "subsubfolder");
        testFolder.newFolder("storage", "emulated", "0");
        testFolder.newFolder("xFolder");

        File alarmsFolder = testFolder.newFolder("storage", "emulated", "0", "Alarms");
        List<File> desiredOrder = CollectionsKt.listOf(
                alarmsFolder,
                testFolder.newFile("folder/subfolder/subsubfolder/test2.mp3"),
                testFolder.newFile("folder/subfolder/test.mp3"),
                testFolder.newFile("folder/subfolder/test2.mp3"),
                testFolder.newFile("folder/a.jpg"),
                testFolder.newFile("folder/aC.jpg"),
                testFolder.newFile("storage/emulated/0/1.ogg"),
                testFolder.newFile("storage/emulated/0/2.ogg"),
                testFolder.newFile("xFolder/d.jpg"),
                testFolder.newFile("1.mp3"),
                testFolder.newFile("a.jpg"));

        List<File> sorted = new ArrayList<>(desiredOrder);
        Collections.sort(sorted, NaturalOrderComparator.INSTANCE.getFILE_COMPARATOR());
        Assert.assertEquals(desiredOrder, sorted);
    }

    @Test
    public void testSimpleComparison() {
        List<String> desiredOrder = CollectionsKt.listOf(
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
                "e");

        List<String> sorted = new ArrayList<>(desiredOrder);
        Collections.sort(sorted, NaturalOrderComparator.INSTANCE.getSTRING_COMPARATOR());
        Assert.assertEquals(desiredOrder, sorted);
    }
}
