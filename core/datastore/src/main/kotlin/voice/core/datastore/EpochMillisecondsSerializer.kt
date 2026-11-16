package voice.core.datastore

import kotlin.time.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// stores written before instants kept plain numbers, so the files stay readable
object EpochMillisecondsSerializer : KSerializer<Instant> {

  override val descriptor =
    PrimitiveSerialDescriptor("voice.Instant.EpochMilliseconds", PrimitiveKind.LONG)

  override fun deserialize(decoder: Decoder): Instant =
    Instant.fromEpochMilliseconds(decoder.decodeLong())

  override fun serialize(
    encoder: Encoder,
    value: Instant,
  ) {
    encoder.encodeLong(value.toEpochMilliseconds())
  }
}
