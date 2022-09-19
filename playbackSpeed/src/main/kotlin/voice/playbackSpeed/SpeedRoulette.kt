import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.chrisbanes.snapper.rememberSnapperFlingBehavior
import voice.playbackSpeed.RouletteItem
import voice.playbackSpeed.RouletteNestedScrollConnection
import voice.playbackSpeed.Utils

@OptIn(ExperimentalSnapperApi::class)
@Composable
internal fun SpeedRoulette(
  value: RouletteItem,
  values: List<RouletteItem>,
  listState: LazyListState,
  divisionWidth: Dp = 24.dp,
  lineWidth: Dp = 2.dp,
  lineHeight: Dp = 40.dp,
  selectedLineHeight: Dp = 48.dp,
  selectedColor: Color,
  unselectedColor: Color,
  onValueChange: (RouletteItem) -> Unit,
) {
  val configuration = LocalConfiguration.current
  LazyRow(
    state = listState,
    flingBehavior = rememberSnapperFlingBehavior(listState),
    modifier = Modifier
      .fillMaxWidth()
      .nestedScroll(
        RouletteNestedScrollConnection(listState, values, value) {

          onValueChange.invoke(it)
        },
      ),
    contentPadding = PaddingValues(
      horizontal = calculateCenter(
        configuration,
        divisionWidth,
      ),
    ),
  ) {
    items(
      count = values.size,
      itemContent = { index ->
        val item = values[index]
        val color = if (index == value.index) Utils.blendColors(
          selectedColor,
          unselectedColor,
          value.colorMix,
        ) else unselectedColor
        Box(
          modifier = Modifier
            .height(66.dp)
            .width(divisionWidth),
        ) {
          val boxHeight = if (index == value.index) selectedLineHeight else lineHeight
          Box(
            Modifier
              .height(boxHeight)
              .width(lineWidth)
              .align(alignment = Alignment.TopCenter)
              .background(color),
          ) {
          }
          if (item.value % 0.5f == 0f) {
            Text(
              item.value.toString() + "x",
              color = unselectedColor,
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.align(alignment = Alignment.BottomCenter),
            )
          } else {
            Text(text = "")
          }
        }
      },
    )
  }

  LaunchedEffect(true) {
    listState.scrollToItem(value.index)
  }
}

private fun calculateCenter(configuration: Configuration, divisionWidth: Dp): Dp {
  return configuration.screenWidthDp.dp / 2 - divisionWidth / 2
}

