package de.ph1b.audiobook.misc.recyclerComponent

import android.support.v7.util.DiffUtil

class EqualityItemCallback<T : Any> private constructor() : DiffUtil.ItemCallback<T>() {

  override fun areItemsTheSame(oldItem: T, newItem: T): Boolean = oldItem == newItem
  override fun areContentsTheSame(oldItem: T, newItem: T): Boolean = oldItem == newItem

  companion object {

    private val instance = EqualityItemCallback<Any>()

    @Suppress("UNCHECKED_CAST") // safe because erasure
    operator fun <T : Any> invoke(): EqualityItemCallback<T> = instance as EqualityItemCallback<T>
  }
}
