package voice.core.sleeptimer

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class ManualClock(
  var instant: Instant,
  private val zoneId: ZoneId = ZoneId.of("UTC"),
) : Clock() {

  override fun instant(): Instant {
    return instant
  }

  override fun withZone(zone: ZoneId?): Clock {
    error("Not implemented")
  }

  override fun getZone(): ZoneId {
    return zoneId
  }
}
