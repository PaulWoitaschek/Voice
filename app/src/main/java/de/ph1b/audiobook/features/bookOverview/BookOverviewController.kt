package de.ph1b.audiobook.features.bookOverview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bluelinelabs.conductor.RouterTransaction
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.features.BaseController
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewAdapter
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewClick
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewItemDecoration
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewHeaderType
import de.ph1b.audiobook.features.bookPlaying.BookPlayController
import de.ph1b.audiobook.features.folderOverview.FolderOverviewController
import de.ph1b.audiobook.features.imagepicker.ImagePickerController
import de.ph1b.audiobook.features.settings.SettingsController
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.conductor.asTransaction
import de.ph1b.audiobook.misc.conductor.clearAfterDestroyView
import de.ph1b.audiobook.misc.conductor.clearAfterDestroyViewNullable
import de.ph1b.audiobook.misc.postedIfComputingLayout
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.uitools.BookChangeHandler
import de.ph1b.audiobook.uitools.PlayPauseDrawable
import kotlinx.android.synthetic.main.book_shelf.*
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

private const val COVER_FROM_GALLERY = 1

/**
 * Showing the shelf of all the available books and provide a navigation to each book.
 */
class BookOverviewController : BaseController(),
  EditCoverDialogFragment.Callback, EditBookBottomSheet.Callback {

  override val layoutRes = R.layout.book_shelf

  init {
    App.component.inject(this)
  }

  @field:[Inject Named(PrefKeys.CURRENT_BOOK)]
  lateinit var currentBookIdPref: Pref<UUID>

  @Inject
  lateinit var viewModel: BookOverviewViewModel

  private var playPauseDrawable: PlayPauseDrawable by clearAfterDestroyView()
  private var adapter: BookOverviewAdapter by clearAfterDestroyView()
  private var currentTapTarget by clearAfterDestroyViewNullable<TapTargetView>()
  private var menuBook: Book? = null
  private var pendingTransaction: FragmentTransaction? = null

  override fun onViewCreated() {
    playPauseDrawable = PlayPauseDrawable()
    setupToolbar()
    setupFab()
    setupRecyclerView()

    viewModel.state
      .subscribe(::render)
      .disposeOnDestroyView()
    viewModel.coverChanged
      .subscribe(::bookCoverChanged)
      .disposeOnDestroyView()
  }

  private fun setupFab() {
    fab.setIconDrawable(playPauseDrawable)
    fab.setOnClickListener { viewModel.playPause() }
  }

  private fun setupRecyclerView() {
    recyclerView.setHasFixedSize(true)
    adapter = BookOverviewAdapter { book, clickType ->
      when (clickType) {
        BookOverviewClick.REGULAR -> invokeBookSelectionCallback(book)
        BookOverviewClick.MENU -> {
          val editDialog = EditBookBottomSheet.newInstance(this, book)
          editDialog.show(fragmentManager, "editBottomSheet")
        }
      }
    }
    recyclerView.adapter = adapter
    // without this the item would blink on every change
    val anim = recyclerView.itemAnimator as SimpleItemAnimator
    anim.supportsChangeAnimations = false
    val listDecoration = BookOverviewItemDecoration(activity)
    recyclerView.addItemDecoration(listDecoration)
    recyclerView.layoutManager = LinearLayoutManager(activity)
  }

  private fun setupToolbar() {
    toolbar.inflateMenu(R.menu.book_shelf)
    toolbar.title = getString(R.string.app_name)
    toolbar.setOnMenuItemClickListener {
      when (it.itemId) {
        R.id.action_settings -> {
          val transaction = SettingsController().asTransaction()
          router.pushController(transaction)
          true
        }
        R.id.library -> {
          toFolderOverview()
          true
        }
        else -> false
      }
    }
  }

  private fun toFolderOverview() {
    val controller = FolderOverviewController()
    router.pushController(controller.asTransaction())
  }

  override fun onActivityResumed(activity: Activity) {
    super.onActivityResumed(activity)

    pendingTransaction?.commit()
    pendingTransaction = null
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      COVER_FROM_GALLERY -> {
        if (resultCode == Activity.RESULT_OK) {
          val imageUri = data?.data
          val book = menuBook
          if (imageUri == null || book == null) {
            return
          }

          @SuppressLint("CommitTransaction")
          pendingTransaction = fragmentManager.beginTransaction()
            .add(
              EditCoverDialogFragment.newInstance(this, book, imageUri),
              EditCoverDialogFragment.TAG
            )
        }
      }
      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  private fun invokeBookSelectionCallback(book: Book) {
    currentBookIdPref.value = book.id
    val transaction = RouterTransaction.with(BookPlayController(book.id))
    val transition = BookChangeHandler()
    transition.transitionName = book.coverTransitionName
    transaction.pushChangeHandler(transition)
      .popChangeHandler(transition)
    router.pushController(transaction)
  }

  private fun render(state: BookOverviewState) {
    Timber.i("render ${state.javaClass.simpleName}")
    when (state) {
      is BookOverviewState.Content -> {
        val content = ArrayList<Any>().apply {
          if (state.currentBooks.isNotEmpty()) {
            add(BookOverviewHeaderType.CURRENT)
            addAll(state.currentBooks)
          }
          if (state.notStartedBooks.isNotEmpty()) {
            add(BookOverviewHeaderType.NOT_STARTED)
            addAll(state.notStartedBooks)
          }
          if (state.completedBooks.isNotEmpty()) {
            add(BookOverviewHeaderType.FINISHED)
            addAll(state.completedBooks)
          }
        }
        adapter.submitList(content)
        val currentBook = state.currentBook

        fab.isVisible = currentBook != null
        showPlaying(state.playing)
      }
      is BookOverviewState.NoFolderSet -> {
        showNoFolderWarning()
      }
    }
    loadingProgress.isVisible = state == BookOverviewState.Loading
  }

  private fun showPlaying(playing: Boolean) {
    Timber.i("Called showPlaying $playing")
    val laidOut = ViewCompat.isLaidOut(fab)
    if (playing) {
      playPauseDrawable.transformToPause(laidOut)
    } else {
      playPauseDrawable.transformToPlay(laidOut)
    }
  }

  /** Show a warning that no audiobook folder was chosen */
  private fun showNoFolderWarning() {
    if (currentTapTarget?.isVisible == true)
      return

    val target = TapTarget.forToolbarMenuItem(
      toolbar,
      R.id.library,
      getString(R.string.onboarding_title),
      getString(R.string.onboarding_content)
    )
      .cancelable(false)
      .tintTarget(false)
      .outerCircleColor(R.color.accentDark)
      .descriptionTextColorInt(Color.WHITE)
      .textColorInt(Color.WHITE)
      .targetCircleColorInt(Color.BLACK)
      .transparentTarget(true)
    currentTapTarget = TapTargetView.showFor(activity, target, object : TapTargetView.Listener() {
      override fun onTargetClick(view: TapTargetView?) {
        super.onTargetClick(view)
        toFolderOverview()
      }
    })
  }

  private fun bookCoverChanged(bookId: UUID) {
    // there is an issue where notifyDataSetChanges throws:
    // java.lang.IllegalStateException: Cannot call this method while RecyclerView is computing a layout or scrolling
    recyclerView.postedIfComputingLayout {
      adapter.reloadBookCover(bookId)
    }
  }

  override fun onBookCoverChanged(book: Book) {
    recyclerView.postedIfComputingLayout {
      adapter.reloadBookCover(book.id)
    }
  }

  override fun onInternetCoverRequested(book: Book) {
    router.pushController(ImagePickerController(book).asTransaction())
  }

  override fun onFileCoverRequested(book: Book) {
    menuBook = book
    val galleryPickerIntent = Intent(Intent.ACTION_PICK)
    galleryPickerIntent.type = "image/*"
    startActivityForResult(galleryPickerIntent, COVER_FROM_GALLERY)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    recyclerView.adapter = null
  }

  override fun onAttach(view: View) {
    super.onAttach(view)
    viewModel.attach()
  }
}
