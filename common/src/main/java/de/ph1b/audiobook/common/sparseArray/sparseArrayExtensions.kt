package de.ph1b.audiobook.common.sparseArray

import android.support.v4.util.SparseArrayCompat

fun <T> SparseArrayCompat<T>.contentEquals(other: SparseArrayCompat<T>): Boolean {
  val size = this.size()
  if (size != other.size()) return false
  return (0 until size).none {
    keyAt(it) != other.keyAt(it) || valueAt(it) != other.valueAt(it)
  }
}

inline fun <E> SparseArrayCompat<E>.forEachIndexed(
  reversed: Boolean = false,
  action: (index: Int, key: Int, value: E) -> Unit
) {
  val size = size()
  val range = 0 until size
  for (index in if (reversed) range.reversed() else range) {
    val key = keyAt(index)
    val value = valueAt(index)
    action(index, key, value)
  }
}

fun <T> Map<Int, T>.toSparseArray(): SparseArrayCompat<T> {
  if (isEmpty())
    return emptySparseArray()

  val array = SparseArrayCompat<T>(size)
  forEach { (key, value) ->
    array.put(key, value)
  }
  return array
}

fun SparseArrayCompat<*>.keyAtOrNull(index: Int) = if (index >= size()) null else keyAt(index)
