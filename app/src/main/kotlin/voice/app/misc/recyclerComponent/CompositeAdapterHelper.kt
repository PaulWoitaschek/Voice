package voice.app.misc.recyclerComponent

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class CompositeAdapterHelper<T : Any>(private val getItem: (position: Int) -> T) {

  private val components = ArrayList<AdapterComponent<T, RecyclerView.ViewHolder>>()

  fun getItemViewType(position: Int): Int {
    val item = getItem(position)
    components.forEach { component ->
      if (component.isForViewType(item)) {
        return component.viewType
      }
    }
    throw IllegalStateException("No component for item $item at position=$position")
  }

  fun <VH : RecyclerView.ViewHolder> addComponent(component: AdapterComponent<T, VH>) {
    @Suppress("UNCHECKED_CAST")
    components.add(component as AdapterComponent<T, RecyclerView.ViewHolder>)
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

  private fun componentForViewType(viewType: Int): AdapterComponent<T, RecyclerView.ViewHolder>? {
    return components.find { it.viewType == viewType }
  }
}
