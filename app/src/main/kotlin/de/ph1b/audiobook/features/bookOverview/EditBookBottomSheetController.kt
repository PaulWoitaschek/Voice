package de.ph1b.audiobook.features.bookOverview

import android.app.Dialog
import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.ph1b.audiobook.common.conductor.DialogController
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.getBookId
import de.ph1b.audiobook.data.putBookId
import de.ph1b.audiobook.data.repo.BookRepo2
import de.ph1b.audiobook.databinding.BookMoreBottomSheetBinding
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.RouterProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

class EditBookBottomSheetController(args: Bundle) : DialogController(args) {

  @Inject
  lateinit var repo: BookRepo2

  private val bookId = args.getBookId(NI_BOOK)!!

  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    appComponent.inject(this)

    val dialog = BottomSheetDialog(activity!!)

    // if there is no book, skip here
    val book = runBlocking { repo.flow(bookId).first() }
    if (book == null) {
      Timber.e("book is null. Return early")
      return dialog
    }

    val binding = BookMoreBottomSheetBinding.inflate(activity!!.layoutInflater)
    dialog.setContentView(binding.root)

    binding.title.setOnClickListener {
      val router = (activity as RouterProvider).provideRouter()
      // todo EditBookTitleDialogController(book).showDialog(router)
      dismissDialog()
    }
    binding.internetCover.setOnClickListener {
      // todo callback().onInternetCoverRequested(book)
      dismissDialog()
    }
    binding.fileCover.setOnClickListener {
      // todo callback().onFileCoverRequested(book)
      dismissDialog()
    }
    binding.bookmark.setOnClickListener {
      val router = (activity as RouterProvider).provideRouter()
      // todo val controller = BookmarkController(book.id)
      // router.pushController(controller.asTransaction())

      dismissDialog()
    }

    return dialog
  }

  private fun callback() = targetController as Callback

  companion object {
    private const val NI_BOOK = "ni#book"
    operator fun <T> invoke(
      target: T,
      id: Book2.Id
    ): EditBookBottomSheetController where T : Controller, T : Callback {
      val args = Bundle().apply {
        putBookId(NI_BOOK, id)
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
