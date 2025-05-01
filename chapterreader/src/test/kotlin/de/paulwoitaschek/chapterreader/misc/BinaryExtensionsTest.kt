package de.paulwoitaschek.chapterreader.misc

import de.paulwoitaschek.chapterreader.ogg.DatatypeConverter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.InputStream

class BinaryStreamExtensionsTest {

  private val binaryData = DatatypeConverter.parseHexBinary("6c6f6cffd7904df58a9e4e0c2582390eb2")
  private lateinit var stream: InputStream

  @Before
  fun setUp() {
    stream = ByteArrayInputStream(binaryData)
  }

  @Test
  fun startsWithExtension() {
    binaryData.startsWith("lol".toByteArray()) shouldBe true
    binaryData.startsWith("test".toByteArray()) shouldBe false
  }

  @Test
  fun toUIntExtension() {
    2.toByte().toUInt() shouldBe 2
    (-2).toByte().toUInt() shouldBe 254
    254.toByte().toUInt() shouldBe 254
  }

  @Test
  fun toUlongExtension() {
    2.toByte().toULong() shouldBe 2L
    (-2).toByte().toULong() shouldBe 254L
    254.toByte().toULong() shouldBe 254L
  }

  @Test
  fun readUint8() {
    stream.readUInt8() shouldBe 0x6c
    stream.readUInt8() shouldBe 0x6f
    stream.readUInt8() shouldBe 0x6c
    stream.readUInt8() shouldBe 0xff
  }

  @Test
  fun readLeUInt32() {
    stream.readLeUInt32() shouldBe 0xff6c6f6cL
    stream.readLeUInt32() shouldBe 0xf54d90d7L
  }

  @Test
  fun readLeInt32() {
    stream.readLeInt32() shouldBe -9670804
    stream.readLeInt32() shouldBe -179466025
  }

  @Test
  fun readLeInt64() {
    stream.readLeInt64() shouldBe -770800703832821908L
    stream.readLeInt64() shouldBe 1024993485835378314L
  }

  @Test
  fun readBytes() {
    stream.readAmountOfBytes(4) shouldBe DatatypeConverter.parseHexBinary("6c6f6cff")
    stream.readAmountOfBytes(3) shouldBe DatatypeConverter.parseHexBinary("d7904d")
  }

  @Test
  fun skipBytes() {
    stream.skipBytes(10)
    stream.readUInt8() shouldBe 0x4e
  }

  @Test
  fun endOfBinaryStreamException() {
    shouldThrow<EOFException> {
      stream.readAmountOfBytes(100)
    }
  }
}
