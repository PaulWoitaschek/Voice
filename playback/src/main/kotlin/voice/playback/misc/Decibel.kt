package voice.playback.misc

@JvmInline
value class Decibel(val value: Float) {

  val milliBel: Int get() = (value * 100).toInt()

  operator fun compareTo(other: Decibel) = value.compareTo(other.value)

  companion object {
    val Zero = Decibel(0F)
  }
}
