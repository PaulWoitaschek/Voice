package voice.features.sibha

internal fun shouldPlaySibhaReminder(count: Int, reminderEvery: Int): Boolean {
  return count > 0 && reminderEvery > 0 && count % reminderEvery == 0
}
