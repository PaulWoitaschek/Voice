package de.ph1b.audiobook.playback.events

/**
 * Represents the way focus on audio is gained or lost.
 *
 * @author Paul Woitaschek
 */
enum class AudioFocus {
  GAIN,
  LOSS,
  LOSS_TRANSIENT_CAN_DUCK,
  LOSS_TRANSIENT,
  LOSS_INCOMING_CALL
}