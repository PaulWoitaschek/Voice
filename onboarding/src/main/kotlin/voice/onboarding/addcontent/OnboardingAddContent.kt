package voice.onboarding.addcontent

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.squareup.anvil.annotations.ContributesTo
import voice.common.AppScope
import voice.common.compose.VoiceTheme
import voice.common.compose.rememberScoped
import voice.common.rootComponentAs
import voice.folderPicker.selectFolder.SelectFolder

@ContributesTo(AppScope::class)
interface OnboardingAddContentComponent {
  val viewModel: OnboardingAddContentViewModel
}

@Composable
fun OnboardingAddContent() {
  val viewModel = rememberScoped {
    rootComponentAs<OnboardingAddContentComponent>().viewModel
  }
  SelectFolder(
    onBack = {
      viewModel.back()
    },
    onAdd = { folderType, uri ->
      viewModel.add(uri, folderType)
    },
  )
}

@Composable
@Preview
private fun OnboardingAddContentPreview() {
  VoiceTheme {
    OnboardingAddContent()
  }
}
