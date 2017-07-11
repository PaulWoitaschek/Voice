package de.ph1b.audiobook.features.chapterReader

import android.util.SparseArray
import de.ph1b.audiobook.misc.emptySparseArray
import de.ph1b.audiobook.misc.values
import e
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

private val OPUS_HEAD_MAGIC = "OpusHead".toByteArray()
private val OPUS_TAGS_MAGIC = "OpusTags".toByteArray()

/**
 * Reads chapters from ogg files.
 */
fun readChaptersFromOgg(inputStream: InputStream): SparseArray<String> {
  try {
    val oggPages = readOggPages(BufferedInputStream(inputStream))
    val streams = demuxOggStreams(oggPages).values

    for (stream in streams) {
      if (stream.peek().startsWith(OPUS_HEAD_MAGIC))
        return readVorbisCommentFromOpusStream(stream).chapters
    }
  } catch (ex: IOException) {
    e(ex)
  } catch (ex: OGGPageParseException) {
    e(ex)
  } catch (ex: OpusStreamParseException) {
    e(ex)
  } catch (ex: VorbisCommentParseException) {
    e(ex)
  }
  return emptySparseArray()
}

class OpusStreamParseException(message: String) : Exception(message)

private fun readVorbisCommentFromOpusStream(stream: OggStream): VorbisComment {
  stream.next()  // skip head packet
  if (!stream.hasNext())
    throw OpusStreamParseException("Opus tags packet not present")
  val tagsPacket = stream.next()
  val packetStream = ByteArrayInputStream(tagsPacket)
  val capturePattern = packetStream.readBytes(OPUS_TAGS_MAGIC.size)
  if (!(capturePattern contentEquals OPUS_TAGS_MAGIC))
    throw OpusStreamParseException("Invalid opus tags capture pattern")
  return readVorbisComment(packetStream)
}
