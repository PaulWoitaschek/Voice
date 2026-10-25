package voice.core.snapshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect

internal class Tile(
  val name: String,
  val group: String,
  val image: Bitmap,
)

internal class Sheet(
  val image: Bitmap,
  val bounds: Map<String, Rect>,
)

internal fun sheetOf(tiles: List<Tile>): Sheet {
  val rows = mutableListOf<MutableList<Tile>>()
  tiles.forEach { tile ->
    val row = rows.lastOrNull()
    if (row == null || row.sumOf { it.cellWidth + GAP } + tile.cellWidth > SHEET_WIDTH) {
      rows += mutableListOf(tile)
    } else {
      row += tile
    }
  }
  val width = rows.maxOf { row -> row.sumOf { it.cellWidth + GAP } } + GAP
  val height = rows.sumOf { row -> row.maxOf { it.image.height } + LABEL_HEIGHT + GAP } + GAP
  val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(image)
  canvas.drawColor(SHEET_BACKGROUND)
  val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = LABEL_COLOR
    textSize = LABEL_TEXT_SIZE
  }
  val bounds = mutableMapOf<String, Rect>()
  var y = GAP
  rows.forEach { row ->
    var x = GAP
    row.forEach { tile ->
      canvas.drawBitmap(tile.image, x.toFloat(), y.toFloat(), null)
      bounds[tile.name] = Rect(x, y, x + tile.image.width, y + tile.image.height)
      // labels are cut from the start, the end tells previews of one file apart
      val label = generateSequence(tile.name) { it.drop(1) }
        .first { it.isEmpty() || labelPaint.measureText(it) <= tile.cellWidth }
      canvas.drawText(label, x.toFloat(), (y + tile.image.height + LABEL_HEIGHT - 8).toFloat(), labelPaint)
      x += tile.cellWidth + GAP
    }
    y += row.maxOf { it.image.height } + LABEL_HEIGHT + GAP
  }
  return Sheet(image, bounds)
}

// narrow previews still get room for a readable label
private val Tile.cellWidth: Int get() = maxOf(image.width, MIN_CELL_WIDTH)

internal val SHEET_BACKGROUND = Color.rgb(0xEC, 0xEF, 0xF1)
private val LABEL_COLOR = Color.rgb(0x37, 0x47, 0x4F)
private const val LABEL_TEXT_SIZE = 14F
private const val SHEET_WIDTH = 2_600
private const val LABEL_HEIGHT = 24
private const val GAP = 16
private const val MIN_CELL_WIDTH = 200
