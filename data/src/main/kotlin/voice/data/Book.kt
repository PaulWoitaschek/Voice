package voice.data

import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.core.net.toUri
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import voice.logging.core.Logger

data class Book(
  val content: BookContent,
  val chapters: List<Chapter>,
) {

  val id: Id = content.id

  val transitionName: String = id.transitionName

  init {
    check(chapters.size == content.chapters.size) {
      "Different chapter count in $this"
    }
    check(chapters.map { it.id } == content.chapters) {
      "Different chapter order in $this"
    }
  }

  val currentChapter: Chapter = chapters[content.currentChapterIndex]
  val previousChapter: Chapter? = chapters.getOrNull(content.currentChapterIndex - 1)
  val nextChapter: Chapter? = chapters.getOrNull(content.currentChapterIndex + 1)

  val nextMark: ChapterMark? = currentChapter.nextMark(content.positionInChapter)
  val currentMark: ChapterMark = currentChapter.markForPosition(content.positionInChapter)

  val position: Long = chapters.takeWhile { it.id != content.currentChapter }
    .sumOf { it.duration } + content.positionInChapter
  val duration: Long = chapters.sumOf { it.duration }

  inline fun update(update: (BookContent) -> BookContent): Book {
    return copy(content = update(content))
  }

  fun progress(): Float {
    val globalPosition = position
    val totalDuration = duration
    val progress = globalPosition.toFloat() / totalDuration.toFloat()
    if (progress < 0F) {
      Logger.w("Couldn't determine progress for book=$this")
    }
    return progress.coerceIn(0F, 1F)
  }

  @Serializable(with = BookIdSerializer::class)
  @Parcelize
  data class Id(val value: String) : Parcelable {

    val transitionName: String get() = value

    constructor(uri: Uri) : this(uri.toString())

    fun toUri(): Uri {
      return value.toUri()
    }
  }

  companion object {
    const val SPEED_MAX = 2.5F
    const val SPEED_MIN = 0.5F
  }
}

object BookIdSerializer : KSerializer<Book.Id> {

  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("bookId", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): Book.Id = Book.Id(decoder.decodeString())

  override fun serialize(encoder: Encoder, value: Book.Id) {
    encoder.encodeString(value.value)
  }
}


private fun Chapter.nextMark(positionInChapterMs: Long): ChapterMark? {
  val markForPosition = markForPosition(positionInChapterMs)
  val marks = chapterMarks
  val index = marks.indexOf(markForPosition)
  return if (index != -1) {
    marks.getOrNull(index + 1)
  } else {
    null
  }
}

fun Bundle.putBookId(key: String, id: Book.Id) {
  putString(key, id.value)
}

fun Bundle.getBookId(key: String): Book.Id? {
  return getString(key)?.let(Book::Id)
}
