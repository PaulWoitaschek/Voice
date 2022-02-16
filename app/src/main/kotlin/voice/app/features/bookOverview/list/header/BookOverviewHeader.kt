package voice.app.features.bookOverview.list.header

import android.view.ViewGroup
import voice.app.databinding.BookOverviewHeaderBinding
import voice.app.features.bookOverview.list.BookOverviewHeaderModel
import voice.app.misc.recyclerComponent.AdapterComponent
import voice.app.uitools.ViewBindingHolder

class BookOverviewHeaderHolder(parent: ViewGroup) :
  ViewBindingHolder<BookOverviewHeaderBinding>(parent, BookOverviewHeaderBinding::inflate) {


  fun bind(model: BookOverviewHeaderModel) {
    binding.text.setText(model.category.nameRes)
  }
}

class BookOverviewHeaderComponent :
  AdapterComponent<BookOverviewHeaderModel, BookOverviewHeaderHolder>(BookOverviewHeaderModel::class) {

  override fun onCreateViewHolder(parent: ViewGroup) =
    BookOverviewHeaderHolder(parent)

  override fun onBindViewHolder(model: BookOverviewHeaderModel, holder: BookOverviewHeaderHolder) {
    holder.bind(model)
  }
}
