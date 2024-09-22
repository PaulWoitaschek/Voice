package voice.playback.misc

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class Decibel(val value: Float) {

  val milliBel: Int get() = (value * 100).toInt()

  operator fun compareTo(other: Decibel) = value.compareTo(other.value)

  companion object {
    val Zero = Decibel(0F)
  }
}
