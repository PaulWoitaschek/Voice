package voice.playbackScreen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.squareup.anvil.annotations.ContributesTo
import voice.common.AppScope
import voice.common.BookId
import voice.common.compose.ComposeController
import voice.common.rootComponentAs
import voice.data.getBookId
import voice.data.putBookId
import voice.logging.core.Logger
import voice.sleepTimer.SleepTimerDialogController
import javax.inject.Inject

private const val NI_BOOK_ID = "niBookId"

class BookPlayController(bundle: Bundle) : ComposeController(bundle) {

  constructor(bookId: BookId) : this(Bundle().apply { putBookId(NI_BOOK_ID, bookId) })

  @Inject
  lateinit var viewModel: BookPlayViewModel

  private val bookId: BookId = bundle.getBookId(NI_BOOK_ID)!!

  init {
    rootComponentAs<Component>().inject(this)
    this.viewModel.bookId = bookId
  }

  @Composable
  override fun Content() {
    val windowSizeClass: WindowSizeClass = calculateWindowSizeClass(activity = activity!!)
    val snackbarHostState = remember { SnackbarHostState() }
    val dialogState = viewModel.dialogState.value
    val viewState = remember(viewModel) { viewModel.viewState() }
      .collectAsState(initial = null).value ?: return
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
      viewModel.viewEffects.collect { viewEffect ->
        when (viewEffect) {
          BookPlayViewEffect.BookmarkAdded -> {
            snackbarHostState.showSnackbar(message = context.getString(R.string.bookmark_added))
          }

          BookPlayViewEffect.RequestIgnoreBatteryOptimization -> {
            val result = snackbarHostState.showSnackbar(
              message = context.getString(R.string.battery_optimization_rationale),
              duration = SnackbarDuration.Long,
              actionLabel = context.getString(R.string.battery_optimization_action),
            )
            if (result == SnackbarResult.ActionPerformed) {
              toBatteryOptimizations()
            }
          }

          BookPlayViewEffect.ShowSleepTimeDialog -> {
            openSleepTimeDialog()
          }
        }
      }
    }
    BookPlayView(
      viewState,
      onPlayClick = { viewModel.playPause() },
      onFastForwardClick = { viewModel.fastForward() },
      onRewindClick = { viewModel.rewind() },
      onSeek = { viewModel.seekTo(it.inWholeMilliseconds) },
      onBookmarkClick = { viewModel.onBookmarkClicked() },
      onBookmarkLongClick = { viewModel.onBookmarkLongClicked() },
      onSkipSilenceClick = { viewModel.toggleSkipSilence() },
      onSleepTimerClick = { viewModel.toggleSleepTimer() },
      onVolumeBoostClick = { viewModel.onVolumeGainIconClicked() },
      onSpeedChangeClick = { viewModel.onPlaybackSpeedIconClicked() },
      onCloseClick = { router.popController(this@BookPlayController) },
      onSkipToNext = { viewModel.next() },
      onSkipToPrevious = { viewModel.previous() },
      onCurrentChapterClick = { viewModel.onCurrentChapterClicked() },
      useLandscapeLayout = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact,
      snackbarHostState = snackbarHostState,
    )
    if (dialogState != null) {
      when (dialogState) {
        is BookPlayDialogViewState.SpeedDialog -> {
          SpeedDialog(dialogState, viewModel)
        }

        is BookPlayDialogViewState.VolumeGainDialog -> {
          VolumeGainDialog(dialogState, viewModel)
        }

        is BookPlayDialogViewState.SelectChapterDialog -> {
          SelectChapterDialog(dialogState, viewModel)
        }
      }
    }
  }

  private fun toBatteryOptimizations() {
    val intent = Intent()
      .apply {
        @Suppress("BatteryLife")
        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        data = Uri.parse("package:${activity!!.packageName}")
      }
    try {
      startActivity(intent)
    } catch (e: ActivityNotFoundException) {
      Logger.e(e, "Can't request ignoring battery optimizations")
    }
  }

  private fun openSleepTimeDialog() {
    SleepTimerDialogController(bookId)
      .showDialog(router)
  }

  @ContributesTo(AppScope::class)
  interface Component {
    fun inject(target: BookPlayController)
  }
}
