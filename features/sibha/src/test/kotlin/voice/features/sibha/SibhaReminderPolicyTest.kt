package voice.features.sibha

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SibhaReminderPolicyTest {

  @Test
  fun reminderIsDisabledWhenIntervalIsZero() {
    assertFalse(shouldPlaySibhaReminder(count = 10, reminderEvery = 0))
  }

  @Test
  fun reminderDoesNotFireBeforeInterval() {
    assertFalse(shouldPlaySibhaReminder(count = 9, reminderEvery = 10))
  }

  @Test
  fun reminderFiresOnExactMultiples() {
    assertTrue(shouldPlaySibhaReminder(count = 10, reminderEvery = 10))
    assertTrue(shouldPlaySibhaReminder(count = 20, reminderEvery = 10))
  }

  @Test
  fun reminderNeverFiresForZeroOrNegativeCounts() {
    assertFalse(shouldPlaySibhaReminder(count = 0, reminderEvery = 10))
    assertFalse(shouldPlaySibhaReminder(count = -1, reminderEvery = 10))
  }
}
