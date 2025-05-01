package de.paulwoitaschek.chapterreader.id3

import de.paulwoitaschek.chapterreader.Chapter
import de.paulwoitaschek.chapterreader.misc.skipBytes
import voice.logging.core.Logger
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import javax.inject.Inject

private const val HEADER_LENGTH = 10
private const val ID3_LENGTH = 3
private const val FRAME_ID_LENGTH = 4
private const val ENCODING_UTF16_WITH_BOM: Byte = 1
private const val ENCODING_UTF16_WITHOUT_BOM: Byte = 2
private const val ENCODING_UTF8: Byte = 3
private const val FRAME_ID_CHAPTER = "CHAP"
private const val FRAME_ID_TITLE = "TIT2"

/**
 * Reads chapter marks from an mp3 file.
 *
 * Original source from [AntennaPod](https://github.com/AntennaPod/AntennaPod/tree/develop/core/src/main/java/de/danoeh/antennapod/core/util/id3reader)
 * Licensed under Apache 2.0
 */
internal class ID3ChapterReader @Inject constructor() {

  private val chapters = ArrayList<ChapterMetaData>()
  private var readerPosition: Int = 0
  private var currentChapter: ChapterMetaData? = null

  fun read(file: File) = file.inputStream().use {
    readInputStream(it)
  }

  @Synchronized
  private fun readInputStream(input: InputStream): List<Chapter> {
    chapters.clear()

    try {
      readerPosition = 0
      val tagHeaderSource = readBytes(input, HEADER_LENGTH)
      val tagHeader = createTagHeader(tagHeaderSource)
      if (tagHeader != null) {
        while (readerPosition < tagHeader.size) {
          val frameHeader = createFrameHeader(readBytes(input, HEADER_LENGTH), tagHeader)
          if (checkForNullString(frameHeader.id)) {
            break
          } else {
            if (!onStartFrameHeader(frameHeader, input)) {
              if (frameHeader.size + readerPosition > tagHeader.size) {
                break
              } else {
                skipBytes(input, frameHeader.size)
              }
            }
          }
        }
        onEndTag()
      }
    } catch (exception: ID3ReaderException) {
      Logger.w(exception, "Error in readInputStream")
    } catch (exception: IOException) {
      Logger.w(exception, "Error in readInputStream")
    }

    return chapters.mapNotNull {
      val title = it.title ?: return@mapNotNull null
      Chapter(it.start, title)
    }
  }

  /** Returns true if string only contains null-bytes.  */
  private fun checkForNullString(s: String): Boolean {
    return if (s.isNotEmpty()) {
      var i = 0
      if (s[i].code == 0) {
        i = 1
        while (i < s.length) {
          if (s[i].code != 0) {
            return false
          }
          i++
        }
        return true
      }
      false
    } else {
      true
    }
  }

  /**
   * Read a certain number of bytes from the given input stream. This method
   * changes the readerPosition-attribute.
   */
  @Throws(IOException::class, ID3ReaderException::class)
  private fun readBytes(
    input: InputStream,
    number: Int,
  ): CharArray {
    val header = CharArray(number)
    for (i in 0 until number) {
      val b = input.read()
      readerPosition++
      if (b != -1) {
        header[i] = b.toChar()
      } else {
        throw ID3ReaderException("Unexpected end of stream")
      }
    }
    return header
  }

  /**
   * Skip a certain number of bytes on the given input stream. This method
   * changes the readerPosition-attribute.
   */
  @Throws(IOException::class)
  private fun skipBytes(
    input: InputStream,
    number: Int,
  ) {
    val numberCorrected = number.coerceAtLeast(1)
    input.skipBytes(numberCorrected)
    readerPosition += numberCorrected
  }

  @Throws(ID3ReaderException::class)
  private fun createTagHeader(source: CharArray): Header.TagHeader? {
    val hasTag = source[0].code == 0x49 && source[1].code == 0x44 && source[2].code == 0x33
    if (source.size != HEADER_LENGTH) {
      throw ID3ReaderException("Length of header must be $HEADER_LENGTH")
    }
    return if (hasTag) {
      val id = String(source, 0, ID3_LENGTH)
      val version = (source[3].code shl 8 or source[4].code).toChar()
      var size =
        source[6].code shl 24 or (source[7].code shl 16) or (source[8].code shl 8) or source[9].code
      size = unsynchsafe(size)
      Header.TagHeader(id, size, version)
    } else {
      null
    }
  }

  @Throws(ID3ReaderException::class)
  private fun createFrameHeader(
    source: CharArray,
    tagHeader: Header.TagHeader,
  ): Header.FrameHeader {
    if (source.size != HEADER_LENGTH) {
      throw ID3ReaderException("Length of header must be $HEADER_LENGTH")
    }
    val id = String(source, 0, FRAME_ID_LENGTH)
    var size = source[4].code shl 24 or (source[5].code shl 16) or (source[6].code shl 8) or source[7].code
    if (tagHeader.version.code >= 0x0400) {
      size = unsynchsafe(size)
    }
    return Header.FrameHeader(id, size)
  }

  private fun unsynchsafe(input: Int): Int {
    var out = 0
    var mask = 0x7F000000

    while (mask != 0) {
      out = out shr 1
      out = out or (input and mask)
      mask = mask shr 8
    }

    return out
  }

  @Throws(IOException::class, ID3ReaderException::class)
  private fun readString(
    buffer: StringBuffer,
    input: InputStream,
    max: Int,
  ): Int = if (max > 0) {
    val encoding = readBytes(input, 1)
    val maxCorrected = max - 1

    if (encoding[0].code.toByte() == ENCODING_UTF16_WITH_BOM || encoding[0].code.toByte() == ENCODING_UTF16_WITHOUT_BOM) {
      readUnicodeString(buffer, input, maxCorrected, Charset.forName("UTF-16")) + 1 // take encoding byte into account
    } else if (encoding[0].code.toByte() == ENCODING_UTF8) {
      readUnicodeString(buffer, input, maxCorrected, Charset.forName("UTF-8")) + 1 // take encoding byte into account
    } else {
      readISOString(buffer, input, maxCorrected) + 1 // take encoding byte into account
    }
  } else {
    buffer.append("")
    0
  }

  @Throws(IOException::class, ID3ReaderException::class)
  private fun readISOString(
    buffer: StringBuffer,
    input: InputStream,
    max: Int,
  ): Int {
    var bytesRead = 0
    while (true) {
      bytesRead++
      if (bytesRead > max) {
        break
      }
      val c = input.read().toChar()
      if (c.code <= 0) {
        break
      } else {
        buffer.append(c)
      }
    }
    return bytesRead
  }

  @Throws(IOException::class, ID3ReaderException::class)
  private fun readUnicodeString(
    strBuffer: StringBuffer,
    input: InputStream,
    max: Int,
    charset: Charset,
  ): Int {
    val buffer = ByteArray(max)
    var c: Int
    var cZero = -1
    var i = 0
    while (i < max) {
      c = input.read()
      if (c == -1) {
        break
      } else if (c == 0) {
        cZero = if (cZero == 0) {
          // termination character found
          break
        } else {
          0
        }
      } else {
        buffer[i] = c.toByte()
        cZero = -1
      }
      i++
    }
    strBuffer.append(charset.newDecoder().decode(ByteBuffer.wrap(buffer)).toString())
    return i
  }

  @Throws(IOException::class, ID3ReaderException::class)
  private fun onStartFrameHeader(
    header: Header.FrameHeader,
    input: InputStream,
  ): Boolean {
    when (header.id) {
      FRAME_ID_CHAPTER -> {
        currentChapter?.let {
          if (!hasId3Chapter(it)) {
            chapters.add(it)
            currentChapter = null
          }
        }
        val elementId = StringBuffer()
        readISOString(elementId, input, Integer.MAX_VALUE)
        val startTimeSource = readBytes(input, 4)
        val startTime =
          (startTimeSource[0].code shl 24 or (startTimeSource[1].code shl 16) or (startTimeSource[2].code shl 8) or startTimeSource[3].code)
        currentChapter = ChapterMetaData(elementId.toString(), startTime.toLong(), null)
        skipBytes(input, 12)
        return true
      }
      FRAME_ID_TITLE -> if (currentChapter != null && currentChapter!!.title == null) {
        val title = StringBuffer()
        readString(title, input, header.size)
        currentChapter!!.title = title.toString()
        return true
      }
    }

    return false
  }

  private fun hasId3Chapter(chapter: ChapterMetaData) = chapters.any { it.id3ID == chapter.id3ID }

  private fun onEndTag() {
    currentChapter?.let {
      if (!hasId3Chapter(it)) {
        chapters.add(it)
      }
    }
  }
}
