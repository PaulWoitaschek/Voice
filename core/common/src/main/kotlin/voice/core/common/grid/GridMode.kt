package voice.core.common.grid

import kotlinx.serialization.Serializable

@Serializable
enum class GridMode {
  LIST,
  GRID,
  FOLLOW_DEVICE,
}
