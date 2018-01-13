package de.ph1b.audiobook.features.bookOverview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.support.v4.app.FragmentTransaction
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SimpleItemAnimator
import com.bluelinelabs.conductor.RouterTransaction
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import de.ph1b.audiobook.R
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.databinding.BookShelfBinding
import de.ph1b.audiobook.features.bookOverview.list.BookShelfAdapter
import de.ph1b.audiobook.features.bookOverview.list.BookShelfClick
import de.ph1b.audiobook.features.bookOverview.list.BookShelfItemDecoration
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
import de.ph1b.audiobook.mvp.MvpController
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.uitools.BookChangeHandler
import de.ph1b.audiobook.uitools.PlayPauseDrawable
import de.ph1b.audiobook.uitools.visible
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

private const val COVER_FROM_GALLERY = 1

/**
 * Showing the shelf of all the available books and provide a navigation to each book.
 */
class BookShelfController : MvpController<BookShelfView, BookShelfPresenter, BookShelfBinding>(), EditCoverDialogFragment.Callback, EditBookBottomSheet.Callback, BookShelfView {

  override fun createPresenter() = App.component.bookShelfPresenter
  override val layoutRes = R.layout.book_shelf

  override fun provideView() = this

  init {
    App.component.inject(this)
  }

  @field:[Inject Named(PrefKeys.CURRENT_BOOK)]
  lateinit var currentBookIdPref: Pref<Long>

  private var playPauseDrawable: PlayPauseDrawable by clearAfterDestroyView()
  private var adapter: BookShelfAdapter by clearAfterDestroyView()
  private var currentTapTarget by clearAfterDestroyViewNullable<TapTargetView>()
  private var menuBook: Book? = null
  private var pendingTransaction: FragmentTransaction? = null

  override fun onBindingCreated(binding: BookShelfBinding) {
    playPauseDrawable = PlayPauseDrawable()
    setupToolbar()
    setupFab()
    setupRecyclerView()
  }

  private fun setupFab() {
    binding.fab.setIconDrawable(playPauseDrawable)
    binding.fab.setOnClickListener { presenter.playPause() }
  }

  private fun setupRecyclerView() {
    binding.recyclerView.setHasFixedSize(true)
    adapter = BookShelfAdapter { book, clickType ->
      when (clickType) {
        BookShelfClick.REGULAR -> invokeBookSelectionCallback(book)
        BookShelfClick.MENU -> {
          val editDialog = EditBookBottomSheet.newInstance(this, book)
          editDialog.show(fragmentManager, "editBottomSheet")
        }
      }
    }
    binding.recyclerView.adapter = adapter
    // without this the item would blink on every change
    val anim = binding.recyclerView.itemAnimator as SimpleItemAnimator
    anim.supportsChangeAnimations = false
    val listDecoration = BookShelfItemDecoration(activity)
    binding.recyclerView.addItemDecoration(listDecoration)
    binding.recyclerView.layoutManager = LinearLayoutManager(activity)
  }

  private fun setupToolbar() {
    binding.toolbar.inflateMenu(R.menu.book_shelf)
    binding.toolbar.title = getString(R.string.app_name)
    binding.toolbar.setOnMenuItemClickListener {
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

  override fun render(state: BookShelfState) {
    Timber.i("render ${state.javaClass.simpleName}")
    when (state) {
      is BookShelfState.Content -> {
        adapter.newDataSet(state.books)
        val currentBook = state.currentBook

        binding.fab.visible = currentBook != null
        showPlaying(state.playing)
      }
      is BookShelfState.NoFolderSet -> {
        showNoFolderWarning()
      }
    }
    binding.loadingProgress.visible = state is BookShelfState.Loading
  }

  private fun showPlaying(playing: Boolean) {
    Timber.i("Called showPlaying $playing")
    val laidOut = ViewCompat.isLaidOut(binding.fab)
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
        binding.toolbar, R.id.library, getString(R.string.onboarding_title), getString(R.string.onboarding_content))
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

  override fun bookCoverChanged(bookId: Long) {
    // there is an issue where notifyDataSetChanges throws:
    // java.lang.IllegalStateException: Cannot call this method while RecyclerView is computing a layout or scrolling
    binding.recyclerView.postedIfComputingLayout {
      adapter.reloadBookCover(bookId)
    }
  }

  override fun onBookCoverChanged(book: Book) {
    binding.recyclerView.postedIfComputingLayout {
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

  override fun onDestroyBinding(binding: BookShelfBinding) {
    super.onDestroyBinding(binding)
    binding.recyclerView.adapter = null
  }
}
