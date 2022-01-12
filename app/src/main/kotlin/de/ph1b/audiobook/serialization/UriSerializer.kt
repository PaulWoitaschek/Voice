package de.ph1b.audiobook.serialization

import android.net.Uri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object UriSerializer : KSerializer<Uri> {

  override val descriptor = PrimitiveSerialDescriptor("Uri", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): Uri = Uri.parse(decoder.decodeString())

  override fun serialize(encoder: Encoder, value: Uri) {
    encoder.encodeString(value.toString())
  }
}
