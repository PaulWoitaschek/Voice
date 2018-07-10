package de.ph1b.audiobook.misc.recyclerComponent

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup

class CompositeAdapterHelper(
  private val getItem: (position: Int) -> Any
) {

  private val components = ArrayList<AdapterComponent<Any, RecyclerView.ViewHolder>>()

  fun getItemViewType(position: Int): Int {
    val item = getItem(position)
    components.forEach { component ->
      if (component.isForViewType(item)) {
        return component.viewType
      }
    }
    throw IllegalStateException("No component for item $item at position=$position")
  }

  fun <T : Any, VH : RecyclerView.ViewHolder> addComponents(component: AdapterComponent<T, VH>) {
    @Suppress("UNCHECKED_CAST")
    components.add(component as AdapterComponent<Any, RecyclerView.ViewHolder>)
  }

  fun addComponents(vararg component: AdapterComponent<*, *>) {
    component.forEach {
      addComponents(it)
    }
  }

  fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    val viewType = holder.itemViewType
    val component = componentForViewType(viewType)
        ?: throw NullPointerException("No component for viewType $viewType")
    val item = getItem(position)
    component.onBindViewHolder(item, holder)
  }

  fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val component = componentForViewType(viewType)
        ?: throw NullPointerException("No component for viewType $viewType")
    return component.onCreateViewHolder(parent)
  }

  private fun componentForViewType(viewType: Int): AdapterComponent<Any, RecyclerView.ViewHolder>? {
    return components.find { it.viewType == viewType }
  }
}
