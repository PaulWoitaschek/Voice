package voice.core.snapshot

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import com.luciad.imageio.webp.CompressionType
import com.luciad.imageio.webp.WebPImageWriterSpi
import com.luciad.imageio.webp.WebPWriteParam
import java.awt.image.BufferedImage
import java.io.File
import java.util.Locale
import javax.imageio.IIOImage
import javax.imageio.ImageWriteParam
import javax.imageio.stream.FileImageOutputStream
import kotlin.math.abs

internal fun recordReferences(
  tiles: List<Tile>,
  referenceDir: File,
) {
  tiles.groupBy { it.group }.forEach { (group, groupTiles) ->
    val sheet = sheetOf(groupTiles)
    referenceDir.mkdirs()
    sheet.image.writeLosslessWebP(File(referenceDir, "$group.webp"))
    File(referenceDir, "$group.txt").writeText(
      sheet.bounds.entries
        .sortedBy { it.key }
        .joinToString(separator = "") { (name, rect) -> "$name ${rect.left} ${rect.top} ${rect.width()} ${rect.height()}\n" },
    )
  }
}

internal fun verifyReferences(
  tiles: List<Tile>,
  referenceDir: File,
  diffDir: File,
): List<String> {
  val problems = mutableListOf<String>()
  val groups = tiles.groupBy { it.group }
  groups.forEach { (group, groupTiles) ->
    val index = File(referenceDir, "$group.txt")
    val sheetFile = File(referenceDir, "$group.webp")
    if (!index.exists() || !sheetFile.exists()) {
      problems += "$group: no reference sheet"
      return@forEach
    }
    val bounds = readIndex(index)
    val sheet = sheetFile.readBytes().let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    groupTiles.forEach { tile ->
      val rect = bounds[tile.name]
      val actual = tile.image
      if (rect == null) {
        problems += "${tile.name}: new"
        return@forEach
      }
      val reference = Bitmap.createBitmap(sheet, rect.left, rect.top, rect.width(), rect.height())
      if (reference.width != actual.width || reference.height != actual.height) {
        problems += "${tile.name}: was ${reference.width}x${reference.height}, is ${actual.width}x${actual.height}"
        writeComparison(reference, actual, diff = null, File(diffDir, "${tile.name}.png"))
        return@forEach
      }
      val diff = differences(reference, actual)
      if (diff != null) {
        problems += "${tile.name}: ${diff.count} pixels differ"
        writeComparison(reference, actual, diff.image, File(diffDir, "${tile.name}.png"))
      }
    }
    (bounds.keys - groupTiles.map { it.name }.toSet()).forEach { problems += "$it: removed" }
  }
  referenceDir.listFiles { file -> file.extension == "webp" && file.nameWithoutExtension !in groups }
    ?.forEach { problems += "${it.nameWithoutExtension}: sheet without previews" }
  return problems
}

// libwebp at its highest effort, as Android's encoder makes the files about 60% larger
private fun Bitmap.writeLosslessWebP(file: File) {
  val pixels = IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }
  val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).apply { setRGB(0, 0, width, height, pixels, 0, width) }
  val param = WebPWriteParam(Locale.ROOT).apply {
    compressionMode = ImageWriteParam.MODE_EXPLICIT
    compressionType = CompressionType.Lossless
    compressionQuality = 1F
    method = 6
  }
  val writer = WebPImageWriterSpi().createWriterInstance(null)
  try {
    FileImageOutputStream(file).use {
      writer.output = it
      writer.write(null, IIOImage(image, null, null), param)
    }
  } finally {
    writer.dispose()
  }
}

private fun readIndex(index: File): Map<String, Rect> {
  return index.readLines().filter { it.isNotBlank() }.associate { line ->
    val parts = line.split(' ')
    val (left, top, width, height) = parts.takeLast(4).map { it.toInt() }
    parts.dropLast(4).joinToString(" ") to Rect(left, top, left + width, top + height)
  }
}

private class Differences(
  val count: Int,
  val image: Bitmap,
)

private fun differences(
  reference: Bitmap,
  actual: Bitmap,
): Differences? {
  val width = reference.width
  val height = reference.height
  val expected = IntArray(width * height).also { reference.getPixels(it, 0, width, 0, 0, width, height) }
  val pixels = IntArray(width * height).also { actual.getPixels(it, 0, width, 0, 0, width, height) }
  var count = 0
  for (index in pixels.indices) {
    if (differs(expected[index], pixels[index])) {
      count++
      pixels[index] = Color.RED
    } else {
      // faded, so the red stands out
      pixels[index] = Color.argb(255, fade(Color.red(pixels[index])), fade(Color.green(pixels[index])), fade(Color.blue(pixels[index])))
    }
  }
  if (count == 0) return null
  return Differences(count, Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888))
}

private fun differs(
  expected: Int,
  actual: Int,
): Boolean {
  return abs(Color.red(expected) - Color.red(actual)) > CHANNEL_TOLERANCE ||
    abs(Color.green(expected) - Color.green(actual)) > CHANNEL_TOLERANCE ||
    abs(Color.blue(expected) - Color.blue(actual)) > CHANNEL_TOLERANCE
}

private fun fade(channel: Int): Int = 255 - (255 - channel) / 4

private fun writeComparison(
  reference: Bitmap,
  actual: Bitmap,
  diff: Bitmap?,
  file: File,
) {
  val panels = listOfNotNull(reference, actual, diff)
  val image = Bitmap.createBitmap(
    panels.sumOf { it.width } + GAP * (panels.size + 1),
    panels.maxOf { it.height } + GAP * 2,
    Bitmap.Config.ARGB_8888,
  )
  val canvas = Canvas(image)
  canvas.drawColor(Color.DKGRAY)
  var x = GAP
  panels.forEach {
    canvas.drawBitmap(it, x.toFloat(), GAP.toFloat(), null)
    x += it.width + GAP
  }
  file.parentFile?.mkdirs()
  file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
}

// rendering on Linux and macOS differs by a few levels in places
private const val CHANNEL_TOLERANCE = 6
private const val GAP = 16
