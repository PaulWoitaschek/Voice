package de.ph1b.audiobook.features.bookOverview

import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import com.bluelinelabs.conductor.Controller
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.features.bookmarks.BookmarkController
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.DialogController
import de.ph1b.audiobook.misc.DialogLayoutContainer
import de.ph1b.audiobook.misc.RouterProvider
import de.ph1b.audiobook.misc.bottomCompoundDrawable
import de.ph1b.audiobook.misc.color
import de.ph1b.audiobook.misc.conductor.asTransaction
import de.ph1b.audiobook.misc.endCompoundDrawable
import de.ph1b.audiobook.misc.getUUID
import de.ph1b.audiobook.misc.inflate
import de.ph1b.audiobook.misc.putUUID
import de.ph1b.audiobook.misc.startCompoundDrawable
import de.ph1b.audiobook.misc.tinted
import de.ph1b.audiobook.misc.topCompoundDrawable
import kotlinx.android.synthetic.main.book_more_bottom_sheet.*
import timber.log.Timber
import javax.inject.Inject

/**
 * Bottom sheet dialog fragment that will be displayed when a book edit was requested
 */
class EditBookBottomSheetController(args: Bundle) : DialogController(args) {

  @Inject
  lateinit var repo: BookRepository

  private val bookId = args.getUUID(NI_BOOK)

  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    App.component.inject(this)

    val dialog = BottomSheetDialog(activity!!, R.style.BottomSheetStyle)

    // if there is no book, skip here
    val book = repo.bookById(bookId)
    if (book == null) {
      Timber.e("book is null. Return early")
      return dialog
    }

    val container =
      DialogLayoutContainer(activity!!.layoutInflater.inflate(R.layout.book_more_bottom_sheet))
    dialog.setContentView(container.containerView)

    container.title.setOnClickListener {
      val router = (activity as RouterProvider).provideRouter()
      EditBookTitleDialogController.newInstance(book)
        .showDialog(router, EditBookTitleDialogController.TAG)
      dismissDialog()
    }
    container.internetCover.setOnClickListener {
      callback().onInternetCoverRequested(book)
      dismissDialog()
    }
    container.fileCover.setOnClickListener {
      callback().onFileCoverRequested(book)
      dismissDialog()
    }
    container.bookmark.setOnClickListener {
      val router = (activity as RouterProvider).provideRouter()
      val controller = BookmarkController.newInstance(book.id)
      router.pushController(controller.asTransaction())

      dismissDialog()
    }

    tintLeftDrawable(container.title)
    tintLeftDrawable(container.internetCover)
    tintLeftDrawable(container.fileCover)
    tintLeftDrawable(container.bookmark)

    return dialog
  }

  private fun callback() = targetController as Callback

  private fun tintLeftDrawable(textView: TextView) {
    val left = textView.startCompoundDrawable()!!
    val tinted = left.tinted(activity!!.color(R.color.icon_color))
    textView.setCompoundDrawablesRelative(
      tinted,
      textView.topCompoundDrawable(),
      textView.endCompoundDrawable(),
      textView.bottomCompoundDrawable()
    )
  }

  companion object {
    private const val NI_BOOK = "ni#book"
    operator fun <T> invoke(
      target: T,
      book: Book
    ): EditBookBottomSheetController where T : Controller, T : Callback {
      val args = Bundle().apply {
        putUUID(NI_BOOK, book.id)
      }
      return EditBookBottomSheetController(args).apply {
        targetController = target
      }
    }
  }

  interface Callback {
    fun onInternetCoverRequested(book: Book)
    fun onFileCoverRequested(book: Book)
  }
}
