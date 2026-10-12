package voice.core.snapshot

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

internal fun writeContactSheet(
  snapshots: List<File>,
  output: File,
) {
  val labeled = snapshots.map { file ->
    // scaled right away, so a module with many previews doesn't hold them all at full size
    val image = ImageIO.read(file)
    val scale = minOf(1.0, CELL_HEIGHT.toDouble() / image.height, CELL_WIDTH.toDouble() / image.width)
    val width = (image.width * scale).toInt().coerceAtLeast(1)
    val height = (image.height * scale).toInt().coerceAtLeast(1)
    val scaled = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    scaled.createGraphics().apply {
      setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
      drawImage(image, 0, 0, width, height, null)
      dispose()
    }
    Cell(file.nameWithoutExtension, scaled)
  }
  val rows = mutableListOf<MutableList<Cell>>()
  labeled.forEach { cell ->
    val row = rows.lastOrNull()
    if (row == null || row.sumOf { it.width + GAP } + cell.width > SHEET_WIDTH) {
      rows += mutableListOf(cell)
    } else {
      row += cell
    }
  }
  val width = rows.maxOf { row -> row.sumOf { it.width + GAP } } + GAP
  val height = rows.sumOf { row -> row.maxOf { it.height } + LABEL_HEIGHT + GAP } + GAP
  val sheet = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
  val graphics = sheet.createGraphics()
  graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
  graphics.color = Color(0x77, 0x77, 0x77)
  graphics.fillRect(0, 0, width, height)
  graphics.font = Font(Font.SANS_SERIF, Font.PLAIN, 18)
  val metrics = graphics.fontMetrics
  var y = GAP
  rows.forEach { row ->
    var x = GAP
    row.forEach { cell ->
      graphics.color = Color.WHITE
      // labels are cut from the start, the end tells previews of one file apart
      val label = generateSequence(cell.label) { it.drop(1) }
        .first { it.isEmpty() || metrics.stringWidth(it) <= cell.width }
      graphics.drawString(label, x, y + LABEL_HEIGHT - 8)
      graphics.drawImage(cell.image, x, y + LABEL_HEIGHT, null)
      x += cell.width + GAP
    }
    y += row.maxOf { it.height } + LABEL_HEIGHT + GAP
  }
  graphics.dispose()
  ImageIO.write(sheet, "png", output)
}

private class Cell(
  val label: String,
  val image: BufferedImage,
) {
  // narrow previews still get room for a readable label
  val width: Int get() = maxOf(image.width, MIN_CELL_WIDTH)
  val height: Int get() = image.height
}

private const val CELL_WIDTH = 900
private const val CELL_HEIGHT = 900
private const val SHEET_WIDTH = 3_000
private const val LABEL_HEIGHT = 28
private const val GAP = 16
private const val MIN_CELL_WIDTH = 240
