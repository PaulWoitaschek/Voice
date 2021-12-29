package de.ph1b.audiobook.features.bookOverview.list.header

import android.view.ViewGroup
import androidx.core.view.isInvisible
import de.ph1b.audiobook.databinding.BookOverviewHeaderBinding
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewHeaderModel
import de.ph1b.audiobook.misc.recyclerComponent.AdapterComponent
import de.ph1b.audiobook.uitools.ViewBindingHolder

typealias OpenCategoryListener = (BookOverviewCategory) -> Unit

class BookOverviewHeaderHolder(parent: ViewGroup, listener: OpenCategoryListener) :
  ViewBindingHolder<BookOverviewHeaderBinding>(parent, BookOverviewHeaderBinding::inflate) {

  private var boundCategory: BookOverviewCategory? = null

  init {
    binding.showAll.setOnClickListener {
      boundCategory?.let(listener)
    }
  }

  fun bind(model: BookOverviewHeaderModel) {
    boundCategory = model.category
    binding.text.setText(model.category.nameRes)
    binding.showAll.isInvisible = !model.hasMore
  }
}

class BookOverviewHeaderComponent(private val listener: OpenCategoryListener) :
  AdapterComponent<BookOverviewHeaderModel, BookOverviewHeaderHolder>(BookOverviewHeaderModel::class) {

  override fun onCreateViewHolder(parent: ViewGroup) =
    BookOverviewHeaderHolder(parent, listener)

  override fun onBindViewHolder(model: BookOverviewHeaderModel, holder: BookOverviewHeaderHolder) {
    holder.bind(model)
  }
}
