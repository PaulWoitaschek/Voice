package de.ph1b.audiobook.data.repo.internals

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.TypeConverter
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.Bookmark2
import de.ph1b.audiobook.data.Chapter2
import de.ph1b.audiobook.data.MarkData
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
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
  fun fromFile(file: File): String = file.absolutePath

  @TypeConverter
  fun toFile(path: String) = File(path)

  @TypeConverter
  fun fromBookType(type: Book.Type): String = type.name

  @TypeConverter
  fun toBookType(name: String): Book.Type = Book.Type.valueOf(name)

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
  fun fromChapterList(list: List<Chapter2.Id>): String {
    return json.encodeToString(ListSerializer(Chapter2.Id.serializer()), list)
  }

  @TypeConverter
  fun toChapterList(string: String): List<Chapter2.Id> {
    return json.decodeFromString(ListSerializer(Chapter2.Id.serializer()), string)
  }

  @TypeConverter
  fun toBookId(value: String): Book2.Id = Book2.Id(value)

  @TypeConverter
  fun fromBookId(id: Book2.Id): String = id.value

  @TypeConverter
  fun toChapterId(value: String): Chapter2.Id = Chapter2.Id(value)

  @TypeConverter
  fun fromChapterId(id: Chapter2.Id): String = id.value

  @TypeConverter
  fun toBookmarkId(value: String): Bookmark2.Id = Bookmark2.Id(UUID.fromString(value))

  @TypeConverter
  fun fromBookmarkId(id: Bookmark2.Id): String = id.value.toString()
}
