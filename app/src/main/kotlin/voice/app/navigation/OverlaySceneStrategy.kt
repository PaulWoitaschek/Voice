package voice.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.get
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import voice.navigation.OverlayNav

class OverlaySceneStrategy<T : Any> : SceneStrategy<T> {

  override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
    val lastEntry = entries.lastOrNull() ?: return null
    if (lastEntry.metadata[OverlayNav.OverlayKey] != true) return null
    return EntryOverlayScene(
      key = lastEntry.contentKey,
      previousEntries = entries.dropLast(1),
      overlaidEntries = entries.dropLast(1),
      entry = lastEntry,
    )
  }
}

private data class EntryOverlayScene<T : Any>(
  override val key: Any,
  override val previousEntries: List<NavEntry<T>>,
  override val overlaidEntries: List<NavEntry<T>>,
  private val entry: NavEntry<T>,
) : OverlayScene<T> {

  override val entries: List<NavEntry<T>> = listOf(entry)

  override val content: @Composable (() -> Unit) = {
    entry.Content()
  }
}
