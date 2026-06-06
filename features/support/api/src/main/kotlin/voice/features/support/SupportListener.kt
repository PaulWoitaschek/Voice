package voice.features.support

interface SupportListener {
  fun close()

  fun openSupport()

  fun setSupporterBadgeVisible(visible: Boolean)

  companion object {
    fun noop() = object : SupportListener {
      override fun close() {}
      override fun openSupport() {}
      override fun setSupporterBadgeVisible(visible: Boolean) {}
    }
  }
}
