package de.ph1b.audiobook.common.comparator

import android.net.Uri
import java.io.File
import java.util.ArrayList
import java.util.Comparator


object NaturalOrderComparator {

  val stringComparator: Comparator<String> = IntelliJStringComparator()

  val uriComparator = object : Comparator<Uri> {
    override fun compare(lhs: Uri, rhs: Uri): Int {
      val lhsSegments = lhs.pathSegments.flatMap { it.split("/") }
      val rhsSegments = rhs.pathSegments.flatMap { it.split("/") }

      val leftSize = lhsSegments.size
      val rightSize = rhsSegments.size

      // compare parents only and return if one differs
      var i = 0
      val toLeft = leftSize - 1
      val toRight = rightSize - 1
      while (i < toLeft && i < toRight) {
        val pl = lhsSegments[i]
        val pr = rhsSegments[i]
        if (pl != pr) {
          return stringComparator.compare(pl, pr)
        }
        i++
      }

      // if sizes are the same
      return if (leftSize == rightSize) {
        stringComparator.compare(lhsSegments.lastOrNull() ?: "", rhsSegments.lastOrNull() ?: "")
      } else {
        rightSize - leftSize
      }
    }
  }
  val fileComparator = Comparator<File> { lhs, rhs ->
    if (lhs == rhs) return@Comparator 0

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
      stringComparator.compare(lhs.name, rhs.name)
    } else {
      rightSize - leftSize
    }
  }

  private fun getFileWithParents(target: File): List<File> {
    val all = ArrayList<File>(10)
    var current: File? = target
    do {
      all.add(current!!)
      current = current.parentFile
    } while (current != null)
    return all.reversed()
  }
}
