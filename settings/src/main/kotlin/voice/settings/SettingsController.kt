package voice.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import com.squareup.anvil.annotations.ContributesTo
import kotlinx.coroutines.launch
import voice.common.compose.ComposeController
import voice.core.AppScope
import voice.core.rootComponentAs
import voice.logging.core.Logger
import voice.settings.views.Settings
import javax.inject.Inject

class SettingsController : ComposeController() {

  @Inject
  lateinit var viewModel: SettingsViewModel

  init {
    rootComponentAs<Component>().inject(this)
  }

  override fun onCreateView() {
    lifecycleScope.launch {
      viewModel.viewEffects.collect {
        when (it) {
          SettingsViewEffect.CloseScreen -> {
            router.popController(this@SettingsController)
          }
          SettingsViewEffect.ToSupport -> {
            visitUri("https://github.com/PaulWoitaschek/Voice".toUri())
          }
          SettingsViewEffect.ToTranslations -> {
            visitUri("https://www.transifex.com/projects/p/voice".toUri())
          }
        }
      }
    }
  }

  @Composable
  override fun Content() {
    Settings(viewModel)
  }

  private fun visitUri(uri: Uri) {
    try {
      startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (exception: ActivityNotFoundException) {
      Logger.w(exception)
    }
  }

  @ContributesTo(AppScope::class)
  interface Component {
    fun inject(target: SettingsController)
  }
}
