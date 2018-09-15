package de.ph1b.audiobook.data.repo.internals

import androidx.collection.SparseArrayCompat
import androidx.room.TypeConverter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import de.ph1b.audiobook.data.Book
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.io.File
import java.util.UUID

class Converters : KoinComponent {

  private val moshi: Moshi by inject()

  private val sparseStringArrayAdapter: JsonAdapter<SparseArrayCompat<String>>

  init {
    val stringAdapter = moshi.adapter(String::class.java)
    sparseStringArrayAdapter = SparseArrayAdapter(stringAdapter)
  }

  @TypeConverter
  fun fromFile(file: File): String = file.absolutePath

  @TypeConverter
  fun toFile(path: String) = File(path)

  @TypeConverter
  fun fromSparseArrayCompat(array: SparseArrayCompat<String>): String {
    return sparseStringArrayAdapter.toJson(array)
  }

  @TypeConverter
  fun toSparseArrayCompat(json: String): SparseArrayCompat<String> {
    return sparseStringArrayAdapter.fromJson(json)!!
  }

  @TypeConverter
  fun fromBookType(type: Book.Type): String = type.name

  @TypeConverter
  fun toBookType(name: String): Book.Type = Book.Type.valueOf(name)

  @TypeConverter
  fun fromUUID(uuid: UUID): String = uuid.toString()

  @TypeConverter
  fun toUUID(string: String): UUID = UUID.fromString(string)
}
