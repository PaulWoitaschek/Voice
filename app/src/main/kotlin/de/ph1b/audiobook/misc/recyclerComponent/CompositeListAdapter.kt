package de.ph1b.audiobook.misc.recyclerComponent

import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * An adapter that is designed for composition instead of inheritance.
 *
 * Instead of handling the view type and items manually, add a [AdapterComponent] for each view type.
 */
open class CompositeListAdapter<T : Any>(
  itemCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, RecyclerView.ViewHolder>(itemCallback) {

  private val helper = CompositeAdapterHelper<T> { getItem(it) }

  @CallSuper
  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    helper.onBindViewHolder(holder, position).also { onViewHolderBound(holder) }
  }

  final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    return helper.onCreateViewHolder(parent, viewType)
  }

  open fun onViewHolderBound(holder: RecyclerView.ViewHolder) {}

  final override fun getItemViewType(position: Int): Int = helper.getItemViewType(position)

  fun <U : T, VH : RecyclerView.ViewHolder> addComponent(component: AdapterComponent<U, VH>) {
    @Suppress("UNCHECKED_CAST")
    helper.addComponent(component as AdapterComponent<T, VH>)
  }
}
