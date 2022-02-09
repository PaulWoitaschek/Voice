package de.ph1b.audiobook.features.bookOverview.list.header

import android.view.ViewGroup
import de.ph1b.audiobook.databinding.BookOverviewHeaderBinding
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewHeaderModel
import de.ph1b.audiobook.misc.recyclerComponent.AdapterComponent
import de.ph1b.audiobook.uitools.ViewBindingHolder

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
