package voice.common.comparator

import android.net.Uri

object NaturalOrderComparator {

  val stringComparator: Comparator<String> = IntelliJStringComparator

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
}

fun Set<String>.sortedNaturally(): List<String> = sortedWith(NaturalOrderComparator.stringComparator)
