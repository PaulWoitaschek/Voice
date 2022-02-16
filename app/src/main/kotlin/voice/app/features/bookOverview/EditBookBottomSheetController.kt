package voice.app.features.bookOverview

import android.app.Dialog
import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import com.google.android.material.bottomsheet.BottomSheetDialog
import voice.app.databinding.BookMoreBottomSheetBinding
import voice.app.features.bookmarks.BookmarkController
import voice.app.misc.RouterProvider
import voice.app.misc.conductor.asTransaction
import voice.common.conductor.DialogController
import voice.data.Book
import voice.data.getBookId
import voice.data.putBookId

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
      val controller = BookmarkController(bookId)
      router.pushController(controller.asTransaction())

      dismissDialog()
    }

    return dialog
  }

  private fun callback() = targetController as Callback

  companion object {
    private const val NI_BOOK = "ni#book"
    operator fun <T> invoke(
      target: T,
      id: Book.Id
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
    fun onInternetCoverRequested(book: Book.Id)
    fun onFileCoverRequested(book: Book.Id)
  }
}
