package voice.migration

import androidx.compose.runtime.Composable
import com.squareup.anvil.annotations.ContributesTo
import kotlinx.coroutines.runBlocking
import voice.common.compose.ComposeController
import voice.core.AppScope
import voice.core.rootComponentAs
import voice.migration.views.Migration
import javax.inject.Inject

class MigrationController : ComposeController() {

  @Inject
  lateinit var viewModel: MigrationViewModel

  init {
    rootComponentAs<Component>().inject(this)
  }

  @Composable
  override fun Content() {
    val viewState = runBlocking {
      viewModel.viewState()
    }
    Migration(viewState) {
      router.popController(this)
    }
  }

  @ContributesTo(AppScope::class)
  interface Component {
    fun inject(target: MigrationController)
  }
}
