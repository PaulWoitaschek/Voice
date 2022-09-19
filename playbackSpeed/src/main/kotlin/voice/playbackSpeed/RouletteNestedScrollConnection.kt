package voice.playbackSpeed

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.math.abs

internal class RouletteNestedScrollConnection(
  private val state: LazyListState,
  private val values: List<RouletteItem>,
  private val lastSelected: RouletteItem,
  private val onSelected: (RouletteItem) -> Unit,
) : NestedScrollConnection {

  override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
    var selectorPosition: LazyListItemInfo? = null
    state.layoutInfo.visibleItemsInfo.forEach {
      val offset = abs(it.offset)
      if (offset >= 0 && offset < it.size / 2) {
        selectorPosition = it
      }
    }
    val item = values[selectorPosition?.index ?: lastSelected.index].copy(
      colorMix = (abs(selectorPosition?.offset ?: 0) * 100 / (selectorPosition?.size
        ?: 1)).toFloat() / 100
    )
    onSelected.invoke(item)
    return Offset.Zero
  }
}
