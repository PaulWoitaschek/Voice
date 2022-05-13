package voice.migration

import androidx.compose.runtime.Composable
import voice.common.compose.ComposeController
import voice.migration.views.Migration
import voice.migration.views.MigrationViewStatePreviewProvider

class MigrationController : ComposeController() {

  @Composable
  override fun Content() {
    Migration(MigrationViewStatePreviewProvider().values.first())
  }
}
