package voice.core.sleeptimer

interface ShakeDetector {

  /**
   * This function returns once a shake was detected
   */
  suspend fun detect()
}
