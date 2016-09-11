package de.ph1b.audiobook.misc


import java.io.File
import java.util.*


/**
 * Simple class holding various static comparators.

 * @author Paul Woitaschek
 */
object NaturalOrderComparator {

    val stringComparator: Comparator<String> = IntelliJStringComparator()
    val fileComparator = Comparator<File> { lhs, rhs ->

        if (lhs.isDirectory && !rhs.isDirectory) {
            return@Comparator -1
        } else if (!lhs.isDirectory && rhs.isDirectory) {
            return@Comparator 1
        }

        val left = getFileWithParents(lhs)
        val right = getFileWithParents(rhs)

        val leftSize = left.size
        val rightSize = right.size

        // compare parents only and return if one differs
        var i = 0
        val toLeft = leftSize - 1
        val toRight = rightSize - 1
        while (i < toLeft && i < toRight) {
            val pl = left[i].name
            val pr = right[i].name
            if (pl != pr) {
                return@Comparator stringComparator.compare(pl, pr)
            }
            i++
        }

        // if sizes are the same
        if (leftSize == rightSize) {
            return@Comparator stringComparator.compare(lhs.name, rhs.name)
        } else {
            return@Comparator rightSize - leftSize
        }
    }

    private fun getFileWithParents(target: File): List<File> {
        val all = ArrayList<File>(10)
        var current: File ? = target
        do {
            all.add(current!!)
            current = current.parentFile
        } while (current != null)
        return all.reversed()
    }
}
