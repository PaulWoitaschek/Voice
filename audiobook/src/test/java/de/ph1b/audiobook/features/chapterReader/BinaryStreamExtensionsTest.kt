package de.ph1b.audiobook.features.chapterReader

import org.assertj.core.api.Assertions.*
import org.bouncycastle.util.encoders.Hex
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.EOFException

@RunWith(RobolectricTestRunner::class)
class BinaryStreamExtensionsTest {
  private val binaryData = Hex.decode("6c6f6cffd7904df58a9e4e0c2582390eb2")
  private var stream = ByteArrayInputStream(ByteArray(0))

  @Before
  fun setUp() {
    stream = ByteArrayInputStream(binaryData)
  }

  @Test
  fun startsWithExtension() {
    assertThat(binaryData.startsWith("lol".toByteArray())).isTrue()
    assertThat(binaryData.startsWith("test".toByteArray())).isFalse()
  }

  @Test
  fun toUIntExtension() {
    assertThat(2.toByte().toUInt()).isEqualTo(2)
    assertThat((-2).toByte().toUInt()).isEqualTo(254)
    assertThat(254.toByte().toUInt()).isEqualTo(254)
  }

  @Test
  fun toUlongExtension() {
    assertThat(2.toByte().toULong()).isEqualTo(2L)
    assertThat((-2).toByte().toULong()).isEqualTo(254L)
    assertThat(254.toByte().toULong()).isEqualTo(254L)
  }

  @Test
  fun readUint8() {
    assertThat(stream.readUInt8()).isEqualTo(0x6c)
    assertThat(stream.readUInt8()).isEqualTo(0x6f)
    assertThat(stream.readUInt8()).isEqualTo(0x6c)
    assertThat(stream.readUInt8()).isEqualTo(0xff)
  }

  @Test
  fun readLeUInt32() {
    assertThat(stream.readLeUInt32()).isEqualTo(0xff6c6f6cL)
    assertThat(stream.readLeUInt32()).isEqualTo(0xf54d90d7L)
  }

  @Test
  fun readLeInt32() {
    assertThat(stream.readLeInt32()).isEqualTo(-9670804)
    assertThat(stream.readLeInt32()).isEqualTo(-179466025)
  }

  @Test
  fun readLeInt64() {
    assertThat(stream.readLeInt64()).isEqualTo(-770800703832821908L)
    assertThat(stream.readLeInt64()).isEqualTo(1024993485835378314L)
  }

  @Test
  fun readBytes() {
    assertThat(stream.readBytes(4)).isEqualTo(Hex.decode("6c6f6cff"))
    assertThat(stream.readBytes(3)).isEqualTo(Hex.decode("d7904d"))
  }

  @Test
  fun skipBytes() {
    stream.skipBytes(10)
    assertThat(stream.readUInt8()).isEqualTo(0x4e)
  }

  @Test
  fun endOfBinaryStreamException() {
    stream.readBytes(10)
    try {
      stream.readBytes(100)
      failBecauseExceptionWasNotThrown(EOFException::class.java)
    } catch (_: EOFException) {}
  }
}
