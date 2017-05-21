package de.ph1b.audiobook.misc

import android.util.SparseArray
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

/**
 * JsonAdapter for a sparse array
 *
 * @author Paul Woitaschek
 */
class SparseArrayAdapter<T>(private val adapter: JsonAdapter<T>) : JsonAdapter<SparseArray<T>>() {

  override fun toJson(writer: JsonWriter, sparseArray: SparseArray<T>?) {
    writer.writeObject {
      sparseArray?.forEachIndexed { _, key, value ->
        writer.name(key.toString())
        adapter.toJson(writer, value)
      }
    }
  }

  override fun fromJson(reader: JsonReader): SparseArray<T> {
    val sparseArray = SparseArray<T>()

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