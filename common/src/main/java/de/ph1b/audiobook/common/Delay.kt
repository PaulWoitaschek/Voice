package de.ph1b.audiobook.common

import kotlinx.coroutines.delay
import kotlin.time.Duration

suspend fun delay(duration: Duration) {
  delay(duration.toLongMilliseconds())
}
