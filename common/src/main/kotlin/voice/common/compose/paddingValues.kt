package voice.common.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

operator fun PaddingValues.plus(other: PaddingValues): PaddingValues {
  val self = this
  return object : PaddingValues {

    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp {
      return self.calculateLeftPadding(layoutDirection) + other.calculateLeftPadding(layoutDirection)
    }

    override fun calculateTopPadding(): Dp {
      return self.calculateTopPadding() + other.calculateTopPadding()
    }

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp {
      return self.calculateRightPadding(layoutDirection) + other.calculateRightPadding(layoutDirection)
    }

    override fun calculateBottomPadding(): Dp {
      return self.calculateBottomPadding() + other.calculateBottomPadding()
    }
  }
}
