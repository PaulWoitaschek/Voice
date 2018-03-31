package de.ph1b.audiobook.data.repo.internals

import android.arch.persistence.room.TypeConverter
import java.io.File

class Converters {

  @TypeConverter
  fun fromFile(file: File): String = file.absolutePath

  @TypeConverter
  fun toFile(path: String) = File(path)
}
