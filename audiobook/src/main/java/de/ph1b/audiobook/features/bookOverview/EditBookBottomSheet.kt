package de.ph1b.audiobook.features.bookOverview

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.BottomSheetDialogFragment
import android.widget.TextView
import com.bluelinelabs.conductor.Controller
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.R
import de.ph1b.audiobook.databinding.BookMoreBottomSheetBinding
import de.ph1b.audiobook.features.bookmarks.BookmarkController
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.*
import de.ph1b.audiobook.persistence.BookRepository
import e
import javax.inject.Inject

/**
 * Bottom sheet dialog fragment that will be displayed when a book edit was requested
 *
 * @author Paul Woitaschek
 */
class EditBookBottomSheet : BottomSheetDialogFragment() {

  @Inject lateinit var repo: BookRepository

  private fun callback() = findCallback<Callback>(NI_TARGET)

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    App.component.inject(this)

    val dialog = BottomSheetDialog(context, R.style.BottomSheetStyle)

    // if there is no book, skip here
    val book = repo.bookById(bookId())
    if (book == null) {
      e { "book is null. Return early" }
      return dialog
    }

    val binding = BookMoreBottomSheetBinding.inflate(activity.layoutInflater)
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
    val left = textView.leftCompoundDrawable()!!
    val tinted = left.tinted(context.color(R.color.icon_color))
    textView.setCompoundDrawables(tinted, textView.topCompoundDrawable(), textView.rightCompoundDrawable(), textView.bottomCompoundDrawable())
  }

  private fun bookId() = arguments.getLong(NI_BOOK)

  companion object {
    private const val NI_BOOK = "ni#book"
    private const val NI_TARGET = "ni#target"
    fun <T> newInstance(target: T, book: Book) where T : Controller, T : Callback = EditBookBottomSheet().apply {
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
