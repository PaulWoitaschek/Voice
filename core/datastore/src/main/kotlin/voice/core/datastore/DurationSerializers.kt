package voice.core.datastore

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// stores written before durations kept plain numbers, so the files stay readable
abstract class WholeDurationSerializer(private val unit: DurationUnit) : KSerializer<Duration> {

  override val descriptor =
    PrimitiveSerialDescriptor("voice.Duration.${unit.name}", PrimitiveKind.LONG)

  override fun deserialize(decoder: Decoder): Duration = decoder.decodeLong().toDuration(unit)

  override fun serialize(
    encoder: Encoder,
    value: Duration,
  ) {
    encoder.encodeLong(value.toLong(unit))
  }
}

object DurationInSecondsSerializer : WholeDurationSerializer(DurationUnit.SECONDS)

object DurationInMillisecondsSerializer : WholeDurationSerializer(DurationUnit.MILLISECONDS)

object DurationInHoursSerializer : WholeDurationSerializer(DurationUnit.HOURS)
