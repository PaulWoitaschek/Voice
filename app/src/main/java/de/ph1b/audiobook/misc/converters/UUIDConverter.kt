package de.ph1b.audiobook.misc.converters

import com.f2prateek.rx.preferences2.Preference
import java.util.UUID

class UUIDConverter : Preference.Converter<UUID> {

  override fun deserialize(serialized: String): UUID = UUID.fromString(serialized)

  override fun serialize(value: UUID): String = value.toString()
}
