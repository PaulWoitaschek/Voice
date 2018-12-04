package de.ph1b.audiobook.misc.recyclerComponent

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * An adapter that is designed for composition instead of inheritance.
 *
 * Instead of handling the view type and items manually, add a [AdapterComponent] for each view type.
 */
open class CompositeListAdapter<T : Any>(
  itemCallback: DiffUtil.ItemCallback<T> = EqualityItemCallback()
) :
  ListAdapter<T, RecyclerView.ViewHolder>(itemCallback) {

  private val helper = CompositeAdapterHelper { getItem(it) }

  final override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    helper.onBindViewHolder(holder, position).also { onViewHolderBound(holder) }
  }

  final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    return helper.onCreateViewHolder(parent, viewType)
  }

  open fun onViewHolderBound(holder: RecyclerView.ViewHolder) {}

  final override fun getItemViewType(position: Int): Int = helper.getItemViewType(position)

  fun <VH : RecyclerView.ViewHolder> addComponents(component: AdapterComponent<T, VH>) {
    helper.addComponents(component)
  }

  fun addComponents(vararg component: AdapterComponent<*, *>) {
    helper.addComponents(*component)
  }
}
