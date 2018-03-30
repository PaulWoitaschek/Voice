package de.ph1b.audiobook.features.bookOverview

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.BottomSheetDialogFragment
import android.widget.TextView
import com.bluelinelabs.conductor.Controller
import dagger.android.support.AndroidSupportInjection
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.databinding.BookMoreBottomSheetBinding
import de.ph1b.audiobook.features.bookmarks.BookmarkController
import de.ph1b.audiobook.misc.RouterProvider
import de.ph1b.audiobook.misc.bottomCompoundDrawable
import de.ph1b.audiobook.misc.color
import de.ph1b.audiobook.misc.conductor.asTransaction
import de.ph1b.audiobook.misc.endCompoundDrawable
import de.ph1b.audiobook.misc.findCallback
import de.ph1b.audiobook.misc.startCompoundDrawable
import de.ph1b.audiobook.misc.tinted
import de.ph1b.audiobook.misc.topCompoundDrawable
import timber.log.Timber
import javax.inject.Inject

/**
 * Bottom sheet dialog fragment that will be displayed when a book edit was requested
 */
class EditBookBottomSheet : BottomSheetDialogFragment() {

  @Inject
  lateinit var repo: BookRepository

  private fun callback() = findCallback<Callback>(NI_TARGET)

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    AndroidSupportInjection.inject(this)

    val dialog = BottomSheetDialog(context!!, R.style.BottomSheetStyle)

    // if there is no book, skip here
    val book = repo.bookById(bookId())
    if (book == null) {
      Timber.e("book is null. Return early")
      return dialog
    }

    val binding = BookMoreBottomSheetBinding.inflate(activity!!.layoutInflater)!!
    dialog.setContentView(binding.root)

    binding.title.setOnClickListener {
      EditBookTitleDialogFragment.newInstance(book)
        .show(fragmentManager, EditBookTitleDialogFragment.TAG)
      dismiss()
    }
    binding.internetCover.setOnClickListener {
      callback().onInternetCoverRequested(book)
      dismiss()
    }
    binding.fileCover.setOnClickListener {
      callback().onFileCoverRequested(book)
      dismiss()
    }
    binding.bookmark.setOnClickListener {
      val router = (activity as RouterProvider).provideRouter()
      val controller = BookmarkController.newInstance(book.id)
      router.pushController(controller.asTransaction())

      dismiss()
    }

    tintLeftDrawable(binding.title)
    tintLeftDrawable(binding.internetCover)
    tintLeftDrawable(binding.fileCover)
    tintLeftDrawable(binding.bookmark)

    return dialog
  }

  private fun tintLeftDrawable(textView: TextView) {
    val left = textView.startCompoundDrawable()!!
    val tinted = left.tinted(context!!.color(R.color.icon_color))
    textView.setCompoundDrawablesRelative(
      tinted,
      textView.topCompoundDrawable(),
      textView.endCompoundDrawable(),
      textView.bottomCompoundDrawable()
    )
  }

  private fun bookId() = arguments!!.getLong(NI_BOOK)

  companion object {
    private const val NI_BOOK = "ni#book"
    private const val NI_TARGET = "ni#target"
    fun <T> newInstance(target: T, book: Book) where T : Controller, T : Callback =
      EditBookBottomSheet().apply {
        arguments = Bundle().apply {
          putLong(NI_BOOK, book.id)
          putString(NI_TARGET, target.instanceId)
        }
      }
  }

  interface Callback {
    fun onInternetCoverRequested(book: Book)
    fun onFileCoverRequested(book: Book)
  }
}
