package voice.core.snapshot

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Lays the snapshots out in rows, each labeled with its file name, so all previews of a module can
 * be looked at in one go.
 */
internal fun writeContactSheet(
  snapshots: List<File>,
  output: File,
) {
  val labeled = snapshots.map { file ->
    val image = ImageIO.read(file)
    val scale = minOf(1.0, CELL_HEIGHT.toDouble() / image.height, CELL_WIDTH.toDouble() / image.width)
    Cell(file.nameWithoutExtension, image, (image.width * scale).toInt(), (image.height * scale).toInt())
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
  graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
  graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
  graphics.color = Color(0x77, 0x77, 0x77)
  graphics.fillRect(0, 0, width, height)
  graphics.font = Font(Font.SANS_SERIF, Font.PLAIN, 18)
  var y = GAP
  rows.forEach { row ->
    var x = GAP
    row.forEach { cell ->
      graphics.color = Color.WHITE
      graphics.drawString(cell.label.take(cell.width / 9), x, y + LABEL_HEIGHT - 8)
      graphics.drawImage(cell.image, x, y + LABEL_HEIGHT, cell.width, cell.height, null)
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
  val width: Int,
  val height: Int,
)

private const val CELL_WIDTH = 900
private const val CELL_HEIGHT = 900
private const val SHEET_WIDTH = 3_000
private const val LABEL_HEIGHT = 28
private const val GAP = 16
