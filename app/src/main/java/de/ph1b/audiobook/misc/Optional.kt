package de.ph1b.audiobook.misc

@Suppress("Unused")
sealed class Optional<T : Any> {

  data class Present<T : Any>(val value: T) : Optional<T>()

  class Absent<T : Any> private constructor() : Optional<T>() {
    companion object {
      private val instance = Absent<Any>()
      @Suppress("UNCHECKED_CAST") // save because erasure
      operator fun <T : Any> invoke(): Absent<T> = instance as Absent<T>
    }

    override fun toString(): String = "Absent"
  }

  companion object {
    fun <T : Any> of(value: T?): Optional<T> =
      if (value == null) Absent()
      else Present(value)
  }
}
