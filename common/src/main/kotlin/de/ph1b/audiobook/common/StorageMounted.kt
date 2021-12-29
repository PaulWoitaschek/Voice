package de.ph1b.audiobook.common

import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun storageMounted(): Boolean = withContext(Dispatchers.IO) {
  Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
}
