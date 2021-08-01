package voice.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import com.bluelinelabs.conductor.Controller
import com.squareup.anvil.annotations.ContributesTo
import de.ph1b.audiobook.AppScope
import de.ph1b.audiobook.rootComponentAs
import timber.log.Timber
import voice.settings.views.Settings
import javax.inject.Inject

class SettingsController : Controller() {

  @Inject
  lateinit var viewModel: SettingsViewModel

  init {
    rootComponentAs<Component>().inject(this)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
    val listener = object : SettingsViewListener {
      override fun close() {
        router.popController(this@SettingsController)
      }

      override fun toggleResumeOnReplug() {
        viewModel.toggleResumeOnReplug()
      }

      override fun seekAmountChanged(seconds: Int) {
        viewModel.changeSeekAmount(seconds)
      }

      override fun autoRewindAmountChanged(seconds: Int) {
        viewModel.changeAutoRewindAmount(seconds)
      }

      override fun openSupport() {
        visitUri("https://github.com/PaulWoitaschek/Voice".toUri())
      }

      override fun openTranslations() {
        visitUri("https://www.transifex.com/projects/p/voice".toUri())
      }

      override fun toggleDarkTheme() {
        viewModel.toggleDarkTheme()
      }
    }
    return ComposeView(container.context).apply {
      setContent {
        val viewState = viewModel.viewState().collectAsState(SettingsViewState.Empty)
        Settings(viewState.value, listener)
      }
    }
  }

  private fun visitUri(uri: Uri) {
    try {
      startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (exception: ActivityNotFoundException) {
      Timber.e(exception)
    }
  }

  @ContributesTo(AppScope::class)
  interface Component {
    fun inject(target: SettingsController)
  }
}
