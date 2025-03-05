package voice.app.serialization

import android.net.Uri
import androidx.core.net.toUri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object UriSerializer : KSerializer<Uri> {

  override val descriptor = PrimitiveSerialDescriptor("Uri", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): Uri = decoder.decodeString().toUri()

  override fun serialize(
    encoder: Encoder,
    value: Uri,
  ) {
    encoder.encodeString(value.toString())
  }
}
