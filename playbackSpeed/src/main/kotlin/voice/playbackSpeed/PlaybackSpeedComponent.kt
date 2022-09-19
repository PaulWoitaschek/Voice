package voice.playbackSpeed

import SpeedRoulette
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Chip
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import voice.common.compose.VoiceTheme

@Composable
fun PlaybackSpeedComponent(
  inputValue: Float,
  valueRange: ClosedFloatingPointRange<Float> = 0.5f..3.5f,
  valueStep: Float = 0.1f,
  quickSpeedValue: ArrayList<Float> = arrayListOf(0.5f, 1.0f, 1.5f, 2.0f, 2.5f),
  selectedColor: Color = Color.Red,
  unselectedColor: Color = Color.White,
  onValueChange: (Float) -> Unit,
) {
  val scope = rememberCoroutineScope()
  val values = generateScaleValues(valueRange, valueStep)
  val listState = rememberLazyListState()

  val initPosition = Utils.findPosition(values, inputValue)
  var selectedItem by remember { mutableStateOf(RouletteItem(inputValue, 0f, initPosition)) }

  Column {
    Text(
      text = "${selectedItem.value}x",
      modifier = Modifier.align(Alignment.CenterHorizontally),
      style = MaterialTheme.typography.titleLarge,
      color = selectedColor,
    )
    Icon(
      Icons.Filled.ArrowDropUp,
      contentDescription = "",
      modifier = Modifier.align(Alignment.CenterHorizontally),
      tint = selectedColor,
    )
    SpeedRoulette(
      value = selectedItem,
      values = values,
      listState = listState,
      selectedColor = selectedColor,
      unselectedColor = unselectedColor,
    ) {
      selectedItem = it
      onValueChange.invoke(it.value)
    }
    Spacer(modifier = Modifier.height(10.dp))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      quickSpeedValue.forEach { positionValue ->
        ChipBox(positionValue) {
          val position = Utils.findPosition(values, it)
          selectedItem = RouletteItem(it, 0f, position)
          scope.launch {
            listState.animateScrollToItem(Utils.findPosition(values, it))
          }
          onValueChange.invoke(selectedItem.value)
        }
      }
    }
  }
}

@Composable
private fun generateScaleValues(
  valueRange: ClosedFloatingPointRange<Float>,
  step: Float,
): ArrayList<RouletteItem> {
  val values = arrayListOf<RouletteItem>()
  generateSequence(valueRange.start) { it + step }
    .takeWhile { it <= valueRange.endInclusive }
    .forEachIndexed { index, item ->
      values.add(RouletteItem(item.roundTo(2), 1f, index))
    }
  return values
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ChipBox(value: Float, onValueChange: (Float) -> Unit) {
  Chip(onClick = { onValueChange.invoke(value) }) {
    Text(text = value.toString() + "x")
  }
}

@Preview
@Composable
fun PlaybackSpeedComponentPreview() {
  VoiceTheme {
    PlaybackSpeedComponent(inputValue = 1f) {}
  }
}
