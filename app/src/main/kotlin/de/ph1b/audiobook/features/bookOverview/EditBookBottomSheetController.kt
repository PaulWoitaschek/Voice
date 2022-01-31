package de.ph1b.audiobook.features.bookOverview

import android.app.Dialog
import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.ph1b.audiobook.common.conductor.DialogController
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.getBookId
import de.ph1b.audiobook.data.putBookId
import de.ph1b.audiobook.databinding.BookMoreBottomSheetBinding
import de.ph1b.audiobook.misc.RouterProvider

class EditBookBottomSheetController(args: Bundle) : DialogController(args) {

  private val bookId = args.getBookId(NI_BOOK)!!

  override fun onCreateDialog(savedViewState: Bundle?): Dialog {
    val dialog = BottomSheetDialog(activity!!)

    val binding = BookMoreBottomSheetBinding.inflate(activity!!.layoutInflater)
    dialog.setContentView(binding.root)

    binding.title.setOnClickListener {
      val router = (activity as RouterProvider).provideRouter()
      EditBookTitleDialogController(bookId).showDialog(router)
      dismissDialog()
    }
    binding.internetCover.setOnClickListener {
      callback().onInternetCoverRequested(bookId)
      dismissDialog()
    }
    binding.fileCover.setOnClickListener {
      callback().onFileCoverRequested(bookId)
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
    fun onInternetCoverRequested(book: Book2.Id)
    fun onFileCoverRequested(book: Book2.Id)
  }
}
