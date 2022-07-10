package voice.app.misc.groupie

import android.view.View
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item

typealias BindLayout<B> = (View) -> B

class BindingItem<B : ViewBinding, D>(
  private val data: D,
  @LayoutRes private val layoutId: Int,
  private val bindLayout: BindLayout<B>,
  private val bind: B.(D, Int) -> Unit
) : Item<ViewBindingGroupieViewHolder<B>>() {

  override fun getLayout(): Int = layoutId

  override fun bind(viewHolder: ViewBindingGroupieViewHolder<B>, position: Int) {
    viewHolder.binding.bind(data, position)
  }

  override fun createViewHolder(itemView: View): ViewBindingGroupieViewHolder<B> {
    val binding = bindLayout(itemView)
    return ViewBindingGroupieViewHolder(binding)
  }
}

class ViewBindingGroupieViewHolder<T : ViewBinding>(val binding: T) : GroupieViewHolder(binding.root)
