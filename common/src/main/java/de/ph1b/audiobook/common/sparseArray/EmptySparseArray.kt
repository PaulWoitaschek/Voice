package de.ph1b.audiobook.common.sparseArray

import androidx.collection.SparseArrayCompat

/**
 * A immutable sparse array without any values
 */
class EmptySparseArray<E> private constructor() : SparseArrayCompat<E>() {

  override fun remove(key: Int) = throw UnsupportedOperationException()

  override fun removeAtRange(index: Int, size: Int) = throw UnsupportedOperationException()

  override fun clear() = throw UnsupportedOperationException()

  override fun delete(key: Int) = throw UnsupportedOperationException()

  override fun append(key: Int, value: E) = throw UnsupportedOperationException()

  override fun put(key: Int, value: E) = throw UnsupportedOperationException()

  override fun removeAt(index: Int) = throw UnsupportedOperationException()

  override fun setValueAt(index: Int, value: E) = throw UnsupportedOperationException()

  companion object {

    private val INSTANCE = EmptySparseArray<Any>()

    fun <E> instance(): EmptySparseArray<E> {
      @Suppress("UNCHECKED_CAST")
      return INSTANCE as EmptySparseArray<E>
    }
  }
}

fun <E> emptySparseArray(): EmptySparseArray<E> = EmptySparseArray.instance()
