package de.ph1b.audiobook.chapterreader.id3

import dagger.Reusable
import de.ph1b.audiobook.common.Logger
import de.ph1b.audiobook.common.skipBytes
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.ArrayList
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
@Reusable internal class ID3ChapterReader @Inject constructor(private val logger: Logger) {

  private val chapters = ArrayList<ChapterMetaData>()
  private var readerPosition: Int = 0
  private var currentChapter: ChapterMetaData? = null

  fun read(file: File): Map<Int, String> = file.inputStream().use {
    readInputStream(it)
  }

  @Synchronized private fun readInputStream(input: InputStream): Map<Int, String> {
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
      logger.e(exception)
    } catch (exception: IOException) {
      logger.e(exception)
    }

    val array = HashMap<Int, String>(chapters.size)
    chapters.forEach { (_, start, title) ->
      if (title != null) array.put(start, title)
    }
    return array
  }

  /** Returns true if string only contains null-bytes.  */
  private fun checkForNullString(s: String): Boolean {
    if (!s.isEmpty()) {
      var i = 0
      if (s[i].toInt() == 0) {
        i = 1
        while (i < s.length) {
          if (s[i].toInt() != 0) {
            return false
          }
          i++
        }
        return true
      }
      return false
    } else return true
  }

  /**
   * Read a certain number of bytes from the given input stream. This method
   * changes the readerPosition-attribute.
   */
  @Throws(IOException::class, ID3ReaderException::class)
  private fun readBytes(input: InputStream, number: Int): CharArray {
    val header = CharArray(number)
    for (i in 0..number - 1) {
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
  private fun skipBytes(input: InputStream, number: Int) {
    val numberCorrected = number.coerceAtLeast(1)
    input.skipBytes(numberCorrected)
    readerPosition += numberCorrected
  }

  @Throws(ID3ReaderException::class)
  private fun createTagHeader(source: CharArray): Header.TagHeader? {
    val hasTag = source[0].toInt() == 0x49 && source[1].toInt() == 0x44 && source[2].toInt() == 0x33
    if (source.size != HEADER_LENGTH) {
      throw ID3ReaderException("Length of header must be " + HEADER_LENGTH)
    }
    if (hasTag) {
      val id = String(source, 0, ID3_LENGTH)
      val version = (source[3].toInt() shl 8 or source[4].toInt()).toChar()
      var size = source[6].toInt() shl 24 or (source[7].toInt() shl 16) or (source[8].toInt() shl 8) or source[9].toInt()
      size = unsynchsafe(size)
      return Header.TagHeader(id, size, version)
    } else {
      return null
    }
  }

  @Throws(ID3ReaderException::class)
  private fun createFrameHeader(source: CharArray, tagHeader: Header.TagHeader): Header.FrameHeader {
    if (source.size != HEADER_LENGTH) {
      throw ID3ReaderException("Length of header must be " + HEADER_LENGTH)
    }
    val id = String(source, 0, FRAME_ID_LENGTH)
    var size = source[4].toInt() shl 24 or (source[5].toInt() shl 16) or (source[6].toInt() shl 8) or source[7].toInt()
    if (tagHeader.version.toInt() >= 0x0400) {
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
  private fun readString(buffer: StringBuffer, input: InputStream, max: Int): Int {
    if (max > 0) {
      val encoding = readBytes(input, 1)
      val maxCorrected = max - 1

      if (encoding[0].toByte() == ENCODING_UTF16_WITH_BOM || encoding[0].toByte() == ENCODING_UTF16_WITHOUT_BOM) {
        return readUnicodeString(buffer, input, maxCorrected, Charset.forName("UTF-16")) + 1 // take encoding byte into account
      } else if (encoding[0].toByte() == ENCODING_UTF8) {
        return readUnicodeString(buffer, input, maxCorrected, Charset.forName("UTF-8")) + 1 // take encoding byte into account
      } else {
        return readISOString(buffer, input, maxCorrected) + 1 // take encoding byte into account
      }
    } else {
      buffer.append("")
      return 0
    }
  }

  @Throws(IOException::class, ID3ReaderException::class)
  private fun readISOString(buffer: StringBuffer, input: InputStream, max: Int): Int {
    var bytesRead = 0
    while (true) {
      bytesRead++
      if (bytesRead > max) {
        break
      }
      val c = input.read().toChar()
      if (c.toInt() <= 0) {
        break
      } else {
        buffer.append(c)
      }
    }
    return bytesRead
  }

  @Throws(IOException::class, ID3ReaderException::class)
  private fun readUnicodeString(strBuffer: StringBuffer, input: InputStream, max: Int, charset: Charset): Int {
    val buffer = ByteArray(max)
    var c: Int
    var cZero = -1
    var i = 0
    while (i < max) {
      c = input.read()
      if (c == -1) {
        break
      } else if (c == 0) {
        if (cZero == 0) {
          // termination character found
          break
        } else {
          cZero = 0
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
  private fun onStartFrameHeader(header: Header.FrameHeader, input: InputStream): Boolean {
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
        val startTime = (startTimeSource[0].toInt() shl 24 or (startTimeSource[1].toInt() shl 16) or (startTimeSource[2].toInt() shl 8) or startTimeSource[3].toInt())
        currentChapter = ChapterMetaData(elementId.toString(), startTime, null)
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
