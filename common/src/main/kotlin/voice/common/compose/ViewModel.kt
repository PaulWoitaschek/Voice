package voice.common.compose

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@PublishedApi
internal class HoldingViewModel<T>(val value: T) : ViewModel()

@Composable
inline fun <reified T> rememberScoped(
  crossinline create: () -> T
): T {
  return viewModel(key = T::class.qualifiedName) {
    HoldingViewModel(create())
  }.value
}
