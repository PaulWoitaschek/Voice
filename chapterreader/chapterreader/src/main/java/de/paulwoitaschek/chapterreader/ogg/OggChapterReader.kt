package de.paulwoitaschek.chapterreader.ogg

import dagger.Reusable
import de.paulwoitaschek.chapterreader.misc.Logger
import de.paulwoitaschek.chapterreader.misc.readAmountOfBytes
import de.paulwoitaschek.chapterreader.misc.startsWith
import de.paulwoitaschek.chapterreader.ogg.oggReading.OggPageParseException
import de.paulwoitaschek.chapterreader.ogg.oggReading.OggStream
import de.paulwoitaschek.chapterreader.ogg.oggReading.demuxOggStreams
import de.paulwoitaschek.chapterreader.ogg.oggReading.readOggPages
import de.paulwoitaschek.chapterreader.ogg.vorbisComment.*
import java.io.*
import javax.inject.Inject

@Reusable internal class OggChapterReader @Inject constructor(
  private val logger: Logger
) {

  private val OPUS_HEAD_MAGIC = "OpusHead".toByteArray()
  private val OPUS_TAGS_MAGIC = "OpusTags".toByteArray()
  private val VORBIS_HEAD_MAGIC = "${1.toChar()}vorbis".toByteArray()
  private val VORBIS_TAGS_MAGIC = "${3.toChar()}vorbis".toByteArray()

  fun read(file: File) = file.inputStream().use {
    read(it)
  }

  private fun read(inputStream: InputStream): Map<Int, String> {
    try {
      val oggPages = readOggPages(BufferedInputStream(inputStream))
      val streams = demuxOggStreams(oggPages).values

      for (stream in streams) {
        if (stream.peek().startsWith(OPUS_HEAD_MAGIC))
          return readVorbisCommentFromOpusStream(stream).chapters
        if (stream.peek().startsWith(VORBIS_HEAD_MAGIC))
          return readVorbisCommentFromVorbisStream(stream).chapters
      }
    } catch (ex: IOException) {
      logger.e(ex)
    } catch (ex: OggPageParseException) {
      logger.e(ex)
    } catch (ex: OpusStreamParseException) {
      logger.e(ex)
    } catch (ex: VorbisStreamParseException) {
      logger.e(ex)
    } catch (ex: VorbisCommentParseException) {
      logger.e(ex)
    }
    return emptyMap()
  }

  private fun readVorbisCommentFromOpusStream(stream: OggStream): VorbisComment {
    stream.next()  // skip head packet
    if (!stream.hasNext())
      throw OpusStreamParseException("Opus tags packet not present")
    val tagsPacket = stream.next()
    val packetStream = ByteArrayInputStream(tagsPacket)
    val capturePattern = packetStream.readAmountOfBytes(OPUS_TAGS_MAGIC.size)
    if (!(capturePattern contentEquals OPUS_TAGS_MAGIC))
      throw OpusStreamParseException("Invalid opus tags capture pattern")
    return VorbisCommentReader.readComment(packetStream)
  }

  private fun readVorbisCommentFromVorbisStream(stream: OggStream): VorbisComment {
    stream.next()  // skip head packet
    if (!stream.hasNext())
      throw VorbisStreamParseException("Vorbis comment header packet not present")
    val tagsPacket = stream.next()
    val packetStream = ByteArrayInputStream(tagsPacket)
    val capturePattern = packetStream.readAmountOfBytes(VORBIS_TAGS_MAGIC.size)
    if (!(capturePattern contentEquals VORBIS_TAGS_MAGIC))
      throw VorbisStreamParseException("Invalid vorbis comment header capture pattern")
    return VorbisCommentReader.readComment(packetStream)
  }
}
