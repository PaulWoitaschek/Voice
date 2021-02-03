package de.ph1b.audiobook.features.bookOverview

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.view.doOnLayout
import com.bluelinelabs.conductor.Controller
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.databinding.BookMoreBottomSheetBinding
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewCategory
import de.ph1b.audiobook.features.bookOverview.list.header.category
import de.ph1b.audiobook.features.bookmarks.BookmarkController
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.misc.DialogController
import de.ph1b.audiobook.misc.RouterProvider
import de.ph1b.audiobook.misc.conductor.asTransaction
import de.ph1b.audiobook.misc.getUUID
import de.ph1b.audiobook.misc.putUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
    appComponent.inject(this)

    val dialog = BottomSheetDialog(activity!!)

    // if there is no book, skip here
    val book = repo.bookById(bookId)
    if (book == null) {
      Timber.e("book is null. Return early")
      return dialog
    }

    val binding = BookMoreBottomSheetBinding.inflate(activity!!.layoutInflater)
    dialog.setContentView(binding.root)

    BottomSheetBehavior.from(dialog.findViewById(R.id.design_bottom_sheet)!!).apply {
      binding.root.doOnLayout {
        peekHeight = it.height
      }
    }

    binding.title.setOnClickListener {
      val router = (activity as RouterProvider).provideRouter()
      EditBookTitleDialogController(book).showDialog(router)
      dismissDialog()
    }
    binding.internetCover.setOnClickListener {
      callback().onInternetCoverRequested(book)
      dismissDialog()
    }
    binding.fileCover.setOnClickListener {
      callback().onFileCoverRequested(book)
      dismissDialog()
    }
    binding.bookmark.setOnClickListener {
      val router = (activity as RouterProvider).provideRouter()
      val controller = BookmarkController(book.id)
      router.pushController(controller.asTransaction())

      dismissDialog()
    }
    binding.markAsComplete.setOnClickListener {
      GlobalScope.launch(Dispatchers.IO) {
        val updatedBook = book.updateContent {
          copy(
            settings = settings.copy(
              currentFile = chapters[chapters.size - 1].file,
              positionInChapter = currentChapter.duration
            )
          )
        }
        repo.addBook(updatedBook)
      }
      dismissDialog()
    }
    binding.markAsComplete.visibility = if (book.category == BookOverviewCategory.FINISHED) {
      View.GONE
    } else {
      View.VISIBLE
    }

    binding.markAsNotStarted.setOnClickListener {
      GlobalScope.launch(Dispatchers.IO) {
        val updatedBook = book.updateContent {
          copy(
            settings = settings.copy(
              currentFile = chapters[0].file,
              positionInChapter = 0
            )
          )
        }
        repo.addBook(updatedBook)
      }
      dismissDialog()
    }
    binding.markAsNotStarted.visibility = if (book.category == BookOverviewCategory.NOT_STARTED) {
      View.GONE
    } else {
      View.VISIBLE
    }

    return dialog
  }

  private fun callback() = targetController as Callback

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
