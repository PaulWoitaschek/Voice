package de.ph1b.audiobook.uitools

import android.os.Build
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import de.ph1b.audiobook.R

enum class NightMode(@StringRes val nameId: Int, val nightMode: Int) {
  DayNight(
      R.string.pref_theme_daynight,
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
      } else {
        AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
      }
  ),
  Day(R.string.pref_theme_day, AppCompatDelegate.MODE_NIGHT_NO),
  Night(R.string.pref_theme_night, AppCompatDelegate.MODE_NIGHT_YES)
}
