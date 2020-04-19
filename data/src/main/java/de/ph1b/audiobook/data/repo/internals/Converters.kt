package de.ph1b.audiobook.data.repo.internals

import androidx.room.TypeConverter
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.MarkData
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.threeten.bp.Instant
import java.io.File
import java.util.UUID

class Converters {

  private val json = Json(JsonConfiguration.Stable)
  private val markDataListSerializer = MarkData.serializer().list

  @TypeConverter
  fun fromMarks(data: List<MarkData>): String = json.stringify(markDataListSerializer, data)

  @TypeConverter
  fun toMarks(string: String): List<MarkData> = json.parse(markDataListSerializer, string)

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
}
