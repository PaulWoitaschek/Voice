package de.ph1b.audiobook.model


import de.ph1b.audiobook.misc.NaturalOrderComparator
import junit.framework.TestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.*

/**
 * A simple test for the file comparator that sorts in a 'natural' way.

 * @author Paul Woitaschek
 */
class NaturalOrderComparatorTest : TestCase() {

    val testFolder = TemporaryFolder()
    @Rule fun testFolder() = testFolder

    override fun setUp() {
        super.setUp()
        testFolder.create()
    }

    @Test fun testFileComparator() {
        testFolder.newFolder("folder", "subfolder", "subsubfolder")
        testFolder.newFolder("storage", "emulated", "0")
        testFolder.newFolder("xFolder")

        val alarmsFolder = testFolder.newFolder("storage", "emulated", "0", "Alarms")
        val desiredOrder = listOf(
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
                testFolder.newFile("a.jpg"))

        val sorted = ArrayList(desiredOrder)
        Collections.sort(sorted, NaturalOrderComparator.FILE_COMPARATOR)
        assertThat(desiredOrder).isEqualTo(sorted)
    }

    @Test fun testStringComparator() {
        val desiredOrder = listOf(
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
                "e")

        val sorted = ArrayList(desiredOrder)
        Collections.sort(sorted, NaturalOrderComparator.STRING_COMPARATOR)
        assertThat(desiredOrder).isEqualTo(sorted)
    }

    override fun tearDown() {
        testFolder.delete()

        super.tearDown()
    }
}
