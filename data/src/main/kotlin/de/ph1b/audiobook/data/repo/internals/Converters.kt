package de.ph1b.audiobook.data.repo.internals

import android.net.Uri
import androidx.room.TypeConverter
import de.ph1b.audiobook.data.Book
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
  fun toUri(string: String): Uri = Uri.parse(string)
}
