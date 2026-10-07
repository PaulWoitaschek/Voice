package voice.features.playbackScreen

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.ui.CoverTheme
import voice.core.ui.HoldSplashScreenWhile
import voice.features.playbackScreen.view.BookPlayView
import voice.features.sleepTimer.SleepTimerDialog
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR

@Composable
fun BookPlayScreen(bookId: BookId) {
  val viewModel = retain(bookId.value) {
    rootGraphAs<BookPlayGraph>()
      .bookPlayViewModelFactory
      .create(bookId)
  }
  val snackbarHostState = remember { SnackbarHostState() }
  val dialogState = viewModel.dialogState.value
  val viewState = viewModel.viewState()
  HoldSplashScreenWhile(loading = viewState == null)
  if (viewState == null) return
  val bookmarkAddedMessage = stringResource(StringsR.string.bookmark_added_snackbar)
  val addNoteAction = stringResource(StringsR.string.bookmark_action_add_note)
  val batteryOptimizationMessage = stringResource(StringsR.string.playback_battery_optimization_rationale)
  val batteryOptimizationAction = stringResource(StringsR.string.playback_battery_optimization_action)
  LaunchedEffect(viewModel) {
    viewModel.viewEffects.collect { viewEffect ->
      when (viewEffect) {
        is BookPlayViewEffect.BookmarkAdded -> {
          val result = snackbarHostState.showSnackbar(
            message = bookmarkAddedMessage,
            actionLabel = addNoteAction,
            duration = SnackbarDuration.Short,
          )
          if (result == SnackbarResult.ActionPerformed) {
            viewModel.onAddBookmarkNote(viewEffect.id)
          }
        }
        BookPlayViewEffect.RequestIgnoreBatteryOptimization -> {
          val result = snackbarHostState.showSnackbar(
            message = batteryOptimizationMessage,
            duration = SnackbarDuration.Long,
            actionLabel = batteryOptimizationAction,
          )
          if (result == SnackbarResult.ActionPerformed) {
            viewModel.onBatteryOptimizationRequested()
          }
        }
      }
    }
  }
  CoverTheme(cover = viewState.cover) {
    BookPlayView(
      viewState,
      bookId = bookId,
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
      onJumpBack = viewModel::onJumpBackClick,
      onJumpBackExpire = viewModel::onJumpBackExpire,
      useLandscapeLayout = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE,
      snackbarHostState = snackbarHostState,
    )
    when (dialogState) {
      null -> {}
      is BookPlayDialogViewState.SpeedDialog -> {
        SpeedSheet(
          dialogState = dialogState,
          onSpeedChange = viewModel::onPlaybackSpeedChanged,
          onDismiss = viewModel::dismissDialog,
        )
      }
      is BookPlayDialogViewState.VolumeGainDialog -> {
        VolumeGainSheet(
          dialogState = dialogState,
          onGainChange = viewModel::onVolumeGainChanged,
          onDismiss = viewModel::dismissDialog,
        )
      }
      is BookPlayDialogViewState.SelectChapterDialog -> {
        ChapterSheet(
          dialogState = dialogState,
          playing = viewState.playing,
          onChapterClick = viewModel::onChapterClick,
          onDismiss = viewModel::dismissDialog,
        )
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

@BindingContainer
@ContributesTo(AppScope::class)
object BookPlayProvider {

  @Provides
  @IntoSet
  fun bookPlayNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.Playback> { key ->
    NavEntry(key) {
      BookPlayScreen(bookId = key.bookId)
    }
  }
}
