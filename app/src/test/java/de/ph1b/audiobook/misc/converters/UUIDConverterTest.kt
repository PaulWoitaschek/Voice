package de.ph1b.audiobook.misc.converters

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.UUID

class UUIDConverterTest {

  private val converter = UUIDConverter()

  @Test
  fun test() {
    val id = UUID.randomUUID()
    val serialized = converter.serialize(id)
    val deSerialized = converter.deserialize(serialized)
    assertThat(deSerialized).isEqualTo(id)
  }
}
