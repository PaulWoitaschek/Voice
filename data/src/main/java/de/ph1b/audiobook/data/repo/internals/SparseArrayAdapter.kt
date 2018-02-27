package de.ph1b.audiobook.data.repo.internals

import android.support.v4.util.SparseArrayCompat
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import de.ph1b.audiobook.common.sparseArray.forEachIndexed

/**
 * JsonAdapter for a sparse array
 */
class SparseArrayAdapter<T>(private val adapter: JsonAdapter<T>) :
  JsonAdapter<SparseArrayCompat<T>>() {

  override fun toJson(writer: JsonWriter, sparseArray: SparseArrayCompat<T>?) {
    writer.writeObject {
      sparseArray?.forEachIndexed { _, key, value ->
        writer.name(key.toString())
        adapter.toJson(writer, value)
      }
    }
  }

  override fun fromJson(reader: JsonReader): SparseArrayCompat<T> {
    val sparseArray = SparseArrayCompat<T>()

    reader.readObject {
      val key = it.toInt()
      val value = adapter.fromJson(reader)
      sparseArray.put(key, value)
      true
    }

    return sparseArray
  }

  /** iterates over the values in an object and executes the handle function. The handle function must return true if it consumed the object */
  private inline fun JsonReader.readObject(handle: (name: String) -> Boolean) {
    beginObject()
    while (hasNext()) {
      if (peek() == JsonReader.Token.NULL) {
        skipValue()
        continue
      }
      val name = nextName()
      if (!handle(name)) skipValue()
    }
    endObject()
  }

  private inline fun JsonWriter.writeObject(write: JsonWriter.() -> Unit) {
    beginObject()
    write(this)
    endObject()
  }
}
