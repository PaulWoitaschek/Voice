package de.ph1b.audiobook.features.settings

import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import de.ph1b.audiobook.R
import de.ph1b.audiobook.features.BaseController
import de.ph1b.audiobook.features.bookPlaying.SeekDialogController
import de.ph1b.audiobook.features.settings.dialogs.AutoRewindDialogController
import de.ph1b.audiobook.features.settings.dialogs.SupportDialogController
import de.ph1b.audiobook.injection.appComponent
import kotlinx.android.synthetic.main.settings.*
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import javax.inject.Inject


class SettingsController : BaseController() {

  @Inject
  lateinit var viewModel: SettingsViewModel

  init {
    appComponent.inject(this)
  }

  override val layoutRes = R.layout.settings

  override fun onViewCreated() {
    setupToolbar()

    resumePlayback.onCheckedChanged { viewModel.toggleResumeOnReplug() }
    darkTheme.onCheckedChanged { viewModel.toggleDarkTheme() }

    skipAmount.setOnClickListener {
      viewModel.changeSkipAmount()
    }
    autoRewind.setOnClickListener {
      viewModel.changeAutoRewindAmount()
    }

    lifecycleScope.launchWhenStarted {
      viewModel.viewEffects.collect {
        handleViewEffect(it)
      }
    }

    lifecycleScope.launchWhenStarted {
      viewModel.viewState().collect {
        render(it)
      }
    }
  }

  private fun handleViewEffect(effect: SettingsViewEffect) {
    when (effect) {
      is SettingsViewEffect.ShowChangeSkipAmountDialog -> {
        SeekDialogController().showDialog(router)
      }
      is SettingsViewEffect.ShowChangeAutoRewindAmountDialog -> {
        AutoRewindDialogController().showDialog(router)
      }
    }
  }

  private fun render(state: SettingsViewState) {
    Timber.d("render $state")
    darkTheme.isVisible = state.showDarkThemePref
    darkTheme.setChecked(state.useDarkTheme)
    resumePlayback.setChecked(state.resumeOnReplug)
    skipAmount.setDescription(resources!!.getQuantityString(R.plurals.seconds, state.seekTimeInSeconds, state.seekTimeInSeconds))
    autoRewind.setDescription(resources!!.getQuantityString(R.plurals.seconds, state.autoRewindInSeconds, state.autoRewindInSeconds))
  }

  private fun setupToolbar() {
    toolbar.setOnMenuItemClickListener {
      if (it.itemId == R.id.action_contribute) {
        SupportDialogController().showDialog(router)
        true
      } else
        false
    }
    toolbar.setNavigationOnClickListener {
      activity.onBackPressed()
    }
  }
}
