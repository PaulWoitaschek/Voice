package de.ph1b.audiobook.persistence.internals

import android.app.backup.BackupAgentHelper
import android.app.backup.SharedPreferencesBackupHelper
import android.os.Build
import android.preference.PreferenceManager

class CustomBackupAgent : BackupAgentHelper() {

  override fun onCreate() {
    val defaultSharedPreferencesName: String =
      if (Build.VERSION.SDK_INT >= 24) PreferenceManager.getDefaultSharedPreferencesName(this)
      else "${packageName}_preferences"
    addHelper("BACKUP_KEY", SharedPreferencesBackupHelper(this, defaultSharedPreferencesName))
  }
}
