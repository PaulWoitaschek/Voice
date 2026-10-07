@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.core.ui

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.toPath
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph

/**
 * A [Shape] that renders a [Morph] between two Material shapes at the given [progress].
 */
internal class MorphShape(
  private val morph: Morph,
  private val progress: Float,
) : Shape {

  override fun createOutline(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
  ): Outline {
    val path = morph.toPath(progress = progress)
    path.transform(Matrix().apply { scale(x = size.width, y = size.height) })
    return Outline.Generic(path)
  }
}
