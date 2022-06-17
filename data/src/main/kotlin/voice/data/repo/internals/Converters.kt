package voice.data.repo.internals

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import voice.data.Book
import voice.data.Bookmark
import voice.data.Chapter
import voice.data.MarkData
import voice.data.legacy.LegacyBookType
import voice.data.legacy.LegacyMarkData
import java.io.File
import java.time.Instant
import java.util.UUID

class Converters {

  private val json = Json { allowStructuredMapKeys = true }
  private val markDataListSerializer = ListSerializer(MarkData.serializer())

  @TypeConverter
  fun fromMarks(data: List<MarkData>): String = json.encodeToString(markDataListSerializer, data)

  @TypeConverter
  fun toMarks(string: String): List<MarkData> = json.decodeFromString(markDataListSerializer, string)

  @TypeConverter
  fun fromLegacyMarks(data: List<LegacyMarkData>): String = json.encodeToString(ListSerializer(LegacyMarkData.serializer()), data)

  @TypeConverter
  fun toLegacyMarks(string: String): List<LegacyMarkData> = json.decodeFromString(ListSerializer(LegacyMarkData.serializer()), string)

  @TypeConverter
  fun fromFile(file: File): String = file.absolutePath

  @TypeConverter
  fun toFile(path: String) = File(path)

  @TypeConverter
  fun fromBookType(type: LegacyBookType): String = type.name

  @TypeConverter
  fun toBookType(name: String): LegacyBookType = LegacyBookType.valueOf(name)

  @TypeConverter
  fun fromUUID(uuid: UUID): String = uuid.toString()

  @TypeConverter
  fun toUUID(string: String): UUID = UUID.fromString(string)

  @TypeConverter
  fun fromInstant(instant: Instant): String {
    return instant.toString()
  }

  @TypeConverter
  fun toInstant(string: String): Instant {
    return Instant.parse(string)
  }

  @TypeConverter
  fun fromUri(uri: Uri): String = uri.toString()

  @TypeConverter
  fun toUri(string: String): Uri = string.toUri()

  @TypeConverter
  fun fromChapterList(list: List<Chapter.Id>): String {
    return json.encodeToString(ListSerializer(Chapter.Id.serializer()), list)
  }

  @TypeConverter
  fun toChapterList(string: String): List<Chapter.Id> {
    return json.decodeFromString(ListSerializer(Chapter.Id.serializer()), string)
  }

  @TypeConverter
  fun toBookId(value: String): Book.Id = Book.Id(value)

  @TypeConverter
  fun fromBookId(id: Book.Id): String = id.value

  @TypeConverter
  fun toChapterId(value: String): Chapter.Id = Chapter.Id(value)

  @TypeConverter
  fun fromChapterId(id: Chapter.Id): String = id.value

  @TypeConverter
  fun toBookmarkId(value: String): Bookmark.Id = Bookmark.Id(UUID.fromString(value))

  @TypeConverter
  fun fromBookmarkId(id: Bookmark.Id): String = id.value.toString()
}
