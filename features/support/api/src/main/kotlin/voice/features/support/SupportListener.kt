package voice.features.support

interface SupportListener {
  fun close()

  fun openSupport()

  companion object {
    fun noop() = object : SupportListener {
      override fun close() {}
      override fun openSupport() {}
    }
  }
}
