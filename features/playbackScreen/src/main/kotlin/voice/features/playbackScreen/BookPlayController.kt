package voice.features.playbackScreen

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.ui.rememberScoped
import voice.features.playbackScreen.view.BookPlayView
import voice.features.sleepTimer.SleepTimerDialog
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR

@Composable
fun BookPlayScreen(bookId: BookId) {
  val viewModel = rememberScoped(bookId.value) {
    rootGraphAs<BookPlayGraph>()
      .bookPlayViewModelFactory
      .create(bookId)
  }
  val snackbarHostState = remember { SnackbarHostState() }
  val dialogState = viewModel.dialogState.value
  val viewState = viewModel.viewState()
    ?: return
  val context = LocalContext.current
  LaunchedEffect(viewModel) {
    viewModel.viewEffects.collect { viewEffect ->
      when (viewEffect) {
        BookPlayViewEffect.BookmarkAdded -> {
          snackbarHostState.showSnackbar(message = context.getString(StringsR.string.bookmark_added))
        }

        BookPlayViewEffect.RequestIgnoreBatteryOptimization -> {
          val result = snackbarHostState.showSnackbar(
            message = context.getString(StringsR.string.battery_optimization_rationale),
            duration = SnackbarDuration.Long,
            actionLabel = context.getString(StringsR.string.battery_optimization_action),
          )
          if (result == SnackbarResult.ActionPerformed) {
            viewModel.onBatteryOptimizationRequested()
          }
        }
      }
    }
  }
  BookPlayView(
    viewState,
    onPlayClick = viewModel::playPause,
    onFastForwardClick = viewModel::fastForward,
    onRewindClick = viewModel::rewind,
    onSeek = viewModel::seekTo,
    onBookmarkClick = viewModel::onBookmarkClick,
    onBookmarkLongClick = viewModel::onBookmarkLongClick,
    onSkipSilenceClick = viewModel::toggleSkipSilence,
    onSleepTimerClick = viewModel::toggleSleepTimer,
    onVolumeBoostClick = viewModel::onVolumeGainIconClick,
    onSpeedChangeClick = viewModel::onPlaybackSpeedIconClick,
    onCloseClick = viewModel::onCloseClick,
    onSkipToNext = viewModel::next,
    onSkipToPrevious = viewModel::previous,
    onCurrentChapterClick = viewModel::onCurrentChapterClick,
    useLandscapeLayout = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE,
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
      is BookPlayDialogViewState.SleepTimer -> {
        SleepTimerDialog(
          viewState = dialogState.viewState,
          onDismiss = viewModel::dismissDialog,
          onIncrementSleepTime = viewModel::incrementSleepTime,
          onDecrementSleepTime = viewModel::decrementSleepTime,
          onAcceptSleepTime = viewModel::onAcceptSleepTime,
          onAcceptSleepAtEndOfChapter = viewModel::onAcceptSleepAtEndOfChapter,
        )
      }
    }
  }
}

@ContributesTo(AppScope::class)
interface BookPlayGraph {
  val bookPlayViewModelFactory: BookPlayViewModel.Factory
}

@ContributesTo(AppScope::class)
interface BookPlayProvider {

  @Provides
  @IntoSet
  fun navEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.Playback> { key, backStack ->
    NavEntry(key) {
      BookPlayScreen(bookId = key.bookId)
    }
  }
}
