package voice.app.scanner.matroska

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import org.ebml.io.DataSource
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max

class SafSeekableDataSource(
  contentResolver: ContentResolver,
  uri: Uri,
) : DataSource,
  AutoCloseable {

  private var fileInputStream: FileInputStream
  private val fileChannel: FileChannel
  private val parcelFileDescriptor: ParcelFileDescriptor
  private val length: Long
  private var closed = false

  init {
    try {
      parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
        ?: throw IOException("Cannot open URI as seekable source: $uri")
      val fileDescriptor = parcelFileDescriptor.fileDescriptor
      fileInputStream = FileInputStream(fileDescriptor)
      fileChannel = fileInputStream.channel
      length = fileChannel.size()
    } catch (e: Exception) {
      throw IOException("Cannot open URI as seekable source: $uri", e)
    }
  }

  override fun length(): Long = length

  override fun getFilePointer(): Long {
    if (closed) return -1
    return try {
      fileChannel.position()
    } catch (e: IOException) {
      -1
    }
  }

  override fun isSeekable(): Boolean = !closed

  override fun seek(pos: Long): Long {
    if (closed) return -1
    try {
      fileChannel.position(pos)
      return fileChannel.position()
    } catch (e: IOException) {
      return -1
    }
  }

  override fun readByte(): Byte {
    check(!closed) { "DataSource is closed" }

    val buffer = ByteBuffer.allocate(1)
    try {
      val bytesRead = fileChannel.read(buffer)
      if (bytesRead == -1) {
        throw IOException("End of stream reached")
      }
      return buffer[0]
    } catch (e: IOException) {
      throw RuntimeException("Failed to read byte", e)
    }
  }

  override fun read(buff: ByteBuffer): Int {
    if (closed) return -1
    return try {
      fileChannel.read(buff)
    } catch (e: IOException) {
      -1
    }
  }

  override fun skip(offset: Long): Long {
    if (closed) return 0
    try {
      val currentPos = fileChannel.position()
      val newPos = max(0.0, (currentPos + offset).toDouble()).toLong()
      fileChannel.position(newPos)
      return newPos - currentPos
    } catch (e: IOException) {
      return 0
    }
  }

  override fun close() {
    if (!closed) {
      closed = true
      fileChannel.close()
      fileInputStream.close()
      parcelFileDescriptor.close()
    }
  }
}
