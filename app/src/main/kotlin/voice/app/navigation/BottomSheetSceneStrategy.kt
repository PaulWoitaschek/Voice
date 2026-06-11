package voice.app.navigation

import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.get
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import voice.navigation.BottomSheetNav

class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {

  override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
    val lastEntry = entries.lastOrNull() ?: return null
    val bottomSheetProperties = lastEntry.metadata[BottomSheetNav.BottomSheetKey] ?: return null
    return bottomSheetProperties.let { properties ->
      @Suppress("UNCHECKED_CAST")
      BottomSheetScene(
        key = lastEntry.contentKey as T,
        previousEntries = entries.dropLast(1),
        overlaidEntries = entries.dropLast(1),
        entry = lastEntry,
        modalBottomSheetProperties = properties,
        onBack = onBack,
      )
    }
  }
}

private data class BottomSheetScene<T : Any>(
  override val key: T,
  override val previousEntries: List<NavEntry<T>>,
  override val overlaidEntries: List<NavEntry<T>>,
  private val entry: NavEntry<T>,
  private val modalBottomSheetProperties: ModalBottomSheetProperties,
  private val onBack: () -> Unit,
) : OverlayScene<T> {

  override val entries: List<NavEntry<T>> = listOf(entry)

  override val content: @Composable (() -> Unit) = {
    val lifecycleOwner = rememberLifecycleOwner()
    val sheetState = rememberBottomSheetState(
      initialValue = Hidden,
      enabledValues = setOf(Hidden, Expanded),
    )
    ModalBottomSheet(
      onDismissRequest = onBack,
      properties = modalBottomSheetProperties,
      sheetState = sheetState,
    ) {
      CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
        entry.Content()
      }
    }
  }
}
