package de.ph1b.audiobook.features.settings

import androidx.core.view.isVisible
import de.ph1b.audiobook.R
import de.ph1b.audiobook.databinding.SettingsBinding
import de.ph1b.audiobook.features.ViewBindingController
import de.ph1b.audiobook.features.bookPlaying.SeekDialogController
import de.ph1b.audiobook.features.settings.dialogs.AutoRewindDialogController
import de.ph1b.audiobook.features.settings.dialogs.SupportDialogController
import de.ph1b.audiobook.injection.appComponent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class SettingsController : ViewBindingController<SettingsBinding>(SettingsBinding::inflate) {

  @Inject
  lateinit var viewModel: SettingsViewModel

  init {
    appComponent.inject(this)
  }

  override fun SettingsBinding.onBindingCreated() {
    setupToolbar()

    resumePlayback.onCheckedChanged { viewModel.toggleResumeOnReplug() }
    darkTheme.onCheckedChanged { viewModel.toggleDarkTheme() }

    skipAmount.setOnClickListener {
      viewModel.changeSkipAmount()
    }
    autoRewind.setOnClickListener {
      viewModel.changeAutoRewindAmount()
    }
  }

  override fun SettingsBinding.onAttach() {
    lifecycleScope.launch {
      viewModel.viewEffects.collect {
        handleViewEffect(it)
      }
    }

    lifecycleScope.launch {
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

  private fun SettingsBinding.render(state: SettingsViewState) {
    Timber.d("render $state")
    darkTheme.isVisible = state.showDarkThemePref
    darkTheme.setChecked(state.useDarkTheme)
    resumePlayback.setChecked(state.resumeOnReplug)
    skipAmount.setDescription(resources!!.getQuantityString(R.plurals.seconds, state.seekTimeInSeconds, state.seekTimeInSeconds))
    autoRewind.setDescription(resources!!.getQuantityString(R.plurals.seconds, state.autoRewindInSeconds, state.autoRewindInSeconds))
  }

  private fun SettingsBinding.setupToolbar() {
    toolbar.setOnMenuItemClickListener {
      if (it.itemId == R.id.action_contribute) {
        SupportDialogController().showDialog(router)
        true
      } else
        false
    }
    toolbar.setNavigationOnClickListener {
      activity!!.onBackPressed()
    }
  }
}
