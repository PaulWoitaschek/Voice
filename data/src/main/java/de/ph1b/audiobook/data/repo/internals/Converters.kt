package de.ph1b.audiobook.data.repo.internals

import android.arch.persistence.room.TypeConverter
import android.support.v4.util.SparseArrayCompat
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import de.ph1b.audiobook.data.di.DataInjector
import java.io.File
import javax.inject.Inject

class Converters {

  @Inject
  lateinit var moshi: Moshi
  private val sparseStringArrayAdapter: JsonAdapter<SparseArrayCompat<String>>

  init {
    DataInjector.component.inject(this)
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
}
